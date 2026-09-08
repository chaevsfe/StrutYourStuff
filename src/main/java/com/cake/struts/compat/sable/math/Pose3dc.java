package com.cake.struts.compat.sable.math;

import net.minecraft.world.phys.Vec3;

public interface Pose3dc {

    Vec3 transformPosition(Vec3 pos);

    Vec3 transformPositionInverse(Vec3 pos);

    Vec3 transformNormal(Vec3 normal);
}
