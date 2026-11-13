package ru.load.funenchantments.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import ru.load.funenchantments.config.ModConfig;
import ru.load.funenchantments.enchant.ModEnchantments;
import ru.load.funenchantments.tag.ModTags;
import ru.load.funenchantments.util.EnchantUtil;
import ru.load.funenchantments.util.InventoryUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class BlockBreakHandler {
    private static final Set<BlockPos> INTERNAL_BREAKS = new HashSet<>();
    private static final List<PendingMagnet> PENDING_MAGNETS = new ArrayList<>();
    private static final List<PendingScan> PENDING_SCANS = new ArrayList<>();

    private BlockBreakHandler() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            if (INTERNAL_BREAKS.contains(pos)) {
                return;
            }

            ItemStack tool = serverPlayer.getMainHandStack();

            int autoSmelt = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.AUTO_SMELT);
            int magnet = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.MAGNET);
            int filter = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.FILTER);
            int drill = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.DRILL);
            int scan = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.SCAN);
            int lumberjack = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.LUMBERJACK);
            int forester = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.FORESTERS_FORTUNE);
            int digger = EnchantUtil.getLevel(serverWorld, tool, ModEnchantments.DIGGERS_FORTUNE);

            if (autoSmelt > 0 || magnet > 0 || filter > 0) {
                PENDING_MAGNETS.add(new PendingMagnet(
                        serverWorld,
                        serverPlayer,
                        pos.toImmutable(),
                        autoSmelt > 0,
                        filter,
                        2
                ));
            }

            if (scan > 0 && isOre(state)) {
                addScan(serverWorld, pos, state, scan);
            }

            if (drill > 0 && isMiningTool(tool)) {
                breakDrillArea(serverWorld, serverPlayer, pos, state, drill, autoSmelt > 0, filter);
            }

            if (lumberjack > 0 && tool.isIn(ItemTags.AXES) && isLog(state)) {
                breakTree(serverWorld, serverPlayer, pos, state, lumberjack, forester);
            }

            if (lumberjack <= 0 && forester > 0 && tool.isIn(ItemTags.AXES) && isLog(state)) {
                if (serverWorld.random.nextFloat() < ModConfig.INSTANCE.forestersFortuneExtraDropChancePerLevel * forester) {
                    InventoryUtil.giveOrDrop(serverPlayer, new ItemStack(state.getBlock().asItem()));
                }
            }

            if (digger > 0 && tool.isIn(ItemTags.SHOVELS)) {
                giveExtraDrops(serverWorld, serverPlayer, pos, state, blockEntity, tool, digger, filter);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickMagnets();
            tickScans();
        });
    }

    private static void tickMagnets() {
        Iterator<PendingMagnet> iterator = PENDING_MAGNETS.iterator();

        while (iterator.hasNext()) {
            PendingMagnet pending = iterator.next();
            pending.ticks--;

            if (pending.ticks > 0) {
                continue;
            }

            ServerWorld world = pending.world;
            ServerPlayerEntity player = pending.player;

            if (world == null || player == null || player.isRemoved()) {
                iterator.remove();
                continue;
            }

            List<ItemEntity> items = world.getEntitiesByClass(
                    ItemEntity.class,
                    new Box(pending.pos).expand(2.0),
                    item -> item.isAlive() && !item.getStack().isEmpty()
            );

            for (ItemEntity itemEntity : items) {
                ItemStack stack = itemEntity.getStack().copy();

                if (pending.autoSmelt) {
                    stack = InventoryUtil.trySmelt(world, stack);
                }

                InventoryUtil.giveOrDropWithFilter(player, stack, pending.filterLevel);
                itemEntity.discard();
            }

            iterator.remove();
        }
    }

    private static void tickScans() {
        Iterator<PendingScan> iterator = PENDING_SCANS.iterator();

        while (iterator.hasNext()) {
            PendingScan scan = iterator.next();

            if (scan.world == null) {
                iterator.remove();
                continue;
            }

            if (scan.ticksLeft <= 0) {
                iterator.remove();
                continue;
            }

            if (scan.ticksLeft % 5 == 0) {
                for (BlockPos pos : scan.positions) {
                    if (scan.world.getBlockState(pos).getBlock() == scan.block) {
                        scan.world.spawnParticles(
                                ParticleTypes.END_ROD,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                3,
                                0.35,
                                0.35,
                                0.35,
                                0.01
                        );
                    }
                }
            }

            scan.ticksLeft--;
        }
    }

    private static void breakDrillArea(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos center,
            BlockState originalState,
            int level,
            boolean autoSmelt,
            int filterLevel
    ) {
        int radius = 1;
        int depth = level >= 2 ? 1 : 0;

        List<BlockPos> positions = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -depth; z <= depth; z++) {
                    BlockPos target = center.add(x, y, z);

                    if (!target.equals(center)) {
                        positions.add(target.toImmutable());
                    }
                }
            }
        }

        int maxBlocks = level >= 2 ? 26 : 8;
        int broken = 0;

        for (BlockPos target : positions) {
            if (broken >= maxBlocks) {
                break;
            }

            BlockState state = world.getBlockState(target);

            if (!canBreakExtraBlock(world, target, state)) {
                continue;
            }

            if (!sameMiningCategory(originalState, state)) {
                continue;
            }

            breakExtraBlock(world, player, target, autoSmelt, filterLevel);
            broken++;
        }
    }

    private static void breakTree(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos start,
            BlockState startState,
            int level,
            int foresterLevel
    ) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start.toImmutable());
        visited.add(start.toImmutable());

        int maxLogs = level >= 2
                ? ModConfig.INSTANCE.lumberjackLevel2MaxLogs
                : ModConfig.INSTANCE.lumberjackLevel1MaxLogs;

        int broken = 0;

        while (!queue.isEmpty() && broken < maxLogs) {
            BlockPos current = queue.poll();

            for (BlockPos next : nearTreePositions(current)) {
                if (visited.contains(next)) {
                    continue;
                }

                BlockState nextState = world.getBlockState(next);

                if (isLog(nextState) && sameBlockFamily(startState, nextState)) {
                    visited.add(next.toImmutable());
                    queue.add(next.toImmutable());
                }
            }

            if (!current.equals(start)) {
                breakExtraBlock(world, player, current, false, 0);
                broken++;

                if (foresterLevel > 0 && world.random.nextFloat() < ModConfig.INSTANCE.forestersFortuneExtraDropChancePerLevel * foresterLevel) {
                    InventoryUtil.giveOrDrop(player, new ItemStack(startState.getBlock().asItem()));
                }
            }
        }
    }

    private static void giveExtraDrops(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool,
            int diggerLevel,
            int filterLevel
    ) {
        double chance = ModConfig.INSTANCE.diggersFortuneExtraDropChancePerLevel * diggerLevel;

        if (world.random.nextFloat() > chance) {
            return;
        }

        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, blockEntity, player, tool);

        for (ItemStack drop : drops) {
            InventoryUtil.giveOrDropWithFilter(player, drop.copy(), filterLevel);
        }
    }

    private static void breakExtraBlock(
            ServerWorld world,
            ServerPlayerEntity player,
            BlockPos pos,
            boolean autoSmelt,
            int filterLevel
    ) {
        BlockState state = world.getBlockState(pos);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        ItemStack tool = player.getMainHandStack();

        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, blockEntity, player, tool);

        INTERNAL_BREAKS.add(pos.toImmutable());

        try {
            world.breakBlock(pos, false, player);
        } finally {
            INTERNAL_BREAKS.remove(pos.toImmutable());
        }

        for (ItemStack drop : drops) {
            ItemStack finalDrop = autoSmelt ? InventoryUtil.trySmelt(world, drop.copy()) : drop.copy();
            InventoryUtil.giveOrDropWithFilter(player, finalDrop, filterLevel);
        }
    }

    private static void addScan(ServerWorld world, BlockPos center, BlockState minedState, int level) {
        int radius = switch (level) {
            case 1 -> ModConfig.INSTANCE.scanLevel1Radius;
            case 2 -> ModConfig.INSTANCE.scanLevel2Radius;
            default -> ModConfig.INSTANCE.scanLevel3Radius;
        };

        List<BlockPos> positions = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (state.getBlock() == minedState.getBlock()) {
                        positions.add(pos.toImmutable());
                    }
                }
            }
        }

        if (!positions.isEmpty()) {
            PENDING_SCANS.add(new PendingScan(
                    world,
                    minedState.getBlock(),
                    positions,
                    ModConfig.INSTANCE.scanDurationTicks
            ));
        }
    }

    private static boolean canBreakExtraBlock(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }

        if (state.getBlock() == Blocks.BEDROCK) {
            return false;
        }

        return state.getHardness(world, pos) >= 0;
    }

    private static List<BlockPos> nearTreePositions(BlockPos pos) {
        List<BlockPos> result = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    result.add(pos.add(x, y, z));
                }
            }
        }

        return result;
    }

    private static boolean isOre(BlockState state) {
        return state.isIn(ModTags.COMMON_ORES)
                || state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES)
                || state.isIn(BlockTags.EMERALD_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES);
    }

    private static boolean isLog(BlockState state) {
        return state.isIn(BlockTags.LOGS);
    }

    private static boolean sameBlockFamily(BlockState a, BlockState b) {
        return a.getBlock() == b.getBlock();
    }

    private static boolean sameMiningCategory(BlockState original, BlockState target) {
        if (isOre(original)) {
            return isOre(target);
        }

        if (original.isIn(BlockTags.PICKAXE_MINEABLE)) {
            return target.isIn(BlockTags.PICKAXE_MINEABLE);
        }

        if (original.isIn(BlockTags.SHOVEL_MINEABLE)) {
            return target.isIn(BlockTags.SHOVEL_MINEABLE);
        }

        if (original.isIn(BlockTags.AXE_MINEABLE)) {
            return target.isIn(BlockTags.AXE_MINEABLE);
        }

        return original.getBlock() == target.getBlock();
    }

    private static boolean isMiningTool(ItemStack stack) {
        return stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.isIn(ItemTags.HOES);
    }

    private static final class PendingMagnet {
        private final ServerWorld world;
        private final ServerPlayerEntity player;
        private final BlockPos pos;
        private final boolean autoSmelt;
        private final int filterLevel;
        private int ticks;

        private PendingMagnet(ServerWorld world, ServerPlayerEntity player, BlockPos pos, boolean autoSmelt, int filterLevel, int ticks) {
            this.world = world;
            this.player = player;
            this.pos = pos;
            this.autoSmelt = autoSmelt;
            this.filterLevel = filterLevel;
            this.ticks = ticks;
        }
    }

    private static final class PendingScan {
        private final ServerWorld world;
        private final Block block;
        private final List<BlockPos> positions;
        private int ticksLeft;

        private PendingScan(ServerWorld world, Block block, List<BlockPos> positions, int ticksLeft) {
            this.world = world;
            this.block = block;
            this.positions = positions;
            this.ticksLeft = ticksLeft;
        }
    }
}