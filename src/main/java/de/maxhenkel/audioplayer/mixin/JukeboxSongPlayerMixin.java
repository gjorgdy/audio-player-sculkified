package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.interfaces.CustomJukeboxSongPlayer;
import de.maxhenkel.voicechat.api.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerMixin implements CustomJukeboxSongPlayer {

    @Shadow
    @Nullable
    private Holder<JukeboxSong> song;
    @Shadow
    @Final
    private JukeboxSongPlayer.OnSongChanged onSongChanged;

    @Shadow
    private long ticksSinceSongStarted;

    @Shadow
    @Final
    private BlockPos blockPos;

    @Unique
    @Nullable
    private UUID playerID;
    @Unique
    private boolean hasShrieker = false;
    @Unique
    private final List<UUID> speakerChannelIDs = new ArrayList<>();

    @Override
    public UUID audioplayer$getPlayerUUID() {
        return playerID;
    }

    @Override
    public boolean audioplayer$customPlay(ServerLevel level, ItemStack item) {
        CustomSound customSound = CustomSound.of(item);
        if (customSound == null) {
            return false;
        }
        speakerChannelIDs.clear();
        song = null;
        // check if jukebox has shrieker
        hasShrieker = level.getBlockState(blockPos.above()).is(Blocks.SCULK_SHRIEKER);
        if (!hasShrieker) {
            // start playing on jukebox
            UUID jukeboxChannel = AudioManager.play(level, blockPos, PlayerType.MUSIC_DISC, customSound, null);
            if (jukeboxChannel == null) {
                return false;
            }
            playerID = jukeboxChannel;
        // get note blocks around it
        } else {
            SpeakerConnector connector = SpeakerManager.transmit(level, blockPos);
            if (connector.getNoteBlockPositions().isEmpty()) return true;
            playerID = AudioManager.playMultiple(level, connector.getNoteBlockPositions(), PlayerType.MUSIC_DISC, customSound, null);
        }
        ticksSinceSongStarted = 0L;
        onSongChanged.notifyChange();
        return true;
    }

    @Override
    public boolean audioplayer$customStop() {
        if (playerID == null) {
            return false;
        }
        PlayerManager playerManager = PlayerManager.instance();
        playerManager.stop(playerID);
        speakerChannelIDs.forEach(playerManager::stop);
        playerID = null;
        song = null;
        ticksSinceSongStarted = 0L;
        onSongChanged.notifyChange();
        return true;
    }

    @Inject(method = "isPlaying", at = @At(value = "HEAD"), cancellable = true)
    public void isPlaying(CallbackInfoReturnable<Boolean> cir) {
        if (playerID == null) {
            return;
        }
        cir.setReturnValue(PlayerManager.instance().isPlaying(playerID));
    }

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    public void tick(LevelAccessor levelAccessor, BlockState blockState, CallbackInfo ci) {
        if (playerID == null) {
            return;
        }
        ci.cancel();
        if (!isPlaying()) {
            if (playerID != null) {
                audioplayer$customStop();
            }
            return;
        }

        if (shouldEmitJukeboxPlayingEvent()) {
            if (hasShrieker && levelAccessor instanceof ServerLevel serverLevel) {
                // particles of the shrieker
                for(int ah = 0; ah < 5; ++ah) {
                    serverLevel.sendParticles(
                        new ShriekParticleOption(ah * 5),
                            (double)blockPos.getX() + 0.5,
                            (double)blockPos.getY() + 1.5,
                            (double)blockPos.getZ() + 0.5,
                            1, 0.0, 0.0, 0.0, 0.0
                    );
                }
                // notes on speakers
                Collection<Position> speakerPositions = PlayerManager.instance().getSpeakerPositions(playerID);
                if (speakerPositions != null) {
                    speakerPositions.forEach(pos -> {
                        BlockPos sensorBlockPos = new BlockPos((int) (pos.getX() - 0.5), (int) (pos.getY() + 1), (int) (pos.getZ() - 0.5));
                        spawnMusicParticles(levelAccessor, sensorBlockPos);
                    });
                }
            } else {
                spawnMusicParticles(levelAccessor, blockPos);
            }
        }
        ticksSinceSongStarted++;
    }

    @Override
    public void audioplayer$onSave(ItemStack item, CompoundTag compound, HolderLookup.Provider provider) {
        if (playerID != null && !item.isEmpty()) {
            compound.putUUID("ChannelID", playerID);
        }
    }

    @Override
    public void audioplayer$onLoad(ItemStack item, CompoundTag compound, HolderLookup.Provider provider) {
        if (compound.hasUUID("ChannelID") && !item.isEmpty()) {
            playerID = compound.getUUID("ChannelID");
            song = null;
        } else {
            playerID = null;
        }
    }

    @Shadow
    public abstract boolean isPlaying();

    @Shadow
    protected abstract boolean shouldEmitJukeboxPlayingEvent();

    @Shadow
    private static void spawnMusicParticles(LevelAccessor levelAccessor, BlockPos blockPos) {
    }

}
