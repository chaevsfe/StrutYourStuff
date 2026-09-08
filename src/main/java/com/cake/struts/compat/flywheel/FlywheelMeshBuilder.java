package com.cake.struts.compat.flywheel;

import com.cake.struts.content.DiffuseHelper;
import com.cake.struts.content.geometry.StrutVertex;
import com.cake.struts.content.mesh.StrutQuad;
import com.zurrtum.create.client.flywheel.api.material.Material;
import com.zurrtum.create.client.flywheel.api.model.Model;
import com.zurrtum.create.client.flywheel.lib.memory.MemoryBlock;
import com.zurrtum.create.client.flywheel.lib.model.ModelUtil;
import com.zurrtum.create.client.flywheel.lib.model.SimpleModel;
import com.zurrtum.create.client.flywheel.lib.model.SimpleQuadMesh;
import com.zurrtum.create.client.flywheel.lib.vertex.NoOverlayVertexView;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

public class FlywheelMeshBuilder {

    public static Model buildLitModel(final List<StrutQuad> quads,
                                      final Function<Vector3f, Integer> lighter,
                                      final boolean constantAmbientLight,
                                      final ChunkSectionLayer layer) {
        final int vertexCount = quads.size() * 4;
        final MemoryBlock memory = MemoryBlock.mallocTracked((long) vertexCount * NoOverlayVertexView.STRIDE);
        final NoOverlayVertexView view = new NoOverlayVertexView();
        view.ptr(memory.ptr());
        view.vertexCount(vertexCount);
        view.nativeMemoryOwner(memory);

        int vertexIndex = 0;
        for (final StrutQuad quad : quads) {
            final Vector3f faceNormal = new Vector3f(
                    quad.direction().getStepX(),
                    quad.direction().getStepY(),
                    quad.direction().getStepZ()
            );
            final float diffuse = quad.shade()
                    ? DiffuseHelper.calculateWorldSpaceDiffuse(faceNormal, constantAmbientLight)
                    : 1.0f;

            for (int v = 0; v < 4; v++) {
                final StrutVertex vertex = quad.vertex(v);
                final float x = vertex.position().x;
                final float y = vertex.position().y;
                final float z = vertex.position().z;

                final int color = vertex.color();
                final float r = ((color >> 16) & 0xFF) / 255.0f * diffuse;
                final float g = ((color >> 8) & 0xFF) / 255.0f * diffuse;
                final float b = (color & 0xFF) / 255.0f * diffuse;
                final float a = ((color >>> 24) & 0xFF) / 255.0f;

                final int light = lighter.apply(new Vector3f(x, y, z));

                view.x(vertexIndex, x);
                view.y(vertexIndex, y);
                view.z(vertexIndex, z);
                view.r(vertexIndex, r);
                view.g(vertexIndex, g);
                view.b(vertexIndex, b);
                view.a(vertexIndex, a);
                view.u(vertexIndex, vertex.u());
                view.v(vertexIndex, vertex.v());
                view.light(vertexIndex, light);
                view.normalX(vertexIndex, vertex.normal().x);
                view.normalY(vertexIndex, vertex.normal().y);
                view.normalZ(vertexIndex, vertex.normal().z);

                vertexIndex++;
            }
        }

        final Material material = ModelUtil.getMaterial(layer, false, false);
        final SimpleQuadMesh mesh = new SimpleQuadMesh(view, "strut_lit");
        return new SimpleModel(List.of(new Model.ConfiguredMesh(material, mesh)));
    }
}
