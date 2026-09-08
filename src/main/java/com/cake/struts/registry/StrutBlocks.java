package com.cake.struts.registry;

import com.cake.struts.StrutYourStuff;
import com.cake.struts.content.structure.GirderStrutStructureBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class StrutBlocks {

    public static GirderStrutStructureBlock GIRDER_STRUT_STRUCTURE;

    public static void register() {
        final Identifier id = Identifier.fromNamespaceAndPath(StrutYourStuff.MOD_ID, "girder_strut_structure");
        final BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), id))
                .noLootTable()
                .replaceable()
                .noOcclusion()
                .noCollision()
                .pushReaction(PushReaction.DESTROY)
                .mapColor(MapColor.METAL)
                .strength(3f, 6f)
                .sound(SoundType.NETHERITE_BLOCK);
        GIRDER_STRUT_STRUCTURE = Registry.register(BuiltInRegistries.BLOCK, id, new GirderStrutStructureBlock(properties));
    }

    public static Block girderStrutStructure() {
        return GIRDER_STRUT_STRUCTURE;
    }
}
