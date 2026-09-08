package com.cake.struts.content.mesh;

import com.cake.struts.content.geometry.StrutVertex;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public record StrutQuad(StrutVertex[] vertices, TextureAtlasSprite sprite, Direction direction, int tintIndex,
                        boolean shade) {

    public StrutVertex vertex(final int index) {
        return this.vertices[index];
    }
}
