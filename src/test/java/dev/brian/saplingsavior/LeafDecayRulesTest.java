package dev.brian.saplingsavior;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LeafDecayRulesTest {
	@Test
	void delayStaysWithinBounds() {
		assertEquals(LeafDecayRules.MIN_DELAY, LeafDecayRules.delay(bound -> 0));
		assertEquals(LeafDecayRules.MAX_DELAY, LeafDecayRules.delay(bound -> bound - 1));
	}

	@Test
	void delayAsksForTheWholeRange() {
		int[] asked = new int[1];
		LeafDecayRules.delay(bound -> {
			asked[0] = bound;
			return 0;
		});
		assertEquals(LeafDecayRules.MAX_DELAY - LeafDecayRules.MIN_DELAY + 1, asked[0]);
	}

	@Test
	void healthyLeafDoesNothing() {
		assertEquals(LeafDecayRules.Step.NONE, LeafDecayRules.next(false, false));
		assertEquals(LeafDecayRules.Step.NONE, LeafDecayRules.next(true, false));
	}

	@Test
	void newlyDecayingLeafIsScheduled() {
		assertEquals(LeafDecayRules.Step.SCHEDULE, LeafDecayRules.next(false, true));
	}

	@Test
	void leafThatWasAlreadyDecayingIsRemoved() {
		assertEquals(LeafDecayRules.Step.REMOVE, LeafDecayRules.next(true, true));
	}
}
