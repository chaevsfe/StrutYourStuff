package com.cake.struts.content.block;

import com.cake.struts.compat.flywheel.StrutsFlywheelCompatLoader;
import com.cake.struts.content.IAntiClippedShadowLighter;
import com.cake.struts.content.StrutModelType;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.GirderStrutStructureShapes;
import com.zurrtum.create.api.contraption.transformable.TransformableBlockEntity;
import com.zurrtum.create.content.contraptions.StructureTransform;
import com.cake.struts.content.mesh.StrutQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.cake.struts.content.block.StrutBlock.MAX_SPAN;

public class StrutBlockEntity extends BlockEntity implements IAntiClippedShadowLighter, TransformableBlockEntity {

    private static final StrutModelType DEFAULT_MODEL_TYPE = new StrutModelType(
            Identifier.fromNamespaceAndPath("struts", "block/girder_strut/girder_strut_segment"),
            Identifier.fromNamespaceAndPath("struts", "block/industrial_iron_block")
    );

    /**
     * Set on the client during startup to receive connection-change notifications.
     * Never referenced from dedicated-server code.
     */
    public static @Nullable BiConsumer<Level, StrutBlockEntity> CLIENT_UPDATE_LISTENER;
    public static @Nullable BiConsumer<Level, BlockPos> CLIENT_REMOVE_LISTENER;

    private final Set<GirderConnectionNode> connections = new HashSet<>();
    private final Set<GirderConnectionNode> registeredConnections = new HashSet<>();
    private final Set<BlockPos> unresolvedLegacyConnections = new HashSet<>();
    private final Set<BlockPos> tensionedOffsets = new HashSet<>();
    public List<StrutQuad> connectionQuadCache;

    protected boolean checkNextTick = true;
    private boolean loaded;

    public StrutBlockEntity(final BlockEntityType<? extends StrutBlockEntity> type,
                            final BlockPos pos,
                            final BlockState state) {
        super(type, pos, state);
    }

    public void tick() {
        if (this.level == null) return;
        if (!this.loaded) {
            this.loaded = true;
            this.onLoaded();
        }
        if (this.checkNextTick && !this.level.isClientSide()) {
            this.checkNextTick = false;
            for (final GirderConnectionNode conn : this.connections) {
                final BlockPos other = conn.absoluteFrom(this.getBlockPos());
                if (!this.level.isLoaded(other)) continue;
                if ((this.level.getBlockEntity(other) instanceof final StrutBlockEntity otherBE)) {
                    if (otherBE.connections.stream().anyMatch(c -> c.absoluteFrom(other).equals(this.getBlockPos()))) {
                        return;
                    }
                }
                this.removeConnection(other);
            }
            this.removeIfEmpty();
        }
    }

    private void onLoaded() {
        if (this.level == null) {
            return;
        }
        if (!this.level.isClientSide()) {
            final StrutModelType modelType = this.getBlockState().getBlock() instanceof final StrutBlock block ? block.getModelType() : DEFAULT_MODEL_TYPE;
            final boolean shouldRegisterShapes = this.shouldRegisterStructureShapes();
            for (final GirderConnectionNode data : this.connections) {
                if (shouldRegisterShapes && this.registeredConnections.add(data)) {
                    GirderStrutStructureShapes.registerConnection(
                            this.level,
                            this.getBlockPos(),
                            this.getAttachmentDirection(),
                            data.absoluteFrom(this.getBlockPos()),
                            data.peerFacing(),
                            modelType
                    );
                }
            }
            this.tryResolveLegacyConnections();
        } else {
            this.syncStructureShapes();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null) {
            if (!this.level.isClientSide()) {
                for (final GirderConnectionNode data : this.registeredConnections) {
                    GirderStrutStructureShapes.unregisterConnection(
                            this.level,
                            this.getBlockPos(),
                            data.absoluteFrom(this.getBlockPos())
                    );
                }
                this.registeredConnections.clear();
                for (final GirderConnectionNode data : this.connections) {
                    final BlockPos other = data.absoluteFrom(this.getBlockPos());
                    if (!this.level.isLoaded(other)) continue;
                    if (this.level.getBlockEntity(other) instanceof final StrutBlockEntity otherBE) {
                        otherBE.recheckNextTick();
                    }
                }

            } else {
                this.unregisterAllStructureShapes();
                if (CLIENT_REMOVE_LISTENER != null) {
                    CLIENT_REMOVE_LISTENER.accept(this.level, this.getBlockPos());
                }
            }
        }
        this.connectionQuadCache = null;
    }

