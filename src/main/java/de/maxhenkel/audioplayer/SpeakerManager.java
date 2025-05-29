package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.nodes.AudioNode;
import de.maxhenkel.audioplayer.nodes.RepeaterNode;
import de.maxhenkel.audioplayer.nodes.SourceNode;
import de.maxhenkel.audioplayer.nodes.SpeakerNode;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class SpeakerManager {

    private static final SpeakerManager INSTANCE = new SpeakerManager();
    private final ConcurrentHashMap<ServerPosition, SpeakerNode> speakers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ServerPosition, RepeaterNode> repeaters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ServerPosition, SourceNode> sources = new ConcurrentHashMap<>();

    public static SpeakerManager instance() {
        return INSTANCE;
    }

    private void addNode(AudioNode node) {
        if (node instanceof SpeakerNode speaker) {
            speakers.put(node.position, speaker);
        } else if (node instanceof RepeaterNode repeater) {
            repeaters.put(repeater.position, repeater);
        } else if (node instanceof SourceNode source) {
            sources.put(source.position, source);
        }
    }

    public void removeNode(AudioNode node) {
        if (node instanceof SpeakerNode) {
            speakers.remove(node.position);
        } else if (node instanceof RepeaterNode) {
            repeaters.remove(node.position);
        } else if (node instanceof SourceNode) {
            sources.remove(node.position);
        }
    }

    @Nullable
    public AudioNode getNode(@Nullable ServerPosition position) {
        return getNode(position, false);
    }

    @Nullable
    public AudioNode getNode(@Nullable ServerPosition position, boolean createIfNotExists) {
        if (speakers.containsKey(position)) return speakers.get(position);
        if (repeaters.containsKey(position)) return repeaters.get(position);
        if (sources.containsKey(position)) return sources.get(position);
        if (createIfNotExists) return createNode(position);
        return null;
    }

    @Nullable
    private AudioNode createNode(@Nullable ServerPosition position) {
        if (speakers.containsKey(position) || repeaters.containsKey(position) || sources.containsKey(position)) return null;
        AudioNode node = null;
        if (position == null) return null;
        // ignore auto-closable, it will shut down the server
        ServerLevel level = position.fabricLevel();
        if (level == null) return null;
        BlockPos pos = position.fabricBlockPos();
        if (level.getBlockState(pos).is(Blocks.NOTE_BLOCK)) {
            if (level.getBlockState(pos.above()).is(Blocks.SCULK_SENSOR)) {
                node = new SpeakerNode(position, false);
            } else if (level.getBlockState(pos.above()).is(Blocks.CALIBRATED_SCULK_SENSOR)) {
                node = new SpeakerNode(position, true);
            }
        } else if (level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)
                && (level.getBlockState(pos.above()).is(Blocks.SCULK_SENSOR)
                || level.getBlockState(pos.above()).is(Blocks.CALIBRATED_SCULK_SENSOR))
        ) {
            node = new RepeaterNode(position);
        } else if (level.getBlockState(pos).is(Blocks.JUKEBOX)
                && (level.getBlockState(pos.above()).is(Blocks.SCULK_SHRIEKER))
        ) {
            node = new SourceNode(position);
        }
        if (node != null) {
            addNode(node);
            node.scan();
            return node;
        }
        return null;
    }

}