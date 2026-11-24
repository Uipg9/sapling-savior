package dev.brian.saplingsavior;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SaplingSaviorTest {
	@Test
	void idUsesModNamespace() {
		var id = SaplingSavior.id("example");
		assertEquals(SaplingSavior.MOD_ID, id.getNamespace());
		assertEquals("example", id.getPath());
	}

	@Test
	void langKeyFollowsMinecraftConvention() {
		assertEquals("item.sapling_savior.example", SaplingSavior.langKey("item", "example"));
	}
}
