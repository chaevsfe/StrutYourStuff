package com.cake.struts.content;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public record StrutModelType(Identifier segmentModelLocation, Identifier capTexture, int shapeSizeXPixels,
                             int shapeSizeYPixels,
                             int voxelShapeResolutionPixels,
                             StrutRenderLayer renderLayer) {

    public static final int DEFAULT_VOXEL_SHAPE_RESOLUTION = 4;

    public StrutModelType {
        if (voxelShapeResolutionPixels < 1) {
            voxelShapeResolutionPixels = 1;
        }
    }

    public StrutModelType(final Identifier segmentModelLocation, final Identifier capTexture, final int shapeSizeXPixels, final int shapeSizeYPixels) {
        this(segmentModelLocation, capTexture, shapeSizeXPixels, shapeSizeYPixels, DEFAULT_VOXEL_SHAPE_RESOLUTION, StrutRenderLayer.SOLID);
    }

    public StrutModelType(final Identifier segmentModelLocation, final Identifier capTexture) {
        this(segmentModelLocation, capTexture, 8, 12, DEFAULT_VOXEL_SHAPE_RESOLUTION, StrutRenderLayer.SOLID);
    }

    public StrutModelType(final Identifier segmentModelLocation, final Identifier capTexture,
                          final StrutRenderLayer renderLayer) {
        this(segmentModelLocation, capTexture, 8, 12, DEFAULT_VOXEL_SHAPE_RESOLUTION, renderLayer);
    }

    @Environment(EnvType.CLIENT)
    public ChunkSectionLayer getRenderLayer() {
        return switch (this.renderLayer) {
            case SOLID -> ChunkSectionLayer.SOLID;
            case CUTOUT -> ChunkSectionLayer.CUTOUT;
            case TRANSLUCENT -> ChunkSectionLayer.TRANSLUCENT;
        };
    }

    @Environment(EnvType.CLIENT)
    public RenderType getRenderType() {
        return switch (this.renderLayer) {
            case SOLID -> RenderTypes.solidMovingBlock();
            case CUTOUT -> RenderTypes.cutoutMovingBlock();
            case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
        };
    }
}
