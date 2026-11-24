package dev.brian.saplingsavior;

import java.util.List;

/** Pure spacing rules, kept free of Minecraft types so they are unit-testable without bootstrap. */
public final class SaplingRules {
	public static final int MIN_DISTANCE = 4;
	/** Neighbors further than this vertically are on a different "level" and ignored. */
	public static final int VERTICAL_RANGE = 3;
	/** Max saplings in a 2x2 cluster: the 4th may join 3 companions, a 5th may not. */
	private static final int CLUSTER_SIZE = 4;

	private SaplingRules() {}

	/** A sapling already in the world, relative to the candidate spot. */
	public record Neighbor(String type, int dx, int dy, int dz) {}

	/** Species that need four saplings in a 2x2 to grow their big variant. */
	public static boolean isTwoByTwoSpecies(String saplingPath) {
		return saplingPath.equals("dark_oak_sapling") || saplingPath.equals("jungle_sapling");
	}

	public static boolean canPlant(String type, List<Neighbor> neighbors) {
		int companions = 0;
		for (Neighbor n : neighbors) {
			if (Math.abs(n.dy()) > VERTICAL_RANGE) continue;
			if (isCompanion(type, n)) {
				companions++;
				continue;
			}
			if (n.dx() * n.dx() + n.dz() * n.dz() < MIN_DISTANCE * MIN_DISTANCE) return false;
		}
		return companions < CLUSTER_SIZE;
	}

	/** Same 2x2 species, directly adjacent (incl. diagonal) on the same level. */
	private static boolean isCompanion(String type, Neighbor n) {
		return isTwoByTwoSpecies(type) && type.equals(n.type())
				&& n.dy() == 0 && Math.abs(n.dx()) <= 1 && Math.abs(n.dz()) <= 1;
	}
}
