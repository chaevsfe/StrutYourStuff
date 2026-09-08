package com.cake.struts.compat.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SableCompanion {

    public static final SableCompanion INSTANCE = new SableCompanion();

    private SableCompanion() {
    }

    public @Nullable ClientSubLevelAccess getContainingClient(final @Nullable BlockPos pos) {
        return null;
    }

    public double distanceSquaredWithSubLevels(final Level level, final Vec3 a, final Vec3 b) {
        return a.distanceToSqr(b);
    }
}
