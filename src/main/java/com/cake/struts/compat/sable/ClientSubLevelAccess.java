package com.cake.struts.compat.sable;

import com.cake.struts.compat.sable.math.Pose3dc;

public interface ClientSubLevelAccess extends SubLevelAccess {

    Pose3dc renderPose();

    Pose3dc lastPose();
}
