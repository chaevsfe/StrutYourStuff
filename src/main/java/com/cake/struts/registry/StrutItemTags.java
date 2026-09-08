package com.cake.struts.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class StrutItemTags {

    public static final TagKey<Item> WRENCHES = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("struts", "wrenches"));

}
