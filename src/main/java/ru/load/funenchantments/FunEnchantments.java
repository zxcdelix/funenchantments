package ru.load.funenchantments;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.load.funenchantments.config.ModConfig;
import ru.load.funenchantments.event.BlockBreakHandler;
import ru.load.funenchantments.event.PingerHandler;

public class FunEnchantments implements ModInitializer {
    public static final String MOD_ID = "funenchantments";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();

        BlockBreakHandler.register();
        PingerHandler.register();

        LOGGER.info("Fun Enchantments loaded!");
    }
}