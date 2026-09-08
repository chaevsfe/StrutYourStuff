package com.cake.struts.network;

import com.cake.struts.content.StrutBreakerHelper;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class StrutPackets {

    private StrutPackets() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(BreakStrutPacket.TYPE, BreakStrutPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BreakStrutPacket.TYPE, (payload, context) ->
                context.server().execute(() -> StrutBreakerHelper.breakStrut(context.player(), payload.target(), payload.isWrench())));
    }
}
