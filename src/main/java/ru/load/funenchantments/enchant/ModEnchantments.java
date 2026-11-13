package ru.load.funenchantments.enchant;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import ru.load.funenchantments.FunEnchantments;

public final class ModEnchantments {
    public static final RegistryKey<Enchantment> AUTO_SMELT = key("auto_smelt");
    public static final RegistryKey<Enchantment> MAGNET = key("magnet");
    public static final RegistryKey<Enchantment> FILTER = key("filter");
    public static final RegistryKey<Enchantment> DRILL = key("drill");
    public static final RegistryKey<Enchantment> SCAN = key("scan");
    public static final RegistryKey<Enchantment> LUMBERJACK = key("lumberjack");
    public static final RegistryKey<Enchantment> FORESTERS_FORTUNE = key("foresters_fortune");
    public static final RegistryKey<Enchantment> DIGGERS_FORTUNE = key("diggers_fortune");
    public static final RegistryKey<Enchantment> PINGER = key("pinger");

    private ModEnchantments() {
    }

    private static RegistryKey<Enchantment> key(String name) {
        return RegistryKey.of(
                RegistryKeys.ENCHANTMENT,
                Identifier.of(FunEnchantments.MOD_ID, name)
        );
    }
}