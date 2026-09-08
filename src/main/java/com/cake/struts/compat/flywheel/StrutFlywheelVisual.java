package com.cake.struts.compat.flywheel;

import com.cake.struts.content.StrutModelBuilder;
import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import com.zurrtum.create.client.flywheel.api.instance.Instance;
import com.zurrtum.create.client.flywheel.api.model.Model;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.instance.InstanceTypes;
import com.zurrtum.create.client.flywheel.lib.instance.TransformedInstance;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import com.cake.struts.content.mesh.StrutQuad;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class StrutFlywheelVisual extends AbstractBlockEntityVisual<StrutBlockEntity> {

    private @Nullable TransformedInstance instance;
    private @Nullable List<StrutQuad> cachedQuads;
    private @Nullable Model cachedModel;
    private int cachedConnectionHash = Integer.MIN_VALUE;

    public StrutFlywheelVisual(final @NotNull VisualizationContext ctx,
                               final @NotNull StrutBlockEntity blockEntity,
                               final float partialTick) {
        super(ctx, blockEntity, partialTick);
        this.refreshModel();
    }

    @Override
    public void update(final float partialTick) {
        this.refreshModel();
    }

    @Override
    public void updateLight(final float partialTick) {
        this.cachedModel = null;
        this.refreshModel();
    }

    @Override
    public void collectCrumblingInstances(final Consumer<@Nullable Instance> consumer) {
        consumer.accept(this.instance);
    }

    @Override
    protected void _delete() {
        this.clearInstance();
    }

    private void refreshModel() {
        if (!(this.blockState.getBlock() instanceof final StrutBlock strutBlock)) {
            this.clearInstance();
            return;
        }

        final int connectionHash = this.blockEntity.getConnectionHash();
        if (connectionHash == this.cachedConnectionHash && this.cachedQuads != null && this.cachedModel != null && this.instance != null) {
            return;
        }

        if (this.cachedQuads == null || connectionHash != this.cachedConnectionHash) {
            this.cachedQuads = this.resolveQuads(strutBlock);
            this.cachedConnectionHash = connectionHash;
            this.cachedModel = null;
        }

        if (this.cachedQuads.isEmpty()) {
            this.clearInstance();
            return;
        }

        if (this.cachedModel == null) {
            final boolean constantAmbientLight = this.level != null && this.level.dimensionType().hasCeiling();
            this.cachedModel = FlywheelMeshBuilder.buildLitModel(this.cachedQuads, this.blockEntity.createLighter(),
                    constantAmbientLight, strutBlock.getModelType().getRenderLayer());
        }

        if (this.instance != null) {
            this.instance.delete();
        }

        this.instance = this.instancerProvider().instancer(
                InstanceTypes.TRANSFORMED,
                this.cachedModel
        ).createInstance();

        this.instance.setIdentityTransform().translate(this.getVisualPosition());
        this.instance.light(0);
        this.instance.handle().setChanged();
    }

    private @NotNull List<StrutQuad> resolveQuads(final @NotNull StrutBlock strutBlock) {
        final List<StrutQuad> quadCache = this.blockEntity.connectionQuadCache;
        if (quadCache != null) {
            return quadCache;
        }
        return StrutModelBuilder.buildConnectionQuads(
                this.level, this.pos, this.blockState,
                this.blockEntity, strutBlock.getModelType()
        );
    }

    private void clearInstance() {
        if (this.instance != null) {
            this.instance.delete();
            this.instance = null;
        }
        this.cachedModel = null;
    }
}
