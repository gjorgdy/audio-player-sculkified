package de.maxhenkel.audioplayer.listener;

import de.maxhenkel.audioplayer.audioloader.importer.FilebinImporter;
import de.maxhenkel.audioplayer.lang.Lang;
import de.maxhenkel.audioplayer.permission.AudioPlayerPermissionManager;
import de.maxhenkel.audioplayer.webserver.WebServer;
import de.maxhenkel.audioplayer.webserver.WebServerEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class UseItemCallbackListener implements UseItemCallback {

    @Override
    public InteractionResult interact(Player player, Level world, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            boolean hasPermission = AudioPlayerPermissionManager.INSTANCE.hasPermission(serverPlayer, AudioPlayerPermissionManager.UPLOAD_PERMISSION_STRING);
            var item = player.getItemInHand(hand);
            if (!isMusicDisc(world, item) || !hasPermission) return InteractionResult.PASS;
            if (!serverPlayer.getCooldowns().isOnCooldown(item) && sendUploadMessage(serverPlayer)) {
                serverPlayer.swing(hand, true);
                serverPlayer.getCooldowns().addCooldown(item, 60);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Unique
    private boolean sendUploadMessage(ServerPlayer player) {
        // try sending a web message
        Component webMessage = createWebMessage(player);
        if (webMessage != null) {
            player.sendSystemMessage(webMessage, false);
            return true;
        }
        // try sending a file bin message
        Component fileBinMessage = createFileBinMessage(player);
        if (fileBinMessage != null) {
            player.sendSystemMessage(fileBinMessage, false);
            return true;
        }
        return false;
    }

    public Component createFileBinMessage(ServerPlayer player) {
        UUID uuid = UUID.randomUUID();
        URI uploadURL = FilebinImporter.getBin(uuid);

        return Lang.translatable("audioplayer.upload_filebin_instructions",
                Lang.translatable("audioplayer.this_link")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.OpenUrl(uploadURL))
                                    .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_open")));
                        })
                        .withStyle(ChatFormatting.GREEN),
                Component.literal("mp3").withStyle(ChatFormatting.GRAY),
                Component.literal("wav").withStyle(ChatFormatting.GRAY),
                Lang.translatable("audioplayer.here")
                        .withStyle(style -> {
                            return style
                                    .withClickEvent(new ClickEvent.RunCommand("/audioplayer filebin " + uuid))
                                    .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_confirm_upload")));
                        })
                        .withStyle(ChatFormatting.GREEN)
        );
    }

    @Unique
    @Nullable
    public Component createWebMessage(ServerPlayer player) {
        WebServer webServer = WebServerEvents.getWebServer();
        if (webServer == null) {
            return null;
        }

        UUID token = webServer.getTokenManager().generateToken(player.getUUID());

        URI uploadUrl = WebServer.generateUploadUrl(token);

        if (uploadUrl != null) {
            return Lang.translatable("audioplayer.click_upload",
                    Lang.translatable("audioplayer.here").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE).withStyle(
                            style -> style
                                    .withClickEvent(new ClickEvent.OpenUrl(uploadUrl))
                                    .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_open"))))
            );
        }

        return Lang.translatable("audioplayer.visit_website",
                Lang.translatable("audioplayer.this_token").withStyle(ChatFormatting.GREEN).withStyle(
                        style -> style
                                .withClickEvent(new ClickEvent.CopyToClipboard(token.toString()))
                                .withHoverEvent(new HoverEvent.ShowText(Lang.translatable("audioplayer.click_copy"))))
        );
    }

    @Unique
    private boolean isMusicDisc(LevelAccessor levelAccessor, ItemStack stack) {
        Optional<Holder<JukeboxSong>> optional = JukeboxSong.fromStack(levelAccessor.registryAccess(), stack);
        return optional.isPresent();
    }


}
