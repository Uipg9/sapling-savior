package dev.brian.saplingsavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlantQueueTest {
	@Test
	void takesAtMostMaxAndKeepsTheRest() {
		PlantQueue<Integer> q = new PlantQueue<>();
		for (int i = 0; i < 5; i++) q.add(i, 0);
		assertEquals(List.of(0, 1), q.take(0, 2));
		assertEquals(3, q.size());
		assertEquals(List.of(2, 3, 4), q.take(1, 10));
		assertTrue(q.isEmpty());
	}

	@Test
	void keepsOrder() {
		PlantQueue<String> q = new PlantQueue<>();
		q.add("a", 0);
		q.add("b", 0);
		assertEquals(List.of("a", "b"), q.take(0, 10));
	}

	@Test
	void dropsEntriesOlderThanTheExpiry() {
		PlantQueue<String> q = new PlantQueue<>();
		q.add("old", 0);
		q.add("fresh", PlantQueue.EXPIRE_TICKS);
		assertEquals(List.of("fresh"), q.take(PlantQueue.EXPIRE_TICKS + 1, 10));
		assertTrue(q.isEmpty());
	}

	@Test
	void entryExactlyAtExpiryAgeIsStillTaken() {
		PlantQueue<String> q = new PlantQueue<>();
		q.add("edge", 0);
		assertEquals(List.of("edge"), q.take(PlantQueue.EXPIRE_TICKS, 10));
	}

	@Test
	void clearEmptiesTheQueue() {
		PlantQueue<String> q = new PlantQueue<>();
		q.add("a", 0);
		q.clear();
		assertTrue(q.isEmpty());
		assertEquals(0, q.size());
	}
}
