package dev.brian.saplingsavior;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * FIFO of pending plantings with a per-tick budget and an expiry, so a burst of drops (a felled
 * forest) is spread over several ticks and stale entries never pile up. Pure, so unit-testable.
 */
public final class PlantQueue<T> {
	/** Entries older than this many ticks are dropped instead of processed. */
	public static final int EXPIRE_TICKS = 100;

	private record Entry<T>(T value, long tick) {}

	private final ArrayDeque<Entry<T>> entries = new ArrayDeque<>();

	public void add(T value, long tick) {
		entries.add(new Entry<>(value, tick));
	}

	/** Removes and returns up to {@code max} live entries, oldest first. Expired ones are discarded. */
	public List<T> take(long nowTick, int max) {
		List<T> out = new ArrayList<>();
		while (out.size() < max && !entries.isEmpty()) {
			Entry<T> e = entries.poll();
			if (nowTick - e.tick() <= EXPIRE_TICKS) out.add(e.value());
		}
		return out;
	}

	public int size() {
		return entries.size();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public void clear() {
		entries.clear();
	}
}
