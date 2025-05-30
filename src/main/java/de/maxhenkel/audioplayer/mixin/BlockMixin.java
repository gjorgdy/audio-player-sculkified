package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.CustomSound;
import de.maxhenkel.audioplayer.SpeakerManager;
import de.maxhenkel.audioplayer.interfaces.CustomSoundHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;", at = @At(value = "RETURN"), cancellable = true)
    private static void getDrops(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> ci) {
        getDropsInternal(blockState, serverLevel, blockPos, blockEntity, ci);
    }

    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At(value = "RETURN"), cancellable = true)
    private static void getDrops(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack itemStack, CallbackInfoReturnable<List<ItemStack>> ci) {
        getDropsInternal(blockState, serverLevel, blockPos, blockEntity, ci);
    }

    @Unique
    private static void getDropsInternal(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> ci) {
        if (!(blockState.getBlock() instanceof SkullBlock)) {
            return;
        }
        if (!(blockEntity instanceof CustomSoundHolder customSoundHolder)) {
            return;
        }
        CustomSound customSound = customSoundHolder.audioplayer$getCustomSound();
        if (customSound == null) {
            return;
        }

        List<ItemStack> result = ci.getReturnValue();

        for (ItemStack stack : result) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }
            if (!(blockItem.getBlock() instanceof SkullBlock)) {
                continue;
            }
            customSound.saveToItem(stack);
        }
    }

    @Inject(method = "destroy", at = @At("RETURN"))
    public void onDestroy(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (blockState.is(Blocks.AMETHYST_BLOCK) || blockState.is(Blocks.NOTE_BLOCK) || blockState.is(Blocks.JUKEBOX)) {
            var _blockStateAbove = levelAccessor.getBlockState(blockPos.above());
            if (_blockStateAbove.is(Blocks.SCULK_SENSOR) || _blockStateAbove.is(Blocks.CALIBRATED_SCULK_SENSOR) || _blockStateAbove.is(Blocks.SCULK_SHRIEKER)) {
                SpeakerManager.instance().disconnectNode(levelAccessor, blockPos);
            }
        }
    }

}
