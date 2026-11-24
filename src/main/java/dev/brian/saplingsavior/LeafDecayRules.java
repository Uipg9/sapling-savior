package dev.brian.saplingsavior;

import java.util.function.IntUnaryOperator;

/** Pure fast-leaf-decay rules, free of Minecraft types so they are unit-testable. */
public final class LeafDecayRules {
	/** Ticks between a leaf becoming "decaying" and it being removed. Randomised so canopies thin out. */
	public static final int MIN_DELAY = 2;
	public static final int MAX_DELAY = 30;

	private LeafDecayRules() {}

	public enum Step { NONE, SCHEDULE, REMOVE }

	/** @param nextInt returns a value in {@code [0, bound)} (e.g. {@code RandomSource::nextInt}) */
	public static int delay(IntUnaryOperator nextInt) {
		return MIN_DELAY + nextInt.applyAsInt(MAX_DELAY - MIN_DELAY + 1);
	}

	/**
	 * What to do after a leaf's scheduled tick has recomputed its distance.
	 *
	 * @param wasDecaying decaying before this tick ran
	 * @param nowDecaying decaying after the distance update
	 */
	public static Step next(boolean wasDecaying, boolean nowDecaying) {
		if (!nowDecaying) return Step.NONE;
		return wasDecaying ? Step.REMOVE : Step.SCHEDULE;
	}
}
