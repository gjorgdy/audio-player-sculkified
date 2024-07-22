package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.interfaces.CustomJukeboxSongPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeakerManager {

    private static final SpeakerManager INSTANCE = new SpeakerManager();
    // ServerPosition : MultiLocationalAudioPlayer.ID
    private final ConcurrentHashMap<ServerPosition, UUID> speakers = new ConcurrentHashMap<>();

    public static SpeakerManager instance() {
        return INSTANCE;
    }

    private static VoicechatServerApi api() {
        return Plugin.voicechatServerApi;
    }

    public static SpeakerConnector transmit(ServerLevel level, BlockPos jukeboxPosition) {
        return new SpeakerConnector(level).transmit(jukeboxPosition);
    }

    public static SpeakerConnector receive(ServerLevel level, BlockPos noteBlockPosition) {
        return new SpeakerConnector(level).receive(noteBlockPosition);
    }

    public void setSpeakerActive(@Nullable ServerPosition serverPosition, UUID playerID) {
        if (serverPosition == null) return;
        speakers.put(serverPosition, playerID);
    }

    public void setSpeakerInactive(@Nullable ServerPosition serverPosition) {
        if (serverPosition == null) return;
        speakers.remove(serverPosition);
    }

    public boolean isSpeakerActive(ServerLevel level, BlockPos noteBlockPosition) {
        return isSpeakerActive(ServerPosition.create(level, noteBlockPosition));
    }

    public boolean isSpeakerActive(@Nullable ServerPosition serverPosition) {
        return speakers.containsKey(serverPosition);
    }

    public void disconnectSpeaker(ServerLevel level, BlockPos noteBlockPosition) {
        ServerPosition sp = ServerPosition.create(level, noteBlockPosition);
        if (sp == null) return;
        PlayerManager.instance().stopSpeakerChannel(
                speakers.get(sp),
                sp.position()
        );
    }

    public void connectSpeaker(ServerLevel level, BlockPos jukeboxPosition, BlockPos noteBlockPosition) {
        if (level.getBlockEntity(jukeboxPosition) instanceof JukeboxBlockEntity jukeboxBlockEntity
                && jukeboxBlockEntity.getSongPlayer() instanceof CustomJukeboxSongPlayer customJukeboxSongPlayer
        ) {
            UUID jukeboxPlayerID = customJukeboxSongPlayer.audioplayer$getPlayerUUID();
            PlayerManager.instance().addSpeakerChannel(
                    jukeboxPlayerID,
                    PlayerManager.createLocationalAudioChannel(
                            UUID.randomUUID(),
                            api(),
                            level,
                            noteBlockPosition.getBottomCenter(),
                            PlayerType.MUSIC_DISC.getCategory(),
                            PlayerType.MUSIC_DISC.getDefaultRange().get()
                    )
            );
        }
    }

}