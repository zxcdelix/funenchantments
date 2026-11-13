package ru.load.funenchantments.event;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import ru.load.funenchantments.config.ModConfig;
import ru.load.funenchantments.enchant.ModEnchantments;
import ru.load.funenchantments.util.EnchantUtil;
import net.minecraft.registry.tag.ItemTags;

public final class PingerHandler {
    private PingerHandler() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            ItemStack stack = serverPlayer.getStackInHand(hand);

            if (stack.isEmpty() || !stack.isDamageable()) {
                return ActionResult.PASS;
            }

            if (!isToolNoSword(stack)) {
                return ActionResult.PASS;
            }

            int pingerLevel = EnchantUtil.getLevel(serverWorld, stack, ModEnchantments.PINGER);

            if (pingerLevel <= 0) {
                return ActionResult.PASS;
            }

            int maxDamage = stack.getMaxDamage();
            int damage = stack.getDamage();
            int durabilityLeft = maxDamage - damage;

            double percent = durabilityLeft * 100.0 / maxDamage;

            if (percent <= 10.0 && percent > 1.0) {
                serverPlayer.sendMessage(Text.literal("⚠ Tool durability is below 10%!"), true);
            }

            if (percent <= 1.0 && ModConfig.INSTANCE.pingerBlockExpensiveToolsAtOnePercent && isExpensiveTool(stack)) {
                serverPlayer.sendMessage(Text.literal("⛔ Pinger blocked mining to save your tool!"), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isToolNoSword(ItemStack stack) {
        return stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.isIn(ItemTags.HOES);
    }

    private static boolean isExpensiveTool(ItemStack stack) {
        return stack.isOf(Items.DIAMOND_PICKAXE)
                || stack.isOf(Items.DIAMOND_AXE)
                || stack.isOf(Items.DIAMOND_SHOVEL)
                || stack.isOf(Items.DIAMOND_HOE)
                || stack.isOf(Items.NETHERITE_PICKAXE)
                || stack.isOf(Items.NETHERITE_AXE)
                || stack.isOf(Items.NETHERITE_SHOVEL)
                || stack.isOf(Items.NETHERITE_HOE)
                || stack.getMaxDamage() >= 1000;
    }
}