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
class SculkSensorBlockEntityMixin {

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
        return holder == GameEvent.RESONATE_1
                || holder == GameEvent.RESONATE_2
                || holder == GameEvent.RESONATE_3
                || holder == GameEvent.RESONATE_4
                || holder == GameEvent.RESONATE_5
                || holder == GameEvent.RESONATE_6
                || holder == GameEvent.RESONATE_7
                || holder == GameEvent.RESONATE_8
                || holder == GameEvent.RESONATE_9
                || holder == GameEvent.RESONATE_10
                || holder == GameEvent.RESONATE_11
                || holder == GameEvent.RESONATE_12
                || holder == GameEvent.RESONATE_13
                || holder == GameEvent.RESONATE_14
                || holder == GameEvent.RESONATE_15;
    }

}