    public void addConnection(final BlockPos other, final Direction otherFacing) {
        final GirderConnectionNode data = GirderConnectionNode.fromAbsolute(this.getBlockPos(), other, otherFacing);
        if (!other.equals(this.getBlockPos()) && this.connections.add(data)) {
            if (this.level != null && !this.level.isClientSide() && this.shouldRegisterStructureShapes() && this.registeredConnections.add(
                    data)) {
                final StrutModelType modelType = this.getBlockState().getBlock() instanceof final StrutBlock block ? block.getModelType() : DEFAULT_MODEL_TYPE;
                GirderStrutStructureShapes.registerConnection(
                        this.level,
                        this.getBlockPos(),
                        this.getAttachmentDirection(),
                        other,
                        otherFacing,
                        modelType
                );
            }
            this.notifyModelChange();
        }
    }

    public void removeConnection(final BlockPos pos) {
        this.removeConnection(pos, true);
    }

    public void removeConnection(final BlockPos pos, final boolean dropIfEmpty) {
        GirderConnectionNode toRemove = null;
        final BlockPos relative = pos.subtract(this.getBlockPos());
        for (final GirderConnectionNode data : this.connections) {
            if (data.relativeOffset().equals(relative)) {
                toRemove = data;
                break;
            }
        }
        if (toRemove != null && this.connections.remove(toRemove)) {
            this.tensionedOffsets.remove(relative);
            if (this.level != null && !this.level.isClientSide() && this.registeredConnections.remove(toRemove)) {
                GirderStrutStructureShapes.unregisterConnection(this.level, this.getBlockPos(), pos);
            }
            this.notifyModelChange();
        }
        this.removeIfEmpty(dropIfEmpty);
    }

    private void removeIfEmpty() {
        this.removeIfEmpty(true);
    }

    private void removeIfEmpty(final boolean drop) {
        if (this.connections.isEmpty() && this.unresolvedLegacyConnections.isEmpty() && !(this.level == null)) {
            this.level.destroyBlock(this.getBlockPos(), drop);
        }
    }

    public int connectionCount() {
        return this.connections.size();
    }

    public Set<GirderConnectionNode> getConnectionsCopy() {
        return Set.copyOf(this.connections);
    }

    public boolean isTensioned(final BlockPos relativeOffset) {
        return this.tensionedOffsets.contains(relativeOffset);
    }

    public boolean toggleAllCableTension() {
        final boolean shouldTension = this.hasAnyUntensionedConnection();

        for (final GirderConnectionNode conn : this.connections) {
            this.setTensioned(conn.relativeOffset(), shouldTension);
            this.syncTensionToPeer(conn, shouldTension);
        }

        this.notifyModelChange();
        return shouldTension;
    }

    private boolean hasAnyUntensionedConnection() {
        for (final GirderConnectionNode conn : this.connections) {
            if (!this.tensionedOffsets.contains(conn.relativeOffset())) {
                return true;
            }
        }
        return false;
    }

    private void setTensioned(final BlockPos relativeOffset, final boolean tensioned) {
        if (tensioned) {
            this.tensionedOffsets.add(relativeOffset);
        } else {
            this.tensionedOffsets.remove(relativeOffset);
        }
    }

