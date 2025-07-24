package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public class MultiLocationalAudioChannel implements LocationalAudioChannel {

    private String category;
    private float distance;
    private final UUID channelId;

    public final Set<LocationalAudioChannel> channels = new HashSet<>();

    public MultiLocationalAudioChannel(String category, float distance, UUID channelId) {
        this.category = category;
        this.distance = distance;
        this.channelId = channelId;
    }

    public void addChannel(LocationalAudioChannel channel) {
        channels.add(channel);
    }

    public boolean addChannelIfNotEmpty(LocationalAudioChannel channel) {
        if (channels.isEmpty()) return false;
        channels.add(channel);
        return true;
    }

    public void removeChannel(LocationalAudioChannel channel) {
        channels.remove(channel);
    }

    public boolean hasChannel(LocationalAudioChannel channel) {
        return channels.contains(channel);
    }

    @Override
    public void updateLocation(Position position) {
        // do nothing
    }

    @Override
    public Position getLocation() {
        var first = channels.stream().findFirst();
        return first.map(LocationalAudioChannel::getLocation).orElse(null);
    }

    @Override
    public float getDistance() {
        return distance;
    }

    @Override
    public void setDistance(float distance) {
        this.distance = distance;
        channels.forEach(channel -> channel.setDistance(distance));
    }

    @Override
    public void send(byte[] opusData) {
        channels.parallelStream().forEach(channel -> channel.send(opusData));
    }

    @Override
    public void send(MicrophonePacket packet) {
        channels.parallelStream().forEach(channel -> channel.send(packet));
    }

    @Override
    public void setFilter(Predicate<ServerPlayer> filter) {
        channels.forEach(channel -> channel.setFilter(filter));
    }

    @Override
    public void flush() {
        channels.forEach(AudioChannel::flush);
        channels.clear();
    }

    @Override
    public boolean isClosed() {
        return channels.stream().allMatch(AudioChannel::isClosed);
    }

    @Override
    public UUID getId() {
        return channelId;
    }

    @Override
    public @Nullable String getCategory() {
        return category;
    }

    @Override
    public void setCategory(@Nullable String category) {
        this.category = category;
        channels.forEach(channel -> channel.setCategory(category));
    }
}
