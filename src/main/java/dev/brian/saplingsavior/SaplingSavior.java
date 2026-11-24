package dev.brian.saplingsavior;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saplings that drop from chopped trees or decaying leaves plant themselves, as long as no other
 * sapling is within {@link SaplingRules#MIN_DISTANCE} blocks. Items that cannot be planted are
 * left alone, so nothing is ever lost.
 */
public class SaplingSavior implements ModInitializer {
	public static final String MOD_ID = "sapling_savior";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** How far down an item may "fall" to find ground. */
	private static final int MAX_FALL = 48;
	/** A drop only counts when leaves are this close, which excludes chests and the like. */
	private static final int LEAF_RANGE = 6;

	private final List<Pending> pending = new ArrayList<>();

	private record Pending(ServerLevel level, ItemEntity item, BlockPos origin) {}

	@Override
	public void onInitialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && isFreshLootDrop(item) && saplingBlockOf(item.getItem()) != null) {
				pending.add(new Pending(level, item, item.blockPosition()));
			}
		});
		// Planting happens at the end of the tick, never inside the loot/decay code that spawned the item.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (pending.isEmpty()) return;
			List<Pending> batch = new ArrayList<>(pending);
			pending.clear();
			for (Pending p : batch) {
				if (!p.item().isRemoved()) tryPlant(p);
			}
		});
	}

	/** Loot drops are brand new and have no thrower; this skips player-thrown and reloaded items. */
	private static boolean isFreshLootDrop(ItemEntity item) {
		return item.getAge() == 0 && item.getOwner() == null;
	}

	private static SaplingBlock saplingBlockOf(ItemStack stack) {
		if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof SaplingBlock sapling) return sapling;
		return null;
	}

	private static void tryPlant(Pending p) {
		ServerLevel level = p.level();
		ItemEntity item = p.item();
		SaplingBlock sapling = saplingBlockOf(item.getItem());
		if (sapling == null || !hasLeavesNearby(level, p.origin())) return;

		BlockPos target = findLanding(level, p.origin());
		if (target == null) return;
		BlockState state = sapling.defaultBlockState();
		if (!level.getBlockState(target).canBeReplaced() || !level.getFluidState(target).isEmpty()
				|| !state.canSurvive(level, target)) return;

		String type = BuiltInRegistries.BLOCK.getKey(sapling).getPath();
		if (!SaplingRules.canPlant(type, neighborsAround(level, target))) return;

		level.setBlockAndUpdate(target, state);
		ItemStack stack = item.getItem();
		stack.shrink(1);
		if (stack.isEmpty()) item.discard();
		else item.setItem(stack);
	}

	/** First open spot above solid ground below the drop, falling through air and leaves. */
	private static BlockPos findLanding(ServerLevel level, BlockPos from) {
		BlockPos.MutableBlockPos pos = from.mutable();
		for (int i = 0; i < MAX_FALL; i++) {
			BlockState here = level.getBlockState(pos);
			if (!(here.isAir() || here.is(BlockTags.LEAVES) || here.canBeReplaced())) return null;
			BlockPos below = pos.below();
			if (below.getY() < level.getMinY()) return null;
			BlockState belowState = level.getBlockState(below);
			if (!(belowState.isAir() || belowState.is(BlockTags.LEAVES) || belowState.canBeReplaced())) {
				return here.is(BlockTags.LEAVES) ? null : pos.immutable();
			}
			pos.move(0, -1, 0);
		}
		return null;
	}

	private static boolean hasLeavesNearby(ServerLevel level, BlockPos center) {
		int r = LEAF_RANGE;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (level.getBlockState(pos).is(BlockTags.LEAVES)) return true;
		}
		return false;
	}

	private static List<SaplingRules.Neighbor> neighborsAround(ServerLevel level, BlockPos center) {
		List<SaplingRules.Neighbor> found = new ArrayList<>();
		int r = SaplingRules.MIN_DISTANCE - 1;
		int v = SaplingRules.VERTICAL_RANGE;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -v, -r), center.offset(r, v, r))) {
			Block block = level.getBlockState(pos).getBlock();
			if (block instanceof SaplingBlock) {
				found.add(new SaplingRules.Neighbor(BuiltInRegistries.BLOCK.getKey(block).getPath(),
						pos.getX() - center.getX(), pos.getY() - center.getY(), pos.getZ() - center.getZ()));
			}
		}
		return found;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Translation key following the vanilla convention: {@code category.modid.path}. */
	public static String langKey(String category, String path) {
		return category + "." + MOD_ID + "." + path;
	}
}
