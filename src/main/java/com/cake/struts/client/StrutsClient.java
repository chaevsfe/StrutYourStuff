package com.cake.struts.client;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.compat.flywheel.FlywheelCompat;
import com.cake.struts.compat.flywheel.StrutsFlywheelCompatLoader;
import com.cake.struts.content.StrutModelManipulator;
import com.cake.struts.content.StrutPlacementEffects;
import com.cake.struts.content.block.StrutBlockEntity;
import com.cake.struts.content.shape.StrutInteractionHandler;
import com.cake.struts.internal.microliner.Microliner;
import com.cake.struts.internal.util.ChunkedMap;
import com.cake.struts.internal.util.LevelSafeStorage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.InteractionResult;

import java.util.Collection;
import java.util.Set;

public final class StrutsClient {

    private StrutsClient() {
    }

    public static void init() {
        StrutsFlywheelCompatLoader.bind(new FlywheelCompat());
        StrutBlockEntity.CLIENT_UPDATE_LISTENER = StrutInteractionHandler::updateOutlineShapes;
        StrutBlockEntity.CLIENT_REMOVE_LISTENER = StrutInteractionHandler::removeOutlineShapes;

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath(StrutYourStuff.MOD_ID, "strut_meshes");
            }

            @Override
            public Collection<Identifier> getFabricDependencies() {
                return Set.of();
            }

            @Override
            public java.util.concurrent.CompletableFuture<Void> reload(SharedState sharedState,
                                                                       java.util.concurrent.Executor backgroundExecutor,
                                                                       PreparationBarrier barrier,
                                                                       java.util.concurrent.Executor gameExecutor) {
                return barrier.wait(null).thenRunAsync(StrutModelManipulator::invalidateMeshes, gameExecutor);
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            Microliner.get().tick();
            if (client.player != null) {
                StrutPlacementEffects.tick(client.player);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(StrutInteractionHandler::onClientTick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            StrutInteractionHandler.clearSelection();
            final net.minecraft.client.multiplayer.ClientLevel level = client.level;
            if (level != null) {
                LevelSafeStorage.clearAll(level);
                ChunkedMap.unbindAll(level);
            }
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> StrutInteractionHandler.clearSelection());
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> ChunkedMap.evictChunk(level, chunk.getPos()));

        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(context -> {
            Microliner.get().render(context);
            StrutInteractionHandler.renderSelection(context);
        });

        LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) ->
                StrutInteractionHandler.selectedShape == null);

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) ->
                StrutInteractionHandler.onClientAttack());

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!level.isClientSide()) {
                return InteractionResult.PASS;
            }
            return StrutInteractionHandler.onClientUse(Minecraft.getInstance()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }
}
