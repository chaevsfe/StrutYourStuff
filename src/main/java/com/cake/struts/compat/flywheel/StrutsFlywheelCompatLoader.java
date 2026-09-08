package com.cake.struts.compat.flywheel;

import com.cake.struts.content.block.StrutBlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StrutsFlywheelCompatLoader {

    private static @Nullable StrutVisualBridge bridge;
    private static final List<BlockEntityType<? extends StrutBlockEntity>> PENDING = new ArrayList<>();

    public static void bind(final StrutVisualBridge visualBridge) {
        bridge = visualBridge;
        for (final BlockEntityType<? extends StrutBlockEntity> type : PENDING) {
            visualBridge.register(type);
        }
        PENDING.clear();
    }

    public static void registerStrutVisual(final BlockEntityType<? extends StrutBlockEntity> blockEntityType) {
        final StrutVisualBridge current = bridge;
        if (current == null) {
            PENDING.add(blockEntityType);
            return;
        }
        current.register(blockEntityType);
    }

    public static boolean supportsVisualization(final @Nullable LevelAccessor level) {
        final StrutVisualBridge current = bridge;
        return current != null && current.supportsVisualization(level);
    }

    public static void queueUpdate(final @NotNull BlockEntity blockEntity) {
        final StrutVisualBridge current = bridge;
        if (current != null) {
            current.queueUpdate(blockEntity);
        }
    }
}
