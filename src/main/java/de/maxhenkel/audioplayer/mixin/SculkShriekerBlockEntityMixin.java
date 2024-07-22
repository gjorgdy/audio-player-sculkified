package de.maxhenkel.audioplayer.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin {

    @Inject(method = "shriek", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/SculkShriekerBlockEntity;getBlockState()Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.AFTER), cancellable = true)
    public void shriek(ServerLevel serverLevel, Entity entity, CallbackInfo ci, @Local BlockPos pos) {
        if (serverLevel.getBlockState(pos.below()).is(Blocks.JUKEBOX)) {
            ci.cancel();
        }
    }

}
