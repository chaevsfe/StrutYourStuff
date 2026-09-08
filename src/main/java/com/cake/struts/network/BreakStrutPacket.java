package com.cake.struts.network;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.content.structure.ConnectionKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record BreakStrutPacket(ConnectionKey target, boolean isWrench) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BreakStrutPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(StrutYourStuff.MOD_ID, "break_strut"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BreakStrutPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.target().a());
                buf.writeBlockPos(packet.target().b());
                buf.writeBoolean(packet.isWrench());
            },
            buf -> new BreakStrutPacket(new ConnectionKey(buf.readBlockPos(), buf.readBlockPos()), buf.readBoolean())
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
