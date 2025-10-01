package de.maxhenkel.audioplayer.sculkradio;

import de.maxhenkel.audioplayer.voicechat.VoicechatAudioPlayerPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class MultiLocationalAudioChannel implements LocationalAudioChannel {

    private final UUID id;
    private Position sourcePosition;
    private float distance;
    private Predicate<ServerPlayer> filter;
    private String category;
    private final Map<Position, LocationalAudioChannel> audioChannels = new HashMap<>();

    public MultiLocationalAudioChannel(UUID id, Position sourcePosition) {
        this.id = id;
        this.sourcePosition = sourcePosition;
    }

    @Override
    public void updateLocation(Position position) {
        sourcePosition = position;
    }

    public void addChannel(ServerLevel level, Vec3 pos) {
        VoicechatServerApi api = VoicechatAudioPlayerPlugin.voicechatServerApi;
        if (api == null) return;
        var position = api.createPosition(pos.x, pos.y, pos.z);
        var channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(level), position);
        if (channel == null) return;
        channel.setDistance(distance);
        channel.setFilter(filter);
        channel.setCategory(category);
        audioChannels.put(position, channel);
    }

    public void removeChannel(Vec3 pos) {
        VoicechatServerApi api = VoicechatAudioPlayerPlugin.voicechatServerApi;
        if (api == null) return;
        var position = api.createPosition(pos.x, pos.y, pos.z);
        var channel = audioChannels.remove(position);
        channel.flush();
    }

    @Override
    public Position getLocation() {
        return sourcePosition;
    }

    @Override
    public float getDistance() {
        return distance;
    }

    @Override
    public void setDistance(float distance) {
        this.distance = distance;
        audioChannels.values().forEach(channel -> channel.setDistance(distance));
    }

    @Override
    public void send(byte[] opusData) {
        audioChannels.values().forEach(channel -> channel.send(opusData));
    }

    @Override
    public void send(MicrophonePacket packet) {
        audioChannels.values().forEach(channel -> channel.send(packet));
    }

    @Override
    public void setFilter(Predicate<ServerPlayer> filter) {
        this.filter = filter;
        audioChannels.values().forEach(channel -> channel.setFilter(filter));
    }

    @Override
    public void flush() {
        audioChannels.values().forEach(AudioChannel::flush);
    }

    @Override
    public boolean isClosed() {
        return audioChannels.values().stream().allMatch(AudioChannel::isClosed);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public @Nullable String getCategory() {
        return category;
    }

    @Override
    public void setCategory(@Nullable String category) {
        this.category = category;
        audioChannels.values().forEach(channel -> channel.setCategory(category));
    }
}
