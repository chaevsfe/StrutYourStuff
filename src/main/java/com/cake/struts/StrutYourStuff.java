package com.cake.struts;

import com.cake.struts.content.structure.GirderStrutStructureShapes;
import com.cake.struts.internal.util.ChunkedMap;
import com.cake.struts.internal.util.LevelSafeStorage;
import com.cake.struts.network.StrutPackets;
import com.cake.struts.registry.StrutBlocks;
import com.cake.struts.registry.StrutDataComponents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StrutYourStuff {

    public static final String MOD_ID = "struts";

    public static final Logger LOGGER = LoggerFactory.getLogger("Strut Your Stuff");

    private StrutYourStuff() {
    }

    public static void registerBlocksEarly() {
        StrutBlocks.register();
    }

    public static void init() {
        StrutDataComponents.register();
        StrutPackets.register();
        registerServerEvents();
    }

    private static void registerServerEvents() {
        ServerLevelEvents.UNLOAD.register((server, level) -> {
            LevelSafeStorage.clearAll(level);
            ChunkedMap.unbindAll(level);
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> ChunkedMap.evictChunk(level, chunk.getPos()));
        ServerTickEvents.END_LEVEL_TICK.register(GirderStrutStructureShapes::flushQueuedRestores);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level.isClientSide()) {
                return true;
            }
            queueStructureRestore(level, pos, state);
            return true;
        });
    }

    private static void queueStructureRestore(final net.minecraft.world.level.Level level,
                                              final net.minecraft.core.BlockPos pos,
                                              final BlockState brokenState) {
        if (brokenState.getBlock() instanceof com.cake.struts.content.block.StrutBlock
                || brokenState.is(StrutBlocks.GIRDER_STRUT_STRUCTURE)) {
            return;
        }
        if (GirderStrutStructureShapes.hasPositionData(level, pos)) {
            GirderStrutStructureShapes.queueRestoreFromBreak(level, pos);
        }
    }
}
