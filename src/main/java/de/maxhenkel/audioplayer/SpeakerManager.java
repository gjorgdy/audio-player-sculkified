package de.maxhenkel.audioplayer;

import de.maxhenkel.audioplayer.nodes.AudioNode;
import de.maxhenkel.audioplayer.nodes.RepeaterNode;
import de.maxhenkel.audioplayer.nodes.SourceNode;
import de.maxhenkel.audioplayer.nodes.SpeakerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class SpeakerManager {

    private static @Nullable SpeakerManager INSTANCE = null;
    private final ConcurrentHashMap<ServerPosition, AudioNode> nodes = new ConcurrentHashMap<>();

    public synchronized static SpeakerManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new SpeakerManager();
        }
        return INSTANCE;
    }

    public void removeNode(AudioNode node) {
        nodes.remove(node.position);
    }

    @Nullable
    public AudioNode getNode(@Nullable ServerPosition position, boolean createIfNotExists) {
        AudioNode node = nodes.get(position);
        if (node == null && createIfNotExists) {
            node = createNode(position);
            if (node != null) {
                nodes.put(node.position, node);
                node.scanAndConnect();
            }
        }
        return node;
    }

    @Nullable
    private AudioNode createNode(@Nullable ServerPosition position) {
        if (position == null || nodes.containsKey(position)) return null;
        // ignore auto-closable, it will shut down the server
        ServerLevel level = position.fabricLevel();
        if (level == null) return null;
        BlockPos pos = position.fabricBlockPos();
        BlockPos posAbove = pos.above();
        // checks
        boolean isNoteBlock = level.getBlockState(pos).is(Blocks.NOTE_BLOCK);
        boolean isJukebox = level.getBlockState(pos).is(Blocks.JUKEBOX);
        boolean isAmethystBlock = level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK);
        boolean hasSculkSensor = level.getBlockState(posAbove).is(Blocks.SCULK_SENSOR);
        boolean hasSculkShrieker = level.getBlockState(posAbove).is(Blocks.SCULK_SHRIEKER);
        boolean hasCalibratedSculkSensor = level.getBlockState(posAbove).is(Blocks.CALIBRATED_SCULK_SENSOR);
        // early return
        if (!isNoteBlock && !isJukebox && !isAmethystBlock) return null;
        // create nodes
        if (isNoteBlock && hasSculkSensor) {
            return new SpeakerNode(position, false);
        }
        if (isNoteBlock && hasCalibratedSculkSensor) {
            return new SpeakerNode(position, true);
        }
        if (isAmethystBlock && (hasSculkSensor || hasCalibratedSculkSensor)) {
            return new RepeaterNode(position);
        }
        if (isJukebox && hasSculkShrieker) {
            return new SourceNode(position);
        }
        return null;
    }

    public void stopAll() {
        nodes.values().forEach(AudioNode::disconnect);
    }

    public static void onServerStopped(MinecraftServer ignoredServer) {
        instance().stopAll();
    }

}