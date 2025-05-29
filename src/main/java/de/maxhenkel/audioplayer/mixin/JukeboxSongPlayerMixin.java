package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.audioplayer.interfaces.CustomJukeboxSongPlayer;
import de.maxhenkel.audioplayer.nodes.SourceNode;
import de.maxhenkel.audioplayer.nodes.SpeakerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.UUIDUtil;
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
import java.util.List;
import java.util.UUID;

@Mixin(JukeboxSongPlayer.class)
public abstract class JukeboxSongPlayerMixin implements CustomJukeboxSongPlayer {

    // shadow
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
    // end of shadow

    @Unique
    @Nullable
    private UUID playerID;
    @Unique
    private boolean hasShrieker = false;
    @Unique
    private SourceNode sourceNode;
    @Unique
    private List<SpeakerNode> speakerNodes;

    @Shadow
    private static void spawnMusicParticles(LevelAccessor levelAccessor, BlockPos blockPos) {
    }

    @Override
    public UUID audioplayer$getPlayerUUID() {
        return playerID;
    }

    @Override
    public boolean audioplayer$customPlay(ServerLevel level, ItemStack item) {
        if (sourceNode == null) {
            sourceNode = (SourceNode) SpeakerManager.instance().getNode(
                ServerPosition.create(level, blockPos),
                true
            );
        }
        CustomSound customSound = CustomSound.of(item);
        if (customSound == null) {
            return false;
        }
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
            speakerNodes = sourceNode.getSpeakers();
            speakerNodes.forEach(speakerNode -> speakerNode.setSourceNode(sourceNode));
            playerID = AudioManager.playMultiple(level, speakerNodes, PlayerType.MUSIC_DISC, customSound, null);
            sourceNode.setPlayerID(playerID);
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
        if (sourceNode != null) sourceNode.setPlayerID(null);
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
                for (int ah = 0; ah < 5; ++ah) {
                    serverLevel.sendParticles(
                            new ShriekParticleOption(ah * 5),
                            (double) blockPos.getX() + 0.5,
                            (double) blockPos.getY() + 1.5,
                            (double) blockPos.getZ() + 0.5,
                            1, 0.0, 0.0, 0.0, 0.0
                    );
                }
                // notes on speakers
                speakerNodes.forEach(speakerNode -> {
                    if (speakerNode.isPlaying()) {
                        sourceNode.triggerSensor(speakerNode);
                        spawnMusicParticles(levelAccessor, speakerNode.position.fabricBlockPos().above());
                    }
                });
            } else {
                spawnMusicParticles(levelAccessor, blockPos);
            }
        }
        ticksSinceSongStarted++;
    }

    @Override
    public void audioplayer$onSave(ItemStack item, CompoundTag compound, HolderLookup.Provider provider) {
        if (playerID != null && !item.isEmpty()) {
            compound.store("ChannelID", UUIDUtil.CODEC, playerID);
        }
    }

    @Override
    public void audioplayer$onLoad(ItemStack item, CompoundTag compound, HolderLookup.Provider provider) {
        UUID id = compound.read("ChannelID", UUIDUtil.CODEC).orElse(null);
        if (id != null && !item.isEmpty()) {
            playerID = id;
            song = null;
        } else {
            playerID = null;
        }
    }

    @Shadow
    public abstract boolean isPlaying();

    @Shadow
    protected abstract boolean shouldEmitJukeboxPlayingEvent();

}
