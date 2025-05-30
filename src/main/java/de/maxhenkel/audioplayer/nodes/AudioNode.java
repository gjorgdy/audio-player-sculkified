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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AudioNode {

    // statics
    protected static final int TRANSMIT_RADIUS = 16;
    protected static final int MAX_DEPTH = 8;
    protected static final int MAX_SPEAKERS = 32;
    // settings
    public final ServerPosition position;
    // state
    private final boolean canReceive;
    private final boolean canTransmit;
    public ServerPosition sourcePos;

    public AudioNode(ServerPosition position, boolean canReceive, boolean canTransmit) {
        this.position = position;
        this.canReceive = canReceive;
        this.canTransmit = canTransmit;
    }

    public boolean canReceive() {
        return canReceive;
    }

    @Nullable
    public AudioNode getSource() {
        return sourcePos != null
            ? SpeakerManager.instance().getNode(sourcePos)
            : null;
    }

    @Nullable
    public SourceNode getInitialSource() {
        return getInitialSource(MAX_DEPTH);
    }

    @Nullable
    protected SourceNode getInitialSource(int depth) {
        var _source = getSource();
        if (depth == 0 || _source == null) return null;
        return _source instanceof SourceNode sourceNode
                ? sourceNode
                : _source.getInitialSource(depth - 1);
    }

    public boolean canTransmit() {
        return canTransmit;
    }

    public Stream<AudioNode> getReceivers() {
        return SpeakerManager.instance().getReceivingNodes(this);
    }

    /**
     * Remove itself from all connected nodes
     */
    public void disconnect() {
        // remove itself from manager
        SpeakerManager.instance().removeNode(this);
        // remove its source
        var _receivingFrom = getSource();
        if (_receivingFrom != null) {
            _receivingFrom.disconnectFrom(this);
            sourcePos = null;
        }
        // remove its receivers
        if (canTransmit) {
            getReceivers().forEach(receiver -> receiver.disconnectFrom(this));
        }
    }

    private void disconnectFrom(@NotNull AudioNode node) {
        if (node.getSource() == this) {
            node.sourcePos = null;
        }
        // try and re-establish a connection
        scanAndConnect();
        // communicate the disconnect to receivers
        propagateDisconnect(node);
    }

    protected void propagateDisconnect(AudioNode caller) {
        if (this == caller) {
            return;
        }
        if (this instanceof SpeakerNode speakerNode) {
            speakerNode.checkSourceConnection();
        }
        if (canTransmit) {
            getReceivers().forEach(
                node -> node.propagateDisconnect(caller)
            );
        }
    }

    private static boolean tryToConnect(AudioNode transmitNode, AudioNode receiveNode) {
        if (transmitNode == receiveNode) return false;
        if (transmitNode.canTransmit() && receiveNode.canReceive()) {
            // if receiveNode already receives from transmitNode
            if (receiveNode.getSource() == transmitNode) {
                return false;
            }
            // if transmitNode is further than current source of receiveNode
            if (receiveNode.distanceTo(transmitNode) > receiveNode.distanceTo(receiveNode.getSource())) {
                return false;
            }
            // check sources of transmitNode
            AudioNode _node = transmitNode;
            while (!(_node instanceof SourceNode)) {
                // if receiveNode is in source chain of transmitNode (no circular sources)
                if (_node == receiveNode) {
                    return false;
                }
                // if transmitNode doesn't have a source (no floating nodes)
                if (_node == null) {
                    transmitNode.sourcePos = null;
                    return false;
                }
                _node = _node.getSource();
            }
            // connect
            connect(transmitNode, receiveNode);
            return true;
        }
        return false;
    }

    private static void connect(AudioNode transmitNode, AudioNode receiveNode) {
        receiveNode.sourcePos = transmitNode.position;
//        transmitNode.receiversPos.add(receiveNode.position);
    }

    public double distanceTo(AudioNode node) {
        if (node == null) return Double.MAX_VALUE;
        Vec3 thisPos = this.position.vec3();
        Vec3 nodePos = node.position.vec3();
        return Math.sqrt(
            Math.pow(thisPos.x - nodePos.x, 2) +
            Math.pow(thisPos.y - nodePos.y, 2) +
            Math.pow(thisPos.z - nodePos.z, 2)
        );
    }

    /**
     * Scan in a radius around the node to check if it connects to others
     */
    public void scanAndConnect() {
        this.position.forRadius(offsetPosition -> {
            if (offsetPosition.equals(position)) return;
            AudioNode node = SpeakerManager.instance().getNode(offsetPosition, true);
            if (node != null) {
                if (tryToConnect(this, node)) {
                    transmitParticles(node);
                }
                if (tryToConnect(node, this)) {
                    receiveParticles(node);
                }
            }
        }, TRANSMIT_RADIUS);
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
        if (depth == 0 || !canTransmit) return;
        // repeat signal
        getReceivers().forEach(node -> {
            node.getSpeakers(speakers, depth - 1);
            transmitParticles(node);
        });
    }

    public void receiveParticles() {
        var _source = getSource();
        receiveParticles(_source);
        if (_source instanceof RepeaterNode) {
            _source.receiveParticles();
        }
    }

    public void transmitParticles(AudioNode receiveNode) {
        triggerSensor(this, receiveNode);
    }

    public void receiveParticles(AudioNode transmitNode) {
        triggerSensor(transmitNode, this);
    }

    private static void triggerSensor(AudioNode transmitNode, AudioNode receiveNode) {
        if (transmitNode == null || receiveNode == null) return;
        // ignore auto-closable, it will shut down the server
        ServerLevel level = transmitNode.position.fabricLevel();
        if (level == null) return;
        BlockPos transmitPos = transmitNode.position.fabricBlockPos();
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
