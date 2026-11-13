package ru.load.funenchantments.tag;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import ru.load.funenchantments.FunEnchantments;

public final class ModTags {
    public static final TagKey<Block> COMMON_ORES = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("c", "ores")
    );

    public static final TagKey<Item> ENCHANTABLE_AXES = itemTag("enchantable/axes");
    public static final TagKey<Item> ENCHANTABLE_SHOVELS = itemTag("enchantable/shovels");
    public static final TagKey<Item> TOOLS_NO_SWORDS = itemTag("enchantable/tools_no_swords");
    public static final TagKey<Item> MINING_TOOLS = itemTag("enchantable/mining_tools");

    private ModTags() {
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.of(
                RegistryKeys.ITEM,
                Identifier.of(FunEnchantments.MOD_ID, path)
        );
    }
}