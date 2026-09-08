package com.cake.struts.internal.microliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.cake.struts.compat.sable.ClientSubLevelAccess;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bastardized code under MIT liscence, full credit goes to the Create team
 */
public class Microliner {

    private static final Microliner INSTANCE = new Microliner();
    private final Map<String, MicrolinerEntry> entries = new ConcurrentHashMap<>();

    public static Microliner get() {
        return INSTANCE;
    }


    public void showAABB(final String id, final AABB box, final MicrolinerParams params) {
        this.entries.put(id, new MicrolinerEntry(new MicrolinerAABBOutline(box), params));
    }

    public void showOutline(final String id,
                            final MicrolinerOutline outline,
                            final MicrolinerParams params) {
        this.entries.put(id, new MicrolinerEntry(outline, params));
    }

    public void tick() {
        this.entries.entrySet().removeIf(entry -> !entry.getValue().tick());
    }

    public void render(final LevelRenderContext context) {
        if (this.entries.isEmpty()) {
            return;
        }
        final Vec3 camera = context.levelState().cameraRenderState.pos;
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RenderTypes.lines(), (pose, consumer) -> {
            final PoseStack poseStack = new PoseStack();
            poseStack.last().pose().set(pose.pose());
            poseStack.last().normal().set(pose.normal());
            for (final MicrolinerEntry entry : this.entries.values()) {
                final ClientSubLevelAccess subLevelAccess = entry.params().containingSubLevel();
                entry.outline().render(
                        poseStack,
                        subLevelAccess != null ? subLevelAccess.renderPose() : null,
                        consumer,
                        camera,
                        entry.params()
                );
            }
        });
    }
}
