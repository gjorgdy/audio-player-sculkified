package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public record ServerPosition(
    ServerLevel level,
    Position position
) {

    @Nullable
    public net.minecraft.server.level.ServerLevel fabricLevel() {
        if (level.getServerLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return serverLevel;
        }
        return null;
    }

    @NotNull
    public BlockPos fabricBlockPos() {
        return new BlockPos((int) (position.getX()), (int) position.getY(), (int) (position.getZ()));
    }

    public void forRadius(@NotNull Consumer<ServerPosition> consumer, int radius) {
        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    consumer.accept(offset(x, y, z));
        }}}
    }

    public Vec3 vec3() {
        return new Vec3(position.getX(), position.getY(), position.getZ());
    }

    @Nullable
    public ServerPosition offset(double x, double y, double z) {
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) return null;
        return new ServerPosition(
                level,
                api.createPosition(position.getX() + x, position.getY() + y, position.getZ() + z)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerPosition that = (ServerPosition) o;
        return Objects.equals(level, that.level) && Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, position);
    }

    @Nullable
    public static ServerPosition create(ServerLevel serverLevel, Position position) {
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) return null;
        return new ServerPosition(serverLevel, position);
    }

    @Nullable
    public static ServerPosition create(net.minecraft.server.level.ServerLevel serverLevel, BlockPos blockPos) {
        VoicechatServerApi api = Plugin.voicechatServerApi;
        if (api == null) return null;
        return new ServerPosition(
            api.fromServerLevel(serverLevel),
            api.createPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ())
        );
    }

    @Override
    public String toString() {
        return position.toString() + " in " + level.toString();
    }
}
