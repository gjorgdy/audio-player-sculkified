package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.ServerPosition;
import de.maxhenkel.audioplayer.SpeakerManager;
import de.maxhenkel.audioplayer.nodes.AudioNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSensorBlock.class)
public class SculkSensorBlockMixin extends Block {

    public SculkSensorBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "onPlace", at = @At("HEAD"))
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl, CallbackInfo ci) {
        BlockPos noteblockPos = blockPos.below();
        if (level.getBlockState(noteblockPos).is(Blocks.NOTE_BLOCK)
                || level.getBlockState(noteblockPos).is(Blocks.AMETHYST_BLOCK)
        ) {
            ServerPosition serverPosition = ServerPosition.create((ServerLevel) level, noteblockPos);
            SpeakerManager.instance().createNode(serverPosition);
        }
    }

    @Override
    public void destroy(LevelAccessor levelAccessor, BlockPos blockPos, @NotNull BlockState blockState) {
        BlockPos noteblockPos = blockPos.below();
        if (levelAccessor.getBlockState(noteblockPos).is(Blocks.NOTE_BLOCK)
                || levelAccessor.getBlockState(noteblockPos).is(Blocks.AMETHYST_BLOCK)
        ) {
            ServerPosition serverPosition = ServerPosition.create((ServerLevel) levelAccessor, noteblockPos);
            AudioNode node = SpeakerManager.instance().getNode(serverPosition, false);
            if (node != null) node.disconnect();
        }
        super.destroy(levelAccessor, blockPos, blockState);
    }

}
