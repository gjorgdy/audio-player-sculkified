package de.maxhenkel.audioplayer.nodes;

import de.maxhenkel.audioplayer.ServerPosition;
import de.maxhenkel.audioplayer.SpeakerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.BlockPositionSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AudioNode {

    // statics
    protected static final int TRANSMIT_RADIUS = 16;
    protected static final int MAX_DEPTH = 8;
    protected static final int MAX_SPEAKERS = 32;
    // settings
    public final ServerPosition position;
    // state
    protected final List<AudioNode> receivingFrom;
    protected final List<AudioNode> transmittingTo;

    public AudioNode(ServerPosition position, boolean canReceive, boolean canTransmit) {
        this.position = position;
        receivingFrom = canReceive ? new ArrayList<>() : null;
        transmittingTo = canTransmit ? new ArrayList<>() : null;
    }

    public boolean canReceive() {
        return receivingFrom != null;
    }

    public boolean canTransmit() {
        return transmittingTo != null;
    }

    /**
     * Remove itself from all connected nodes
     */
    public void disconnect() {
        if (receivingFrom != null)
            receivingFrom.forEach(transmittingNode -> transmittingNode.transmittingTo.remove(this));
        if (transmittingTo != null)
            transmittingTo.forEach(receivingNode -> {
                if (receivingNode instanceof SpeakerNode speakerNode) {
                    speakerNode.checkSourceConnection();
                }
                receivingNode.receivingFrom.remove(this);
            });
        SpeakerManager.instance().removeNode(this);
    }

    private boolean transmitTo(AudioNode node) {
        if (this.canTransmit() && node.canReceive()) {
            if (!node.receivingFrom.contains(this)) {
                node.receivingFrom.add(this);
            }
            if (!transmittingTo.contains(node)) {
                transmittingTo.add(node);
                return true;
            }
        }
        return false;
    }

    private boolean receiveFrom(AudioNode node) {
        if (this.canReceive() && node.canTransmit()) {
            if (!node.transmittingTo.contains(this)) {
                node.transmittingTo.add(this);
            }
            if (!receivingFrom.contains(node)) {
                receivingFrom.add(node);
                return true;
            }
        }
        return false;
    }

    public void scan() {
        scan(this, MAX_DEPTH);
    }

    /**
     * Scan in a radius around the node to check if it connects to others
     *
     * @param source the node that triggered this scan
     * @param depth  an integer depth state to prevent recursion and infinite range
     */
    private void scan(AudioNode source, int depth) {
        if (depth == 0) return;
        this.position.forRadius(offsetPosition -> {
            if (offsetPosition.equals(position)) return;
            AudioNode node = SpeakerManager.instance().getNode(offsetPosition, true);
            if (node != null && node != source) {
                if (transmitTo(node)) {
                    triggerSensor(node);
                }
                if (receiveFrom(node)) {
                    node.scan(this, depth - 1);
                }
            }
        }, TRANSMIT_RADIUS);
    }

    public List<AudioNode> findSources() {
        return findSources(MAX_DEPTH);
    }

    protected List<AudioNode> findSources(int depth) {
        if (this instanceof SourceNode) return List.of(this);
        if (receivingFrom == null) return new ArrayList<>();
        List<AudioNode> sources = new ArrayList<>();
        if (depth == 0) return sources;
        for (AudioNode node : receivingFrom) {
            sources.addAll(node.findSources(depth - 1));
        }
        return sources;
    }

    public List<SpeakerNode> getSpeakers() {
        List<SpeakerNode> speakers = new ArrayList<>();
        getSpeakers(speakers, MAX_DEPTH);
        return speakers;
    }

    private void getSpeakers(List<SpeakerNode> speakers, int depth) {
        if (speakers.size() >= MAX_SPEAKERS) return;
        // if self speaker
        if (this instanceof SpeakerNode speakerNode) {
            // If it is already connected to this graph or another, end search
            if (!speakers.contains(speakerNode) && !speakerNode.isPlaying())
                speakers.add(speakerNode);
            else return;
        }
        // if reached max range, or can't transmit
        if (depth == 0 || transmittingTo == null) return;
        // repeat signal
        transmittingTo.forEach(node -> {
            node.getSpeakers(speakers, depth - 1);
            triggerSensor(node);
        });
    }

    public void triggerSensor(AudioNode receiveNode) {
        // ignore auto-closable, it will shut down the server
        ServerLevel level = this.position.fabricLevel();
        if (level == null) return;
        BlockPos transmitPos = this.position.fabricBlockPos();
        BlockPos receivePos = receiveNode.position.fabricBlockPos();
        level.sendParticles(new VibrationParticleOption(
                        new BlockPositionSource(receivePos.above()), 20),
                transmitPos.getX() + 0.5, transmitPos.getY() + 1, transmitPos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);

        BlockState sensorBlockState = level.getBlockState(receivePos);
        if (sensorBlockState.is(Blocks.SCULK_SENSOR) || sensorBlockState.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
            level.setBlock(receivePos, (sensorBlockState.setValue(SculkSensorBlock.PHASE, SculkSensorPhase.ACTIVE)), 3);
            level.scheduleTick(receivePos, sensorBlockState.getBlock(), 20);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AudioNode audioNode = (AudioNode) o;
        return Objects.equals(position, audioNode.position);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(position);
    }
}
