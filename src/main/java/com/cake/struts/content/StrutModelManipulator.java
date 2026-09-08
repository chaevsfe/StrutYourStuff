package com.cake.struts.content;

import com.cake.struts.content.cap.CapAccumulator;
import com.cake.struts.content.geometry.StrutGeometry;
import com.cake.struts.content.mesh.StrutMeshQuad;
import com.cake.struts.content.mesh.StrutSegmentMesh;
import com.mojang.blaze3d.vertex.PoseStack;
import com.cake.struts.content.geometry.StrutVertex;
import com.cake.struts.content.mesh.StrutQuad;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class StrutModelManipulator {

    private static final Map<StrutModelType, StrutSegmentMesh> segmentMeshes = new HashMap<>();

    static List<StrutQuad> bakeConnection(final StrutModelBuilder.GirderConnection connection, final StrutModelType modelType) {
        if (connection.cableRenderInfo() != null) {
            return CableStrutModelManipulator.bake(connection, getSegmentMesh(modelType), modelType);
        }

        if (connection.renderLength() <= StrutGeometry.EPSILON) {
            return List.of();
        }

        final Vec3 span = connection.end().subtract(connection.start());
        final double spanLength = span.length();
        if (spanLength <= StrutGeometry.EPSILON) {
            return List.of();
        }
        final double renderLength = Math.min(connection.renderLength(), spanLength);
        if (renderLength <= StrutGeometry.EPSILON) {
            return List.of();
        }

        final StrutSegmentMesh mesh = getSegmentMesh(modelType);
        final List<StrutMeshQuad> quads = mesh.forLength((float) renderLength);

        final PoseStack poseStack = StrutGeometry.poseAlong(connection.start(), span.normalize());
        final PoseStack.Pose last = poseStack.last();
        final Matrix4f pose = new Matrix4f(last.pose());
        final Matrix3f normalMatrix = new Matrix3f(last.normal());

        final Vector3f planePoint = StrutGeometry.toVector3f(connection.surfacePlanePoint());
        final Vector3f planeNormal = StrutGeometry.toVector3f(connection.surfaceNormal());
        if (planeNormal.lengthSquared() > StrutGeometry.EPSILON) {
            planeNormal.normalize();
        }

        final List<StrutQuad> bakedQuads = new ArrayList<>();
        final CapAccumulator capAccumulator = new CapAccumulator(modelType.capTexture());
        for (final StrutMeshQuad quad : quads) {
            quad.transformAndEmit(pose, normalMatrix, planePoint, planeNormal, capAccumulator, bakedQuads);
        }
        capAccumulator.emitCaps(planePoint, planeNormal, bakedQuads);
        return bakedQuads;
    }

    static @NotNull StrutSegmentMesh getSegmentMesh(final StrutModelType modelType) {
        StrutSegmentMesh strutSegmentMesh = segmentMeshes.get(modelType);
        if (strutSegmentMesh == null) {
            segmentMeshes.put(modelType, strutSegmentMesh = new StrutSegmentMesh(loadSegmentQuads(modelType)));
        }
        return strutSegmentMesh;
    }

    private static List<StrutQuad> loadSegmentQuads(final StrutModelType modelType) {
        final BlockStateModel model = PartialModel.of(modelType.segmentModelLocation()).get();
        final RandomSource random = RandomSource.create();
        final List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts);

        final List<StrutQuad> quads = new ArrayList<>();
        for (final BlockStateModelPart part : parts) {
            collect(part.getQuads(null), quads);
            for (final Direction direction : Direction.values()) {
                collect(part.getQuads(direction), quads);
            }
        }
        return quads;
    }

    private static void collect(final List<BakedQuad> source, final List<StrutQuad> target) {
        for (final BakedQuad quad : source) {
            target.add(convert(quad));
        }
    }

    private static StrutQuad convert(final BakedQuad quad) {
        final Vector3f faceNormal = new Vector3f(
                quad.direction().getStepX(),
                quad.direction().getStepY(),
                quad.direction().getStepZ()
        );
        final StrutVertex[] vertices = new StrutVertex[4];
        for (int i = 0; i < 4; i++) {
            final org.joml.Vector3fc position = quad.position(i);
            final long packedUv = quad.packedUV(i);
            vertices[i] = new StrutVertex(
                    new Vector3f(position.x(), position.y(), position.z()),
                    new Vector3f(faceNormal),
                    UVPair.unpackU(packedUv),
                    UVPair.unpackV(packedUv),
                    StrutGeometry.DEFAULT_COLOR,
                    0
            );
        }
        return new StrutQuad(vertices, quad.materialInfo().sprite(), quad.direction(),
                quad.materialInfo().tintIndex(), quad.materialInfo().shade());
    }

    public static void invalidateMeshes() {
        segmentMeshes.clear();
    }

}

