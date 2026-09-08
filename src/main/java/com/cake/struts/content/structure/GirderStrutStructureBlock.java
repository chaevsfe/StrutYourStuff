package com.cake.struts.content.structure;

import com.cake.struts.content.block.StrutBlock;
import com.cake.struts.content.block.StrutBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

/**
 * Structure block for the in-between segments of the girder strut.
 * This block is invisible but acts as a collision box.
 * The collision box has to be generated see {@link GirderStrutStructureShapes} for more details.
 * Whether this block exists or not is determined by the {@link GirderStrutStructureShapes} as it tracks all girder struts
 * (and this block may represent multiple girder struts).
 * Replaceable, so the player can build right through, but breaking this should break the connections within this block.
 */
public class GirderStrutStructureBlock extends Block implements GirderStrutShapedBlock, SimpleWaterloggedBlock {

    public GirderStrutStructureBlock(final Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public @NotNull VoxelShape getShape(final @NotNull BlockState state, final @NotNull BlockGetter level,
                                        final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @NotNull VoxelShape getInteractionShape(final @NotNull BlockState state, final @NotNull BlockGetter level, final @NotNull BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected void spawnDestroyParticles(final @NotNull Level level, final @NotNull Player player, final @NotNull BlockPos pos, final @NotNull BlockState state) {
        //Find the first block state of anchors targeting this block (otherwise do nothing)
        for (final ConnectionKey key : Set.copyOf(GirderStrutStructureShapes.getConnectionsAt(level, pos))) {
            final BlockState anchorState = level.getBlockState(key.a());
            if (anchorState.getBlock() instanceof StrutBlock) {
                level.levelEvent(player, 2001, pos, getId(anchorState));
            }
            return;
        }
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(final @NotNull BlockState state, final @NotNull BlockGetter level,
                                                 final @NotNull BlockPos pos, final @NotNull CollisionContext context) {
        return this.getStrutShape(level, pos);
    }

    @Override
    protected @NotNull BlockState updateShape(final BlockState state, final @NotNull LevelReader level, final @NotNull ScheduledTickAccess tickAccess,
                                              final @NotNull BlockPos pos, final @NotNull Direction direction, final @NotNull BlockPos neighborPos,
                                              final @NotNull BlockState neighborState, final @NotNull RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public @NotNull FluidState getFluidState(final BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(final @NotNull BlockPlaceContext context) {
        final FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    protected void affectNeighborsAfterRemoval(final @NotNull BlockState state, final @NotNull ServerLevel level,
                                               final @NotNull BlockPos pos, final boolean movedByPiston) {
        if (movedByPiston || !level.getBlockState(pos).isAir()) {
            return;
        }
        for (final ConnectionKey key : Set.copyOf(GirderStrutStructureShapes.getConnectionsAt(level, pos))) {
            GirderStrutStructureShapes.unregisterConnection(level, key.a(), key.b());

            if (level.getBlockEntity(key.a()) instanceof final StrutBlockEntity strutA) {
                strutA.removeConnection(key.b(), false);
                if (strutA.connectionCount() == 0) {
                    level.destroyBlock(key.a(), true);
                }
            }
            if (level.getBlockEntity(key.b()) instanceof final StrutBlockEntity strutB) {
                strutB.removeConnection(key.a(), false);
                if (strutB.connectionCount() == 0) {
                    level.destroyBlock(key.b(), true);
                }
            }
        }
    }

    @Override
    public @NotNull RenderShape getRenderShape(final @NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public boolean canBeReplaced(final @NotNull BlockState state, final @NotNull BlockPlaceContext context) {
        return true;
    }

    @Override
    public float getShadeBrightness(final @NotNull BlockState state, final @NotNull BlockGetter level,
                                    final @NotNull BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(final @NotNull BlockState state) {
        return true;
    }
}
