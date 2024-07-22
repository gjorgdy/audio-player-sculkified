package de.maxhenkel.audioplayer;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record ServerPosition(
    ServerLevel level,
    Position position
) {

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
        Vec3 centerPos = blockPos.getBottomCenter();
        return new ServerPosition(
            api.fromServerLevel(serverLevel),
            api.createPosition(centerPos.x, centerPos.y, centerPos.z)
        );
    }

}
