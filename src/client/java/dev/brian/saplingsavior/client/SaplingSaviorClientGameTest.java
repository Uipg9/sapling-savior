package dev.brian.saplingsavior.client;

import dev.brian.saplingsavior.SaplingSavior;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Drops saplings under a leaf canopy and asserts they plant themselves, respecting the 4-block
 * spacing, with dark oak exempt for 2x2 clusters. Run with {@code .\gradlew.bat runClientGameTest}.
 */
public class SaplingSaviorClientGameTest implements FabricClientGameTest {
	private static void cmd(TestServerContext server, String fmt, Object... args) {
		server.runCommand(String.format(fmt, args));
	}

	private static void drop(TestServerContext server, String sapling, int x, int y, int z) {
		cmd(server, "summon item %d.5 %d %d.5 {Item:{id:\"%s\",count:1}}", x, y, z, sapling);
	}

	private static boolean isBlock(TestServerContext server, BlockPos pos, Block block) {
		return server.computeOnServer(s -> s.overworld().getBlockState(pos).is(block));
	}

	private static int itemsNear(TestServerContext server, BlockPos pos) {
		return server.computeOnServer(s -> s.overworld()
				.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.9)).size());
	}

	private static void expect(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!FabricLoader.getInstance().isModLoaded(SaplingSavior.MOD_ID)) {
			throw new AssertionError(SaplingSavior.MOD_ID + " is not loaded");
		}

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(100);
			TestServerContext server = world.getServer();
			BlockPos p = server.computeOnServer(s -> s.getPlayerList().getPlayers().get(0).blockPosition());
			int x = p.getX() + 3, y = p.getY(), z = p.getZ();

			// Flat dirt platform, clear air above, and persistent leaves overhead as the "tree".
			cmd(server, "fill %d %d %d %d %d %d dirt", x, y - 1, z, x + 12, y - 1, z + 12);
			cmd(server, "fill %d %d %d %d %d %d air", x, y, z, x + 12, y + 8, z + 12);
			cmd(server, "fill %d %d %d %d %d %d oak_leaves[persistent=true]", x, y + 5, z, x + 12, y + 5, z + 12);
			context.waitTicks(5);

			// 1) first sapling plants; 2) one 1 block away stays an item; 3) one 6 blocks away plants.
			drop(server, "oak_sapling", x + 1, y + 3, z + 1);
			context.waitTicks(10);
			drop(server, "oak_sapling", x + 2, y + 3, z + 1);
			drop(server, "oak_sapling", x + 7, y + 3, z + 1);
			context.waitTicks(10);
			expect(isBlock(server, new BlockPos(x + 1, y, z + 1), Blocks.OAK_SAPLING), "first oak sapling did not plant");
			expect(!isBlock(server, new BlockPos(x + 2, y, z + 1), Blocks.OAK_SAPLING), "sapling 1 block away must not plant");
			expect(itemsNear(server, new BlockPos(x + 2, y, z + 1)) == 1, "rejected sapling must remain as an item");
			expect(isBlock(server, new BlockPos(x + 7, y, z + 1), Blocks.OAK_SAPLING), "sapling 6 blocks away should plant");

			// 4) dark oak 2x2: all four plant even though they are adjacent.
			for (int[] off : new int[][] { { 1, 8 }, { 2, 8 }, { 1, 9 }, { 2, 9 } }) {
				drop(server, "dark_oak_sapling", x + off[0], y + 3, z + off[1]);
				context.waitTicks(10);
			}
			for (int[] off : new int[][] { { 1, 8 }, { 2, 8 }, { 1, 9 }, { 2, 9 } }) {
				expect(isBlock(server, new BlockPos(x + off[0], y, z + off[1]), Blocks.DARK_OAK_SAPLING),
						"dark oak 2x2 missing at " + off[0] + "," + off[1]);
			}
			context.takeScreenshot("planted");

			fastLeafDecay(context, server, x, y + 12, z + 20);
		}
	}

	private static void leaf(TestServerContext server, int x, int y, int z, int distance, boolean persistent) {
		cmd(server, "setblock %d %d %d oak_leaves[distance=%d,persistent=%s]", x, y, z, distance, persistent);
	}

	/** A log with a few leaves around it, plus a 3-leaf chain going out to the east. */
	private static void buildTree(TestServerContext server, int x, int y, int z, boolean persistent) {
		cmd(server, "setblock %d %d %d oak_log", x, y, z);
		leaf(server, x - 1, y, z, 1, persistent);
		leaf(server, x, y, z - 1, 1, persistent);
		leaf(server, x, y, z + 1, 1, persistent);
		leaf(server, x, y + 1, z, 1, persistent);
		leaf(server, x, y - 1, z, 1, persistent);
		leaf(server, x + 1, y, z, 1, persistent);
		leaf(server, x + 2, y, z, 2, persistent);
		leaf(server, x + 3, y, z, 3, persistent);
	}

	private static int leavesNear(TestServerContext server, int x, int y, int z) {
		return server.computeOnServer(s -> {
			int n = 0;
			for (BlockPos pos : BlockPos.betweenClosed(x - 2, y - 2, z - 2, x + 5, y + 2, z + 2)) {
				if (s.overworld().getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) n++;
			}
			return n;
		});
	}

	private void fastLeafDecay(ClientGameTestContext context, TestServerContext server, int x, int y, int z) {
		cmd(server, "gamerule random_tick_speed 0"); // so only our scheduled-tick decay can remove leaves
		int bx = x + 10, cx = x + 20;
		buildTree(server, x, y, z, false); // A: log removed, must decay
		buildTree(server, bx, y, z, true); // B: persistent, log removed, must stay
		buildTree(server, cx, y, z, false); // C: log stays, must stay
		context.waitTicks(40);
		expect(leavesNear(server, x, y, z) == 8, "tree A leaves vanished before its log was removed");

		cmd(server, "setblock %d %d %d air", x, y, z);
		cmd(server, "setblock %d %d %d air", bx, y, z);
		context.waitTicks(300);
		expect(leavesNear(server, x, y, z) == 0, "leaves without a log did not decay quickly: " + leavesNear(server, x, y, z) + " left");
		expect(leavesNear(server, bx, y, z) == 8, "persistent leaves must not decay");
		expect(leavesNear(server, cx, y, z) == 8, "leaves next to a log must not decay");
	}
}
