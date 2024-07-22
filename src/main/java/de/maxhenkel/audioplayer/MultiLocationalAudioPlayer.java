package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MultiLocationalAudioPlayer implements AudioPlayer {

    // static values
    public static final int SAMPLE_RATE = 48000;
    public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;
    // input values
    private final VoicechatServerApi api;
    private final ServerLevel level;
    private final UUID playerID;
    private final List<LocationalAudioChannel> locationalAudioChannels;
    private final short[] audioData;
    // maps of channel data
    private final ConcurrentHashMap<Position, ChannelPlayer> channelPlayers = new ConcurrentHashMap<>();
    // synced audio
    private UUID controlPlayerID;
    private int framePosition;
    private Runnable onStopped;

    public MultiLocationalAudioPlayer(VoicechatServerApi api, ServerLevel level, UUID playerID, List<LocationalAudioChannel> locationalAudioChannels, short[] audioData) {
        this.api = api;
        this.level = level;
        this.playerID = playerID;
        this.locationalAudioChannels = locationalAudioChannels;
        this.audioData = audioData;
    }

    public Collection<Position> getChannelPositions() {
        return channelPlayers.values().stream().map(ChannelPlayer::position).toList();
    }

    @Override
    public void startPlaying() {
        locationalAudioChannels.forEach(this::addChannel);
        setRandomController();
    }

    public void addChannel(LocationalAudioChannel locationalAudioChannel) {
        // create the player
        ChannelPlayer channelPlayer = createChannelPlayer(locationalAudioChannel);
        channelPlayers.put(channelPlayer.position, channelPlayer);
        SpeakerManager.instance().setSpeakerActive(ServerPosition.create(level, channelPlayer.position), playerID);
        // remove itself when stopped
        channelPlayer.setOnStopped(() -> {
            channelPlayers.remove(channelPlayer.position);
            SpeakerManager.instance().setSpeakerInactive(ServerPosition.create(level, channelPlayer.position));
            if (controlPlayerID == channelPlayer.ID) setRandomController();
        });
        // start playing on this channel
        controlPlayerID = channelPlayer.ID;
        channelPlayer.startPlaying();
    }

    private void setRandomController() {
        if (channelPlayers.isEmpty()) {
            controlPlayerID = null;
        } else controlPlayerID = channelPlayers.values().stream().findFirst().get().ID();
    }

    public void stopPlaying(Position channelPosition) {
        if (channelPlayers.containsKey(channelPosition)) {
            channelPlayers.get(channelPosition).stopPlaying();
        }
    }

    @Override
    public void stopPlaying() {
        channelPlayers.values().forEach(AudioPlayer::stopPlaying);
        if (onStopped != null) {
            onStopped.run();
        }
    }

    @Override
    public boolean isStarted() {
        return channelPlayers.values().stream().anyMatch(AudioPlayer::isStarted);
    }

    @Override
    public boolean isPlaying() {
        return channelPlayers.values().stream().anyMatch(AudioPlayer::isPlaying);
    }

    @Override
    public boolean isStopped() {
        return channelPlayers.values().stream().allMatch(AudioPlayer::isStopped);
    }

    @Override
    public void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    private ChannelPlayer createChannelPlayer(LocationalAudioChannel locationalAudioChannel) {
        return new ChannelPlayer(
                locationalAudioChannel.getId(),
                api.createAudioPlayer(locationalAudioChannel, api.createEncoder(), new AudioSupplier(audioData, locationalAudioChannel.getId())),
                locationalAudioChannel.getLocation()
        );
    }

    private record ChannelPlayer(
            UUID ID,
            AudioPlayer audioPlayer,
            Position position
    ) implements AudioPlayer {

        @Override
        public void startPlaying() {
            audioPlayer.startPlaying();
        }

        @Override
        public void stopPlaying() {
            audioPlayer.stopPlaying();
        }

        @Override
        public boolean isStarted() {
            return audioPlayer.isStarted();
        }

        @Override
        public boolean isPlaying() {
            return audioPlayer.isPlaying();
        }

        @Override
        public boolean isStopped() {
            return audioPlayer.isStopped();
        }

        @Override
        public void setOnStopped(Runnable onStopped) {
            audioPlayer.setOnStopped(onStopped);
        }
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
            if (controlPlayerID == speakerChannelID) {
                framePosition += audioFrame.length;
            }

            if (framePosition >= audioData.length) {
                return null;
            }

            Arrays.fill(audioFrame, (short) 0);
            System.arraycopy(audioData, framePosition, audioFrame, 0, Math.min(audioFrame.length, audioData.length - framePosition));
            return audioFrame;
        }
    }

}
