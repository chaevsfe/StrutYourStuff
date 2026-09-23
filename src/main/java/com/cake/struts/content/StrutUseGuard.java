package com.cake.struts.content;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.registry.StrutItemTags;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

public final class StrutUseGuard {

    private static final Identifier PHASE = Identifier.fromNamespaceAndPath(StrutYourStuff.MOD_ID, "strut_use_guard");
    private static final Map<Player, Integer> BREAK_TICKS = new WeakHashMap<>();

    private StrutUseGuard() {
    }

    public static void register() {
        UseBlockCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.register(PHASE, (player, level, hand, hitResult) ->
                !level.isClientSide() && hand == InteractionHand.MAIN_HAND && consumeBreak(player)
                        ? InteractionResult.FAIL
                        : InteractionResult.PASS);
    }

    public static void markBreak(final ServerPlayer player) {
        if (player.getMainHandItem().is(StrutItemTags.WRENCHES)) {
            BREAK_TICKS.put(player, player.level().getServer().getTickCount());
        }
    }

    private static boolean consumeBreak(final Player player) {
        if (!(player instanceof final ServerPlayer serverPlayer)) {
            return false;
        }
        final Integer tick = BREAK_TICKS.remove(serverPlayer);
        return tick != null && serverPlayer.level().getServer().getTickCount() - tick <= 1;
    }
}
