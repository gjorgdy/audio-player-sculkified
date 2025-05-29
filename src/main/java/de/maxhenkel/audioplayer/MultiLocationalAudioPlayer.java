package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.nodes.SpeakerNode;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class MultiLocationalAudioPlayer implements AudioPlayer {

    // static values
    public static final int SAMPLE_RATE = 48000;
    public static final long FRAME_SIZE_NS = 20_000_000L;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;
    // input values
    private final short[] audioData;
    // maps of channel data
    private final ConcurrentHashMap<ServerPosition, SpeakerNode> speakers = new ConcurrentHashMap<>();
    // synced audio
    private int framePosition = 0;
    private Runnable onStopped;
    private final ExecutorService playbackExecutor = Executors.newFixedThreadPool(1);
    private boolean killSupplier = false;

    public MultiLocationalAudioPlayer(List<SpeakerNode> speakers, short[] audioData) {
        this.audioData = audioData;
        speakers.forEach(speaker -> this.speakers.put(speaker.position, speaker));
    }

    @Override
    public void startPlaying() {
        speakers.values().forEach(speakerNode -> {
            // remove itself when stopped
            speakerNode.setOnStopped(() -> speakers.remove(speakerNode.position));
            speakerNode.setAudioSupplier(new AudioSupplier());
            // start playing on this channel
            speakerNode.startPlaying();
        });
        playbackExecutor.submit(() -> {
            long next = System.nanoTime() + FRAME_SIZE_NS;
            while (framePosition < audioData.length && !isStopped()) {
                framePosition += FRAME_SIZE;
                next += FRAME_SIZE_NS;
                while (System.nanoTime() < next) {
                    Thread.onSpinWait();
                }
            }
        });
    }

    @Override
    public void stopPlaying() {
        killSupplier = true;
        speakers.values().forEach(AudioPlayer::stopPlaying);
        playbackExecutor.shutdownNow();
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

        private final short[] audioFrame = new short[FRAME_SIZE];
        private int localFramePosition = 0;

        @Override
        public short[] get() {
            localFramePosition += FRAME_SIZE;
            if ((localFramePosition + FRAME_SIZE) <= framePosition) {
                localFramePosition = framePosition;
            }
            if (localFramePosition >= audioData.length || killSupplier) return null;
            Arrays.fill(audioFrame, (short) 0);
            System.arraycopy(audioData, localFramePosition, audioFrame, 0, Math.min(audioFrame.length, audioData.length - localFramePosition));
            return audioFrame;
        }
    }

}
