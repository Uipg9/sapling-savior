package dev.brian.saplingsavior.mixin;

import dev.brian.saplingsavior.LeafDecayRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fast leaf decay. Vanilla leaves only decay on a random tick, which takes minutes. Here the
 * scheduled tick that already runs when a log disappears carries the decay through: the first
 * tick marks the leaf as decaying and schedules another after a short random delay, and that
 * second tick removes it with the normal drops. Idea: Quick Leaf Decay and Leaves Us In Peace (CC0).
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
	@Shadow
	protected abstract boolean decaying(BlockState state);

	@Shadow
	protected abstract void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random);

	@Inject(method = "tick", at = @At("TAIL"))
	private void saplingSavior$fastDecay(BlockState before, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		BlockState now = level.getBlockState(pos);
		if (!(now.getBlock() instanceof LeavesBlock)) return;
		switch (LeafDecayRules.next(decaying(before), decaying(now))) {
			case SCHEDULE -> level.scheduleTick(pos, (Block) (Object) this, LeafDecayRules.delay(random::nextInt));
			case REMOVE -> randomTick(now, level, pos, random); // vanilla removal + drops
			case NONE -> {}
		}
	}
}
