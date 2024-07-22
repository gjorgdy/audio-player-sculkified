package de.maxhenkel.audioplayer.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net/minecraft/world/level/block/entity/SculkSensorBlockEntity$VibrationUser")
public class SculkSensorBlockEntityMixin {

    @Final
    @Shadow
    protected BlockPos blockPos;

    @Inject(method = "canReceiveVibration", at = @At("HEAD"), cancellable = true)
    private void canReceiveVibration(ServerLevel serverLevel, BlockPos blockPos, Holder<GameEvent> holder, GameEvent.Context context, CallbackInfoReturnable<Boolean> cir) {
        BlockState blockBelow = serverLevel.getBlockState(this.blockPos.below());
        if (blockBelow.is(Blocks.NOTE_BLOCK)
            || blockBelow.is(Blocks.AMETHYST_BLOCK) && !isResonateEvent(holder)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean isResonateEvent(Holder<GameEvent> holder) {
        return holder.is(GameEvent.RESONATE_1)
                || holder.is(GameEvent.RESONATE_2)
                || holder.is(GameEvent.RESONATE_3)
                || holder.is(GameEvent.RESONATE_4)
                || holder.is(GameEvent.RESONATE_5)
                || holder.is(GameEvent.RESONATE_6)
                || holder.is(GameEvent.RESONATE_7)
                || holder.is(GameEvent.RESONATE_8)
                || holder.is(GameEvent.RESONATE_9)
                || holder.is(GameEvent.RESONATE_10)
                || holder.is(GameEvent.RESONATE_11)
                || holder.is(GameEvent.RESONATE_12)
                || holder.is(GameEvent.RESONATE_13)
                || holder.is(GameEvent.RESONATE_14)
                || holder.is(GameEvent.RESONATE_15);
    }

}
