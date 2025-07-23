package de.maxhenkel.audioplayer.nodes;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.util.UUID;
import java.util.function.Supplier;

public class SpeakerNode extends AudioNode implements AudioPlayer {

    public final UUID uuid;

    private LocationalAudioChannel currentChannel;
    private AudioPlayer internalAudioPlayer;

    private Supplier<Boolean> isPlaying;
    private Runnable onStopped;

    public SpeakerNode(ServerPosition position, boolean canTransmit) {
        super(position, true, canTransmit);
        this.uuid = UUID.randomUUID();
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api != null) {
            currentChannel = PlayerManager.createLocationalAudioChannel(
                    uuid, api, position.fabricLevel(), position.vec3(), PlayerType.MUSIC_DISC.getCategory(), PlayerType.MUSIC_DISC.getDefaultRange().get()
            );
        }
    }

    public synchronized void setAudioSupplier(Supplier<short[]> audioSupplier) {
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api != null) {
            internalAudioPlayer = api.createAudioPlayer(currentChannel, api.createEncoder(), audioSupplier);
        }
    }

    public synchronized void checkSourceConnection() {
        if (getInitialSource() == null) {
            sourcePos = null;
            stopPlaying();
        }
    }

    @Override
    public synchronized void disconnect() {
        // stop playing if playing anything
        stopPlaying();
        // disconnect from neighbouring nodes
        super.disconnect();
    }

    @Override
    public synchronized void startPlaying() {
        if (internalAudioPlayer != null)
            internalAudioPlayer.startPlaying();
    }

    @Override
    public synchronized void stopPlaying() {
        onStopped.run();
    }

    @Override
    public synchronized boolean isStarted() {
        return isPlaying != null && isPlaying.get();
    }

    @Override
    public synchronized boolean isPlaying() {
        return isPlaying != null && isPlaying.get();
    }

    @Override
    public synchronized boolean isStopped() {
        return internalAudioPlayer == null || internalAudioPlayer.isStopped();
    }

    @Override
    public synchronized void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    public synchronized void setIsPlaying(Supplier<Boolean> isPlaying) {
        this.isPlaying = isPlaying;
    }

}
