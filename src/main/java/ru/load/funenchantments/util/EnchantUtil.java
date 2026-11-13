package ru.load.funenchantments.util;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;

public final class EnchantUtil {
    private EnchantUtil() {
    }

    public static int getLevel(ServerWorld world, ItemStack stack, RegistryKey<Enchantment> key) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);

        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(key)) {
                return enchantments.getLevel(entry);
            }
        }

        return 0;
    }
}