package dev.brian.saplingsavior;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SaplingRulesTest {
	private static SaplingRules.Neighbor n(String type, int dx, int dy, int dz) {
		return new SaplingRules.Neighbor(type, dx, dy, dz);
	}

	@Test
	void emptyAreaIsClear() {
		assertTrue(SaplingRules.canPlant("oak_sapling", List.of()));
	}

	@Test
	void neighborAtMinDistanceIsAllowed() {
		assertTrue(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 4, 0, 0))));
	}

	@Test
	void neighborInsideMinDistanceBlocks() {
		assertFalse(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 3, 0, 0))));
		assertFalse(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 1, 0, 0))));
	}

	@Test
	void distanceIsEuclideanNotManhattan() {
		// (3,3): manhattan 6 but euclidean ~4.24, so allowed. (2,3): ~3.6, blocked.
		assertTrue(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 3, 0, 3))));
		assertFalse(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 2, 0, 3))));
	}

	@Test
	void differentSpeciesStillBlock() {
		assertFalse(SaplingRules.canPlant("birch_sapling", List.of(n("oak_sapling", 2, 0, 0))));
	}

	@Test
	void farAboveOrBelowIsIgnored() {
		assertTrue(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 1, 6, 0))));
		assertFalse(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 1, 2, 0))));
	}

	@Test
	void darkOakAllowsAdjacentDarkOakForTwoByTwo() {
		assertTrue(SaplingRules.canPlant("dark_oak_sapling", List.of(n("dark_oak_sapling", 1, 0, 0))));
		assertTrue(SaplingRules.canPlant("dark_oak_sapling", List.of(
				n("dark_oak_sapling", 1, 0, 0), n("dark_oak_sapling", 0, 0, 1), n("dark_oak_sapling", 1, 0, 1))));
	}

	@Test
	void jungleAllowsAdjacentJungleForTwoByTwo() {
		assertTrue(SaplingRules.canPlant("jungle_sapling", List.of(n("jungle_sapling", 0, 0, 1))));
	}

	@Test
	void twoByTwoGroupIsCappedAtFour() {
		// Three companions already exist, so the cluster is full.
		assertFalse(SaplingRules.canPlant("dark_oak_sapling", List.of(
				n("dark_oak_sapling", 1, 0, 0), n("dark_oak_sapling", 0, 0, 1),
				n("dark_oak_sapling", 1, 0, 1), n("dark_oak_sapling", -1, 0, 0))));
	}

	@Test
	void darkOakNextToOtherSpeciesStillBlocked() {
		assertFalse(SaplingRules.canPlant("dark_oak_sapling", List.of(n("oak_sapling", 1, 0, 0))));
	}

	@Test
	void darkOakAdjacentToFarOakIsStillFine() {
		assertTrue(SaplingRules.canPlant("dark_oak_sapling", List.of(
				n("dark_oak_sapling", 1, 0, 0), n("oak_sapling", 5, 0, 0))));
	}

	@Test
	void oakIsNotExemptFromAdjacentOak() {
		assertFalse(SaplingRules.canPlant("oak_sapling", List.of(n("oak_sapling", 1, 0, 0))));
	}

	@Test
	void exemptSpeciesList() {
		assertTrue(SaplingRules.isTwoByTwoSpecies("dark_oak_sapling"));
		assertTrue(SaplingRules.isTwoByTwoSpecies("jungle_sapling"));
		assertFalse(SaplingRules.isTwoByTwoSpecies("spruce_sapling"));
	}
}
