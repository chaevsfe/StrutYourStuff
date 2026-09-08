package com.cake.struts.content.block;

import com.cake.struts.compat.flywheel.StrutsFlywheelCompatLoader;
import com.cake.struts.content.DiffuseHelper;
import com.cake.struts.content.StrutModelBuilder;
import com.cake.struts.content.geometry.StrutVertex;
import com.cake.struts.content.mesh.StrutQuad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

public class StrutBlockEntityRenderer implements BlockEntityRenderer<StrutBlockEntity, StrutBlockEntityRenderer.StrutRenderState> {

    public StrutBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public @NotNull StrutRenderState createRenderState() {
        return new StrutRenderState();
    }

    @Override
    public void extractRenderState(final StrutBlockEntity blockEntity,
                                   final StrutRenderState state,
                                   final float partialTick,
                                   final @NotNull Vec3 cameraPos,
                                   final ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.quads = null;
        state.renderType = null;
        state.lighter = null;
        state.constantAmbientLight = false;

        final Level level = blockEntity.getLevel();
        if (!(blockEntity.getBlockState().getBlock() instanceof final StrutBlock strutBlock) || level == null) {
            return;
        }
        if (StrutsFlywheelCompatLoader.supportsVisualization(level)) {
            return;
        }
        if (blockEntity.connectionQuadCache == null) {
            blockEntity.connectionQuadCache = StrutModelBuilder.buildConnectionQuads(
                    level,
                    blockEntity.getBlockPos(),
                    blockEntity.getBlockState(),
                    blockEntity,
                    strutBlock.getModelType()
            );
        }
        if (blockEntity.connectionQuadCache.isEmpty()) {
            return;
        }
        state.quads = blockEntity.connectionQuadCache;
        state.renderType = strutBlock.getModelType().getRenderType();
        state.lighter = blockEntity.createLighter();
        state.constantAmbientLight = level.dimensionType().hasCeiling();
    }

    @Override
    public void submit(final StrutRenderState state,
                       final @NotNull PoseStack poseStack,
                       final @NotNull SubmitNodeCollector collector,
                       final @NotNull CameraRenderState cameraRenderState) {
        final List<StrutQuad> quads = state.quads;
        final RenderType renderType = state.renderType;
        final Function<Vector3f, Integer> lighter = state.lighter;
        if (quads == null || quads.isEmpty() || renderType == null || lighter == null) {
            return;
        }
        final boolean constantAmbientLight = state.constantAmbientLight;
        collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) ->
                draw(pose, consumer, quads, lighter, constantAmbientLight));
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(final @NotNull StrutBlockEntity blockEntity, final @NotNull Vec3 cameraPos) {
        return true;
    }

    private static void draw(final PoseStack.Pose pose,
                             final VertexConsumer consumer,
                             final List<StrutQuad> quads,
                             final Function<Vector3f, Integer> lighter,
                             final boolean constantAmbientLight) {
        final Matrix4f matrix = pose.pose();
        for (final StrutQuad quad : quads) {
            final Vector3f faceNormal = pose.transformNormal(
                    quad.direction().getStepX(),
                    quad.direction().getStepY(),
                    quad.direction().getStepZ(),
                    new Vector3f()
            );
            final float diffuse = quad.shade()
                    ? DiffuseHelper.calculateWorldSpaceDiffuse(faceNormal, constantAmbientLight)
                    : 1.0f;
            for (int i = 0; i < 4; i++) {
                final StrutVertex vertex = quad.vertex(i);
                final int color = vertex.color();
                final int alpha = (color >>> 24) & 0xFF;
                final int red = (int) (((color >> 16) & 0xFF) * diffuse);
                final int green = (int) (((color >> 8) & 0xFF) * diffuse);
                final int blue = (int) ((color & 0xFF) * diffuse);

                final Vector3f local = new Vector3f(vertex.position());
                final int light = lighter.apply(new Vector3f(local));
                final Vector3f world = matrix.transformPosition(local, new Vector3f());
                consumer.addVertex(world.x(), world.y(), world.z())
                        .setColor(ARGB.color(alpha, red, green, blue))
                        .setUv(vertex.u(), vertex.v())
                        .setLight(light)
                        .setNormal(faceNormal.x(), faceNormal.y(), faceNormal.z());
            }
        }
    }

    public static class StrutRenderState extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public List<StrutQuad> quads;
        public RenderType renderType;
        public Function<Vector3f, Integer> lighter;
        public boolean constantAmbientLight;
    }
}
