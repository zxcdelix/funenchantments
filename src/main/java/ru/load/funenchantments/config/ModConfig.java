package ru.load.funenchantments.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.load.funenchantments.FunEnchantments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("funenchantments.json");

    public static ModConfig INSTANCE = new ModConfig();

    public int filterLevel1Stacks = 1;
    public int filterLevel2Stacks = 2;
    public int filterLevel3Stacks = 4;

    public double forestersFortuneExtraDropChancePerLevel = 0.08;
    public double diggersFortuneExtraDropChancePerLevel = 0.08;

    public int scanLevel1Radius = 2;
    public int scanLevel2Radius = 3;
    public int scanLevel3Radius = 5;
    public int scanDurationTicks = 60;

    public int lumberjackLevel1MaxLogs = 9;
    public int lumberjackLevel2MaxLogs = 512;

    public boolean pingerBlockExpensiveToolsAtOnePercent = true;

    public static void load() {
        try {
            if (Files.notExists(PATH)) {
                save();
                return;
            }

            String json = Files.readString(PATH);
            INSTANCE = GSON.fromJson(json, ModConfig.class);

            if (INSTANCE == null) {
                INSTANCE = new ModConfig();
                save();
            }
        } catch (Exception e) {
            FunEnchantments.LOGGER.warn("Failed to load config, using defaults", e);
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            FunEnchantments.LOGGER.warn("Failed to save config", e);
        }
    }
}