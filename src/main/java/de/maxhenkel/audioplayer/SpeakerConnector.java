package de.maxhenkel.audioplayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.BlockPositionSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SpeakerConnector {

    private static final int MAX_RANGE = 128;
    private static final int TRANSMIT_RADIUS = 16;

    private final List<BlockPos> sculkSensorPositions = new ArrayList<>();
    private final List<BlockPos> noteBlockPositions = new ArrayList<>();
    private final ServerLevel serverLevel;
    private BlockPos jukeboxPosition;

    public SpeakerConnector(ServerLevel level) {
        this.serverLevel = level;
    }
    public SpeakerConnector receive(BlockPos speakerLocation) {
        return receiveInternal(speakerLocation, TRANSMIT_RADIUS);
    }

    public SpeakerConnector receiveInternal(BlockPos speakerLocation, int radius) {
        this.noteBlockPositions.add(speakerLocation);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos blockPos = speakerLocation.offset(x, y, z);
                    if (noteBlockPositions.contains(blockPos) || sculkSensorPositions.contains(blockPos)) continue;
                    BlockState blockState = serverLevel.getBlockState(blockPos);
                    BlockState blockStateAbove = serverLevel.getBlockState(blockPos.above());
                    if (blockState.is(Blocks.NOTE_BLOCK) && blockStateAbove.is(Blocks.CALIBRATED_SCULK_SENSOR)
                            || blockState.is(Blocks.AMETHYST_BLOCK) && (blockStateAbove.is(Blocks.SCULK_SENSOR) || blockStateAbove.is(Blocks.CALIBRATED_SCULK_SENSOR))
                    ) {
                        triggerSensor(blockPos, speakerLocation.above());
                        sculkSensorPositions.add(blockPos);
                        receiveInternal(blockPos, TRANSMIT_RADIUS);
                    }
                    if (blockState.is(Blocks.JUKEBOX) && blockStateAbove.is(Blocks.SCULK_SHRIEKER)) {
                        triggerSensor(blockPos, speakerLocation.above());
                        jukeboxPosition = blockPos;
                        return this;
                    }
                }
            }
        }
        return this;
    }

    public SpeakerConnector transmit(BlockPos jukeboxPosition) {
        this.jukeboxPosition = jukeboxPosition;
        transmitInternal(jukeboxPosition);
        return this;
    }

    private void transmitInternal(BlockPos transmitSourcePosition) {
        if (jukeboxPosition == null) return;
        // store transmit source to prevent infinite recursion
        sculkSensorPositions.add(transmitSourcePosition);
        // check for blocks
        for (int x = -SpeakerConnector.TRANSMIT_RADIUS; x <= SpeakerConnector.TRANSMIT_RADIUS; x++) {
            for (int y = -SpeakerConnector.TRANSMIT_RADIUS; y <= SpeakerConnector.TRANSMIT_RADIUS; y++) {
                for (int z = -SpeakerConnector.TRANSMIT_RADIUS; z <= SpeakerConnector.TRANSMIT_RADIUS; z++) {
                    BlockPos blockPos = transmitSourcePosition.offset(x, y, z);
                    if (!isWithinMaxRange(blockPos, jukeboxPosition)) continue;
                    if (noteBlockPositions.contains(blockPos) || sculkSensorPositions.contains(blockPos)) continue;
                    BlockState blockState = serverLevel.getBlockState(blockPos);
                    BlockState blockStateAbove = serverLevel.getBlockState(blockPos.above());
                    if (blockStateAbove.is(Blocks.SCULK_SENSOR) || blockStateAbove.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
                        if (blockState.is(Blocks.NOTE_BLOCK)) handleSpeaker(transmitSourcePosition, blockPos);
                        if (blockState.is(Blocks.AMETHYST_BLOCK) || blockState.is(Blocks.NOTE_BLOCK) && blockStateAbove.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
                            handleSculkSensor(transmitSourcePosition, blockPos, true);
                        }
                    }
                }
            }
        }
    }

    private void handleSpeaker(BlockPos transmitSourcePosition, BlockPos noteBlockPosition) {
        ServerPosition speakerPosition = ServerPosition.create(serverLevel, noteBlockPosition);
        if (!SpeakerManager.instance().isSpeakerActive(speakerPosition)) {
            triggerSensor(transmitSourcePosition, noteBlockPosition.above());
            noteBlockPositions.add(noteBlockPosition);
        }
    }

    private void handleSculkSensor(BlockPos transmitSourcePosition, BlockPos sculkSensorPosition, boolean transmit) {
        triggerSensor(transmitSourcePosition, sculkSensorPosition.above());
        sculkSensorPositions.add(transmitSourcePosition);
        if (transmit) transmitInternal(sculkSensorPosition);
        else receiveInternal(transmitSourcePosition, TRANSMIT_RADIUS);
    }

    private void triggerSensor(BlockPos sourcePos, BlockPos receivePos) {

        this.serverLevel.sendParticles(new VibrationParticleOption(new BlockPositionSource(receivePos), 20), sourcePos.getX(), sourcePos.getY() + 1, sourcePos.getZ(), 1, 0.0, 0.0, 0.0, 0.0);

        BlockState sensorBlockState = serverLevel.getBlockState(receivePos);
        if (sensorBlockState.is(Blocks.SCULK_SENSOR) || sensorBlockState.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
            serverLevel.setBlock(receivePos, (sensorBlockState.setValue(SculkSensorBlock.PHASE, SculkSensorPhase.ACTIVE)), 3);
            serverLevel.scheduleTick(receivePos, sensorBlockState.getBlock(), 20);
        }
    }

    private boolean isWithinMaxRange(BlockPos receivePosition, BlockPos transmitPosition) {
        return Math.sqrt(receivePosition.distSqr(transmitPosition)) <= MAX_RANGE;
    }

    public List<BlockPos> getNoteBlockPositions() {
        return noteBlockPositions;
    }

    @Nullable
    public BlockPos getJukeboxPosition() {
        return jukeboxPosition;
    }

}
