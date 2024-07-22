package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.interfaces.ChannelHolder;
import de.maxhenkel.audioplayer.interfaces.CustomSoundHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(NoteBlock.class)
public class NoteBlockMixin extends Block {

    public NoteBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "triggerEvent", at = @At(value = "HEAD"), cancellable = true)
    public void triggerEvent(BlockState blockState, Level level, BlockPos blockPos, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(blockPos.above());
        if (!(blockEntity instanceof CustomSoundHolder soundHolder)) {
            return;
        }
        if (!(blockEntity instanceof ChannelHolder channelHolder)) {
            return;
        }
        CustomSound customSound = soundHolder.audioplayer$getCustomSound();
        if (customSound == null) {
            return;
        }
        UUID channelId = channelHolder.audioplayer$getChannelID();
        if (channelId != null && PlayerManager.instance().isPlaying(channelId)) {
            PlayerManager.instance().stop(channelId);
            channelHolder.audioplayer$setChannelID(null);
        }

        UUID channel = AudioManager.play(serverLevel, blockPos, PlayerType.NOTE_BLOCK, customSound, null);

        if (channel != null) {
            channelHolder.audioplayer$setChannelID(channel);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "neighborChanged", at = @At("HEAD"))
    public void onNeighbourChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl, CallbackInfo ci) {
        // if block above is a sculk sensor, and speaker is not active
        if ((level.getBlockState(blockPos.above()).is(Blocks.SCULK_SENSOR)
                || level.getBlockState(blockPos.above()).is(Blocks.CALIBRATED_SCULK_SENSOR))
                && !SpeakerManager.instance().isSpeakerActive((ServerLevel) level, blockPos)
        ) {
            SpeakerConnector connector = SpeakerManager.receive((ServerLevel) level, blockPos);
            BlockPos jukeboxPosition = connector.getJukeboxPosition();
            if (jukeboxPosition == null) return;
            SpeakerManager.instance().connectSpeaker((ServerLevel) level, jukeboxPosition, blockPos);
        }
        // if block above is not a sculk sensor, but speaker is active
        else if (!level.getBlockState(blockPos.above()).is(Blocks.SCULK_SENSOR)
                && !level.getBlockState(blockPos.above()).is(Blocks.CALIBRATED_SCULK_SENSOR)
                && SpeakerManager.instance().isSpeakerActive((ServerLevel) level, blockPos)
        ) {
            SpeakerManager.instance().disconnectSpeaker((ServerLevel) level, blockPos);
        }
    }

    @Override
    public void destroy(@NotNull LevelAccessor levelAccessor, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        if (SpeakerManager.instance().isSpeakerActive((ServerLevel) levelAccessor, blockPos))
            SpeakerManager.instance().disconnectSpeaker((ServerLevel) levelAccessor, blockPos);
        BlockEntity blockEntity = levelAccessor.getBlockEntity(blockPos.above());
        if (blockEntity instanceof ChannelHolder channelHolder) {
            UUID channelID = channelHolder.audioplayer$getChannelID();
            if (channelID != null) {
                PlayerManager.instance().stop(channelID);
            }
            channelHolder.audioplayer$setChannelID(null);
        }
        super.destroy(levelAccessor, blockPos, blockState);
    }
}
