package com.cake.struts.content;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.mesh.StrutQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StrutModelBuilder {

    private static final double SURFACE_OFFSET = (6 / 16f) + 1e-3;
    public static final double SURFACE_CLIPPING_OFFSET = 10 / 16f + 1e-3;

    private StrutModelBuilder() {
    }

    public static @NotNull List<StrutQuad> buildConnectionQuads(final @NotNull BlockAndLightGetter level,
                                                                final @NotNull BlockPos pos,
                                                                final @NotNull BlockState state,
                                                                final @NotNull StrutBlockEntity blockEntity,
                                                                final @NotNull StrutModelType modelType) {
        final GirderStrutModelData connectionData = GirderStrutModelData.collect(level, pos, state, blockEntity);
        return connectionData.connections()
                .stream()
                .flatMap(connection -> StrutModelManipulator.bakeConnection(connection, modelType).stream())
                .toList();
    }

    static class GirderStrutModelData {
        private final List<GirderConnection> connections;
        private final BlockPos pos;

        private GirderStrutModelData(final List<GirderConnection> connections, final BlockPos pos) {
            this.connections = connections;
            this.pos = pos;
        }

        static GirderStrutModelData collect(final BlockAndLightGetter level, final BlockPos pos, final BlockState state, final StrutBlockEntity blockEntity) {
            if (!(state.getBlock() instanceof final StrutBlock block)) {
                return new GirderStrutModelData(List.of(), pos);
            }
            final Direction facing = state.getValue(StrutBlock.FACING);
            final CableStrutInfo cableRenderInfo = block.getCableRenderInfo();
            final Vec3 blockOrigin = Vec3.atLowerCornerOf(pos);
            final Vec3 facePoint = Vec3.atCenterOf(pos).relative(facing, -SURFACE_CLIPPING_OFFSET);
            final Vec3 thisSurface = Vec3.atCenterOf(pos).relative(facing, -SURFACE_OFFSET);

            final List<GirderConnection> connections = new ArrayList<>();

            for (final GirderConnectionNode data : blockEntity.getConnectionsCopy()) {
                final BlockPos otherPos = data.absoluteFrom(pos);
                final Direction otherFacing = data.peerFacing();
                final Vec3 otherSurface = Vec3.atCenterOf(otherPos).relative(otherFacing, -SURFACE_OFFSET);
                final Vec3 span = otherSurface.subtract(thisSurface);
                if (span.lengthSqr() < 1.0e-4) {
                    continue;
                }
                final Vec3 halfVector = span.scale(0.5);
                final double renderLength = halfVector.length() + 0.5f;
                if (renderLength <= 1.0e-4) {
                    continue;
                }

                final Vec3 startLocal = thisSurface.subtract(blockOrigin);
                final Vec3 endLocal = otherSurface.subtract(blockOrigin);
                final Vec3 planePointLocal = facePoint.subtract(blockOrigin);

                final CableStrutInfo effectiveRenderInfo = cableRenderInfo != null && blockEntity.isTensioned(data.relativeOffset())
                        ? cableRenderInfo.withZeroSag()
                        : cableRenderInfo;
                connections.add(new GirderConnection(
                        startLocal,
                        endLocal,
                        renderLength,
                        planePointLocal,
                        Vec3.atLowerCornerOf(facing.getUnitVec3i()),
                        effectiveRenderInfo
                ));
            }

            return new GirderStrutModelData(Collections.unmodifiableList(connections), pos);
        }

        public BlockPos getPos() {
            return pos;
        }

        List<GirderConnection> connections() {
            return connections;
        }
    }

    public record GirderConnection(Vec3 start, Vec3 end, double renderLength, Vec3 surfacePlanePoint,
                                   Vec3 surfaceNormal,
                                   CableStrutInfo cableRenderInfo) {
    }
}
