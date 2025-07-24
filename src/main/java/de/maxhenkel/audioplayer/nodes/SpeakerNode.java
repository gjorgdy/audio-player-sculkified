package de.maxhenkel.audioplayer.nodes;

import de.maxhenkel.audioplayer.*;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.util.UUID;
import java.util.function.Supplier;

public class SpeakerNode extends AudioNode {

    public final UUID uuid;
    public final LocationalAudioChannel channel;
    private Supplier<Boolean> isPlaying;
    private Runnable onStopped;

    public SpeakerNode(ServerPosition position, boolean canTransmit) {
        super(position, true, canTransmit);
        this.uuid = UUID.randomUUID();
        VoicechatServerApi api = Plugin.voicechatServerApi;
        channel = (api != null)
            ? PlayerManager.createLocationalAudioChannel(
                uuid, api, position.fabricLevel(), position.vec3(), PlayerType.MUSIC_DISC.getCategory(), PlayerType.MUSIC_DISC.getDefaultRange().get()
            )
            : null;
    }

    public synchronized void checkSourceConnection() {
        if (getInitialSource() == null) {
            sourcePos = null;
            stopPlaying();
        }
    }

    @Override
    protected boolean tryToConnect(AudioNode transmitNode, AudioNode receiveNode) {
        boolean connected = super.tryToConnect(transmitNode, receiveNode);
        if (connected) {
            SourceNode sourceNode = getInitialSource();
            // if the sourceNode is playing, connect the channel
            if (sourceNode != null && sourceNode.channel.addChannelIfNotEmpty(this.channel)) {
                this.setIsPlaying(() -> sourceNode.channel.hasChannel(this.channel));
                this.setOnStopped(() -> sourceNode.channel.removeChannel(this.channel));
            }
        }
        return connected;
    }

    @Override
    public synchronized void disconnect() {
        // stop playing if playing anything
        stopPlaying();
        // disconnect from neighbouring nodes
        super.disconnect();
    }

    public synchronized void stopPlaying() {
        if (onStopped == null) return;
        onStopped.run();
    }

    public synchronized boolean isPlaying() {
        return isPlaying != null && isPlaying.get();
    }

    public synchronized void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    public synchronized void setIsPlaying(Supplier<Boolean> isPlaying) {
        this.isPlaying = isPlaying;
    }

}
