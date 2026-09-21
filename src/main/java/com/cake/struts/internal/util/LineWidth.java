package com.cake.struts.internal.util;

import net.minecraft.client.Minecraft;

public final class LineWidth {

    private LineWidth() {
    }

    public static float appropriate() {
        return Minecraft.getInstance().getWindow().getAppropriateLineWidth();
    }
}
