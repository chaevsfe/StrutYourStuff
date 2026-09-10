package com.cake.struts.content;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.connection.GirderConnectionNode;
import com.cake.struts.content.structure.ConnectionKey;
import com.cake.struts.content.structure.GirderStrutStructureShapes;
import com.cake.struts.registry.StrutItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class StrutBreakerHelper {
    private static final double MAX_BREAK_DISTANCE_SQ = 64 * 64;

    public static void breakStrut(final @NotNull Player player, final @NotNull ConnectionKey target) {
        if (!(player.level() instanceof final ServerLevel level)) return;
        if (player.isSpectator() || !player.mayBuild()) return;

        final BlockPos a = target.a();
        final BlockPos b = target.b();
        if (a.equals(b)) return;
        if (!level.isLoaded(a) || !level.isLoaded(b)) return;
        if (!(level.getBlockEntity(a) instanceof final StrutBlockEntity strutA)) return;
        if (!(level.getBlockEntity(b) instanceof final StrutBlockEntity strutB)) return;
        if (!isConnectedTo(strutA, a, b) || !isConnectedTo(strutB, b, a)) return;

        final Vec3 centerA = Vec3.atCenterOf(a);
        final Vec3 centerB = Vec3.atCenterOf(b);
        if (distanceToSegmentSq(player.getEyePosition(), centerA, centerB) > MAX_BREAK_DISTANCE_SQ) return;
        if (!level.mayInteract(player, a) || !level.mayInteract(player, b)) return;

        final Set<BlockPos> anchorsToRemove = new LinkedHashSet<>();
        if (strutA.connectionCount() <= 1) {
            anchorsToRemove.add(a);
        }
        if (strutB.connectionCount() <= 1) {
            anchorsToRemove.add(b);
        }
        final Map<BlockPos, ItemStack> pendingDrops = player.hasInfiniteMaterials()
                ? Map.of()
                : collectAnchorDrops(level, anchorsToRemove);

        removeConnection(level, target);

        final BlockPos dropPos = BlockPos.containing(centerA.lerp(centerB, 0.5));
        final boolean toInventory = isHoldingWrench(player);
        for (final BlockPos anchorPos : anchorsToRemove) {
            if (!ensureAnchorRemoved(level, anchorPos)) continue;
            final ItemStack stack = pendingDrops.get(anchorPos);
            if (stack == null || stack.isEmpty()) continue;
            if (toInventory && player.addItem(stack)) continue;
            Block.popResource(level, dropPos, stack);
        }
    }

    private static boolean isConnectedTo(final @NotNull StrutBlockEntity strut,
                                         final @NotNull BlockPos origin,
                                         final @NotNull BlockPos other) {
        for (final GirderConnectionNode node : strut.getConnectionsCopy()) {
            if (node.absoluteFrom(origin).equals(other)) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToSegmentSq(final @NotNull Vec3 point,
                                              final @NotNull Vec3 from,
                                              final @NotNull Vec3 to) {
        final Vec3 segment = to.subtract(from);
        final double lengthSq = segment.lengthSqr();
        if (lengthSq < 1.0E-6) {
            return point.distanceToSqr(from);
        }
        final double t = Math.clamp(point.subtract(from).dot(segment) / lengthSq, 0.0, 1.0);
        return point.distanceToSqr(from.add(segment.scale(t)));
    }

    private static boolean isHoldingWrench(final @NotNull Player player) {
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }

    private static boolean isWrench(final @NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(StrutItemTags.WRENCHES);
    }

    private static @NotNull Map<BlockPos, ItemStack> collectAnchorDrops(final @NotNull ServerLevel level,
                                                                        final @NotNull Set<BlockPos> anchorsToRemove) {
        final Map<BlockPos, ItemStack> drops = new LinkedHashMap<>();
        for (final BlockPos anchorPos : anchorsToRemove) {
            final BlockState anchorState = level.getBlockState(anchorPos);
            if (!(anchorState.getBlock() instanceof StrutBlock)) {
                continue;
            }
            final Item item = anchorState.getBlock().asItem();
            final ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty()) {
                drops.put(anchorPos, stack);
            }
        }
        return drops;
    }

    private static void removeConnection(final @NotNull ServerLevel level, final @NotNull ConnectionKey key) {
        GirderStrutStructureShapes.unregisterConnection(level, key.a(), key.b());
        if (level.getBlockEntity(key.a()) instanceof final StrutBlockEntity strutA) {
            strutA.removeConnection(key.b(), false);
        }
        if (level.getBlockEntity(key.b()) instanceof final StrutBlockEntity strutB) {
            strutB.removeConnection(key.a(), false);
        }
    }

    private static boolean ensureAnchorRemoved(final @NotNull ServerLevel level, final @NotNull BlockPos anchorPos) {
        if (!(level.getBlockState(anchorPos).getBlock() instanceof StrutBlock)) {
            return true;
        }
        if (level.getBlockEntity(anchorPos) instanceof final StrutBlockEntity strut && strut.connectionCount() > 0) {
            return false;
        }
        level.destroyBlock(anchorPos, false);
        return !(level.getBlockState(anchorPos).getBlock() instanceof StrutBlock);
    }
}