    private void syncTensionToPeer(final GirderConnectionNode conn, final boolean shouldTension) {
        if (this.level == null) {
            return;
        }
        final BlockPos peerPos = conn.absoluteFrom(this.worldPosition);
        if (this.level.getBlockEntity(peerPos) instanceof final StrutBlockEntity peer) {
            peer.setTensioned(this.worldPosition.subtract(peerPos), shouldTension);
            peer.notifyModelChange();
        }
    }

    public int getConnectionHash() {
        return 31 * this.connections.hashCode() + this.tensionedOffsets.hashCode();
    }

    private void notifyModelChange() {
        this.connectionQuadCache = null;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(
                    this.worldPosition,
                    this.getBlockState(),
                    this.getBlockState(),
                    Block.UPDATE_ALL
            );
            if (this.level.isClientSide()) {
                StrutsFlywheelCompatLoader.queueUpdate(this);
            }
        }
    }

    @Override
    protected void saveAdditional(final @NotNull ValueOutput output) {
        super.saveAdditional(output);
        final ValueOutput.ValueOutputList list = output.childrenList("Connections");
        for (final GirderConnectionNode p : this.connections) {
            final ValueOutput ct = list.addChild();
            ct.putInt("X", p.relativeOffset().getX());
            ct.putInt("Y", p.relativeOffset().getY());
            ct.putInt("Z", p.relativeOffset().getZ());
            ct.putInt("Facing", p.peerFacing().get3DDataValue());
        }
        for (final BlockPos unresolvedOffset : this.unresolvedLegacyConnections) {
            final ValueOutput ct = list.addChild();
            ct.putInt("X", unresolvedOffset.getX());
            ct.putInt("Y", unresolvedOffset.getY());
            ct.putInt("Z", unresolvedOffset.getZ());
        }
        if (!this.tensionedOffsets.isEmpty()) {
            final ValueOutput.ValueOutputList tensionList = output.childrenList("TensionedConnections");
            for (final BlockPos offset : this.tensionedOffsets) {
                final ValueOutput ct = tensionList.addChild();
                ct.putInt("X", offset.getX());
                ct.putInt("Y", offset.getY());
                ct.putInt("Z", offset.getZ());
            }
        }
    }

    @Override
    protected void loadAdditional(final @NotNull ValueInput input) {
        super.loadAdditional(input);
        this.connections.clear();
        this.unresolvedLegacyConnections.clear();
        for (final ValueInput ct : input.childrenListOrEmpty("Connections")) {
            final BlockPos offset = new BlockPos(ct.getIntOr("X", 0), ct.getIntOr("Y", 0), ct.getIntOr("Z", 0));
            if (offset.distSqr(Vec3i.ZERO) > MAX_SPAN * MAX_SPAN) {
                continue;
            }
            final java.util.Optional<Integer> facing = ct.getInt("Facing");
            if (facing.isPresent()) {
                this.connections.add(new GirderConnectionNode(offset, Direction.from3DDataValue(facing.get())));
            } else {
                this.unresolvedLegacyConnections.add(offset);
            }
        }
        this.tensionedOffsets.clear();
        for (final ValueInput ct : input.childrenListOrEmpty("TensionedConnections")) {
            this.tensionedOffsets.add(new BlockPos(ct.getIntOr("X", 0), ct.getIntOr("Y", 0), ct.getIntOr("Z", 0)));
        }
        this.connectionQuadCache = null;
        if (this.level != null && this.level.isClientSide()) {
            this.syncStructureShapes();
            StrutsFlywheelCompatLoader.queueUpdate(this);
            if (CLIENT_UPDATE_LISTENER != null) {
                CLIENT_UPDATE_LISTENER.accept(this.level, this);
            }
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(final HolderLookup.@NotNull Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void tryResolveLegacyConnections() {
        if (this.level == null || this.level.isClientSide() || this.unresolvedLegacyConnections.isEmpty()) {
            return;
        }

        final Set<BlockPos> resolvedOffsets = new HashSet<>();
        for (final BlockPos unresolvedOffset : this.unresolvedLegacyConnections) {
            final BlockPos otherPosition = this.getBlockPos().offset(unresolvedOffset);
            Direction otherFacing = null;

            if (this.level.getBlockEntity(otherPosition) instanceof final StrutBlockEntity otherBlockEntity) {
                otherFacing = otherBlockEntity.getAttachmentDirection();
            } else if (this.level.getBlockState(otherPosition).getBlock() instanceof StrutBlock) {
                otherFacing = this.level.getBlockState(otherPosition).getValue(StrutBlock.FACING);
            }

            if (otherFacing != null) {
                this.addConnection(otherPosition, otherFacing);
                resolvedOffsets.add(unresolvedOffset);
            }
        }

        if (!resolvedOffsets.isEmpty()) {
            this.unresolvedLegacyConnections.removeAll(resolvedOffsets);
            this.notifyModelChange();
        }
    }

    public Vec3 getAttachment() {
        return Vec3.atCenterOf(this.getBlockPos()).relative(this.getBlockState().getValue(StrutBlock.FACING), -0.4);
    }

    public Direction getAttachmentDirection() {
        return this.getBlockState().getValue(StrutBlock.FACING);
    }

    public StrutModelType getModelType() {
        return this.getBlockState().getBlock() instanceof final StrutBlock block ? block.getModelType() : DEFAULT_MODEL_TYPE;
    }

    private boolean shouldRegisterStructureShapes() {
        return !(this.getBlockState().getBlock() instanceof final StrutBlock block && block.getCableRenderInfo() != null);
    }

    /**
     * Client-side mirror of the server's shape registration. Keeps the {@link GirderStrutStructureShapes}
     * registry populated on the client so collision hitboxes exist when connected to a dedicated server.
     * Idempotent, safe to call on every update tag.
     */
    private void syncStructureShapes() {
        if (this.level == null || !this.level.isClientSide()) {
            return;
        }
        if (!this.shouldRegisterStructureShapes()) {
            this.unregisterAllStructureShapes();
            return;
        }

        final StrutModelType modelType = this.getModelType();
        final Direction attachmentDirection = this.getAttachmentDirection();
        final BlockPos pos = this.getBlockPos();
        for (final GirderConnectionNode data : this.connections) {
            if (this.registeredConnections.add(data)) {
                GirderStrutStructureShapes.registerConnection(
                        this.level,
                        pos,
                        attachmentDirection,
                        data.absoluteFrom(pos),
                        data.peerFacing(),
                        modelType
                );
            }
        }
        for (final Iterator<GirderConnectionNode> iterator = this.registeredConnections.iterator(); iterator.hasNext(); ) {
            final GirderConnectionNode data = iterator.next();
            if (!this.connections.contains(data)) {
                GirderStrutStructureShapes.unregisterConnection(this.level, pos, data.absoluteFrom(pos));
                iterator.remove();
            }
        }
    }

    private void unregisterAllStructureShapes() {
        if (this.level == null) {
            return;
        }
        for (final GirderConnectionNode data : this.registeredConnections) {
            GirderStrutStructureShapes.unregisterConnection(this.level, this.getBlockPos(), data.absoluteFrom(this.getBlockPos()));
        }
        this.registeredConnections.clear();
    }

    @Override
    public void transform(final BlockEntity blockEntity, final StructureTransform transform) {
        if (blockEntity instanceof final StrutBlockEntity strutBlockEntity) {
            strutBlockEntity.transform(transform);
        }
    }

    public void transform(final StructureTransform transform) {
        final List<GirderConnectionNode> transformedConnections = this.connections.stream()
                .map(conn -> new GirderConnectionNode(
                        transform.applyWithoutOffset(conn.relativeOffset()),
                        transform.rotateFacing(conn.peerFacing())
                ))
                .toList();
        this.connections.clear();
        this.unresolvedLegacyConnections.clear();
        this.connections.addAll(transformedConnections);
    }

    public void recheckNextTick() {
        this.checkNextTick = true;
    }
}

