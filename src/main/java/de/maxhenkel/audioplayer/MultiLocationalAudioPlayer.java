package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.nodes.SpeakerNode;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MultiLocationalAudioPlayer implements AudioPlayer {

    // static values
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;
    // input values
    private final short[] audioData;
    // maps of channel data
    private final ConcurrentHashMap<ServerPosition, SpeakerNode> speakers = new ConcurrentHashMap<>();
    // synced audio
    private UUID controlPlayerID;
    private int framePosition;
    private Runnable onStopped;

    public MultiLocationalAudioPlayer(List<SpeakerNode> speakers, short[] audioData) {
        this.audioData = audioData;
        speakers.forEach(speaker -> this.speakers.put(speaker.position, speaker));
    }

    @Override
    public void startPlaying() {
        speakers.values().forEach(speakerNode -> {
            // remove itself when stopped
            speakerNode.setOnStopped(() -> {
                speakers.remove(speakerNode.position);
                setRandomController();
            });
            speakerNode.setAudioSupplier(
                    new AudioSupplier(audioData, speakerNode.uuid)
            );
            // start playing on this channel
            speakerNode.startPlaying();
            controlPlayerID = speakerNode.uuid;
        });
        setRandomController();
    }

    private void setRandomController() {
        if (speakers.isEmpty()) {
            controlPlayerID = null;
        } else controlPlayerID = speakers.values().stream().findFirst().get().uuid;
    }

    @Override
    public void stopPlaying() {
        speakers.values().forEach(AudioPlayer::stopPlaying);
        if (onStopped != null) {
            onStopped.run();
        }
    }

    @Override
    public boolean isStarted() {
        return speakers.values().stream().anyMatch(AudioPlayer::isStarted);
    }

    @Override
    public boolean isPlaying() {
        return speakers.values().stream().anyMatch(AudioPlayer::isPlaying);
    }

    @Override
    public boolean isStopped() {
        return speakers.values().stream().allMatch(AudioPlayer::isStopped);
    }

    @Override
    public void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    private class AudioSupplier implements Supplier<short[]> {

        private final UUID speakerChannelID;
        private final short[] audioData;
        private final short[] audioFrame = new short[FRAME_SIZE];

        public AudioSupplier(short[] audioData, UUID speakerChannelID) {
            this.speakerChannelID = speakerChannelID;
            this.audioData = Arrays.copyOf(audioData, audioData.length);
        }

        @Override
        public short[] get() {
            if (controlPlayerID == speakerChannelID) framePosition += audioFrame.length;
            if (framePosition >= audioData.length) return null;
            Arrays.fill(audioFrame, (short) 0);
            System.arraycopy(audioData, framePosition, audioFrame, 0, Math.min(audioFrame.length, audioData.length - framePosition));
            return audioFrame;
        }
    }

}
