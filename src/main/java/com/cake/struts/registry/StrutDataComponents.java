package com.cake.struts.registry;

import com.cake.struts.StrutYourStuff;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.function.UnaryOperator;

public class StrutDataComponents {

    public static DataComponentType<BlockPos> GIRDER_STRUT_FROM;
    public static DataComponentType<Direction> GIRDER_STRUT_FROM_FACE;

    public static void register() {
        GIRDER_STRUT_FROM = register("girder_strut_from",
                b -> b.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
        GIRDER_STRUT_FROM_FACE = register("girder_strut_from_face",
                b -> b.persistent(Direction.CODEC).networkSynchronized(Direction.STREAM_CODEC));
    }

    private static <T> DataComponentType<T> register(final String name, final UnaryOperator<DataComponentType.Builder<T>> op) {
        final DataComponentType<T> type = op.apply(DataComponentType.builder()).build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(StrutYourStuff.MOD_ID, name), type);
    }
}
