package com.cake.struts.compat.flywheel;

import com.cake.struts.content.block.StrutBlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StrutVisualBridge {

    void register(BlockEntityType<? extends StrutBlockEntity> blockEntityType);

    boolean supportsVisualization(@Nullable LevelAccessor level);

    void queueUpdate(@NotNull BlockEntity blockEntity);
}
