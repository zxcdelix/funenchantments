package ru.load.funenchantments.util;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import ru.load.funenchantments.config.ModConfig;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class InventoryUtil {
    private static final Set<Item> TRASH_LVL_1 = new HashSet<>();
    private static final Set<Item> TRASH_LVL_2 = new HashSet<>();
    private static final Set<Item> TRASH_LVL_3 = new HashSet<>();

    static {
        TRASH_LVL_1.add(Items.COBBLESTONE);
        TRASH_LVL_1.add(Items.DIRT);
        TRASH_LVL_1.add(Items.GRAVEL);

        TRASH_LVL_2.addAll(TRASH_LVL_1);
        TRASH_LVL_2.add(Items.COBBLED_DEEPSLATE);
        TRASH_LVL_2.add(Items.NETHERRACK);
        TRASH_LVL_2.add(Items.SAND);
        TRASH_LVL_2.add(Items.RED_SAND);
        TRASH_LVL_2.add(Items.GRANITE);
        TRASH_LVL_2.add(Items.DIORITE);
        TRASH_LVL_2.add(Items.ANDESITE);
        TRASH_LVL_2.add(Items.TUFF);

        TRASH_LVL_3.addAll(TRASH_LVL_2);
        TRASH_LVL_3.add(Items.CALCITE);
        TRASH_LVL_3.add(Items.BLACKSTONE);
        TRASH_LVL_3.add(Items.BASALT);
        TRASH_LVL_3.add(Items.SOUL_SAND);
        TRASH_LVL_3.add(Items.SOUL_SOIL);
        TRASH_LVL_3.add(Items.MUD);
        TRASH_LVL_3.add(Items.PACKED_MUD);
        TRASH_LVL_3.add(Items.CLAY);
    }

    private InventoryUtil() {
    }

    public static ItemStack trySmelt(ServerWorld world, ItemStack inputStack) {
        if (inputStack.isEmpty()) {
            return inputStack;
        }

        SingleStackRecipeInput input = new SingleStackRecipeInput(inputStack);

        Optional<RecipeEntry<SmeltingRecipe>> recipe = world
                .getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, input, world);

        if (recipe.isEmpty()) {
            return inputStack;
        }

        ItemStack result = recipe.get().value().craft(input, world.getRegistryManager());

        if (result.isEmpty()) {
            return inputStack;
        }

        result.setCount(result.getCount() * inputStack.getCount());
        return result;
    }

    public static void giveOrDrop(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        boolean inserted = player.getInventory().insertStack(stack);

        if (!inserted && !stack.isEmpty()) {
            player.dropItem(stack, false);
        }
    }

    public static void giveOrDropWithFilter(ServerPlayerEntity player, ItemStack stack, int filterLevel) {
        if (stack.isEmpty()) {
            return;
        }

        boolean inserted = player.getInventory().insertStack(stack);

        if (!inserted && filterLevel > 0) {
            clearTrashStacks(player, filterLevel);
            inserted = player.getInventory().insertStack(stack);
        }

        if (!inserted && !stack.isEmpty()) {
            player.dropItem(stack, false);
        }
    }

    public static int clearTrashStacks(ServerPlayerEntity player, int level) {
        Set<Item> trash = getTrashSet(level);

        if (trash.isEmpty()) {
            return 0;
        }

        int maxStacksToDrop = switch (level) {
            case 1 -> ModConfig.INSTANCE.filterLevel1Stacks;
            case 2 -> ModConfig.INSTANCE.filterLevel2Stacks;
            default -> ModConfig.INSTANCE.filterLevel3Stacks;
        };

        int droppedStacks = 0;
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            if (droppedStacks >= maxStacksToDrop) {
                break;
            }

            ItemStack slot = inventory.getStack(i);

            if (slot.isEmpty()) {
                continue;
            }

            if (!trash.contains(slot.getItem())) {
                continue;
            }

            ItemStack removed = inventory.removeStack(i);

            if (!removed.isEmpty()) {
                ItemEntity entity = player.dropItem(removed, false);

                if (entity != null) {
                    entity.setPickupDelay(100);
                }

                droppedStacks++;
            }
        }

        return droppedStacks;
    }

    private static Set<Item> getTrashSet(int level) {
        if (level >= 3) {
            return TRASH_LVL_3;
        }

        if (level == 2) {
            return TRASH_LVL_2;
        }

        if (level == 1) {
            return TRASH_LVL_1;
        }

        return Set.of();
    }
}