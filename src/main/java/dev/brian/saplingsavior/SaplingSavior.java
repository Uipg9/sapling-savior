package dev.brian.saplingsavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
 *
 * <p>Tree-felling mods break logs and leaves in one tick, so "is there a tree here" is decided
 * when the item spawns. Planting itself is queued and budgeted per tick.
 */
public class SaplingSavior implements ModInitializer {
	public static final String MOD_ID = "sapling_savior";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** How far down an item may "fall" to find ground. */
	private static final int MAX_FALL = 48;
	/** A drop only counts when leaves or logs are this close, which excludes chests and the like. */
	private static final int TREE_RANGE = 6;
	/** Plantings per tick; a felled forest is spread over a few ticks instead of one long one. */
	private static final int MAX_PER_TICK = 32;

	private final PlantQueue<Pending> queue = new PlantQueue<>();

	private record Pending(ServerLevel level, ItemEntity item, BlockPos origin) {}

	@Override
	public void onInitialize() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && isFreshLootDrop(item) && saplingBlockOf(item.getItem()) != null
					&& hasTreeNearby(level, item.blockPosition())) {
				queue.add(new Pending(level, item, item.blockPosition()), level.getServer().getTickCount());
			}
		});
		// Planting happens at the end of the tick, never inside the loot/decay code that spawned the item.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (queue.isEmpty()) return;
			for (Pending p : queue.take(server.getTickCount(), MAX_PER_TICK)) {
				if (!p.item().isRemoved()) tryPlant(p);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> queue.clear());
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
		if (sapling == null) return;

		BlockPos target = findLanding(level, p.origin());
		if (target == null) return;
		BlockState state = sapling.defaultBlockState();
		// Another mod (a replanter, say) may have planted here first; then the spot is simply not free.
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

	/** Leaves or logs within range; returns on the first hit, so real trees are cheap to confirm. */
	private static boolean hasTreeNearby(ServerLevel level, BlockPos center) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int r = TREE_RANGE;
		for (int dy = -r; dy <= r; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					BlockState state = level.getBlockState(pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz));
					if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) return true;
				}
			}
		}
		return false;
	}

	private static List<SaplingRules.Neighbor> neighborsAround(ServerLevel level, BlockPos center) {
		List<SaplingRules.Neighbor> found = null;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int r = SaplingRules.MIN_DISTANCE - 1;
		int v = SaplingRules.VERTICAL_RANGE;
		for (int dy = -v; dy <= v; dy++) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					Block block = level.getBlockState(pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz)).getBlock();
					if (block instanceof SaplingBlock) {
						if (found == null) found = new ArrayList<>();
						found.add(new SaplingRules.Neighbor(BuiltInRegistries.BLOCK.getKey(block).getPath(), dx, dy, dz));
					}
				}
			}
		}
		return found == null ? Collections.emptyList() : found;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** Translation key following the vanilla convention: {@code category.modid.path}. */
	public static String langKey(String category, String path) {
		return category + "." + MOD_ID + "." + path;
	}
}
