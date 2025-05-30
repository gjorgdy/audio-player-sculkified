package de.maxhenkel.audioplayer.mixin;

import de.maxhenkel.audioplayer.webserver.UrlUtils;
import de.maxhenkel.audioplayer.webserver.WebServer;
import de.maxhenkel.audioplayer.webserver.WebServerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;
import java.util.UUID;

@Mixin(ItemStack.class)
public class MusicDiscMixin {

    @Unique
    private ItemStack stack = (ItemStack) (Object) this;

    @Inject(method = "use", at = @At("RETURN"), cancellable = true)
    public void onUse(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (isMusicDisc(stack)) {

            WebServer webServer = WebServerEvents.getWebServer();
            if (webServer == null) return;
            UUID token = webServer.getTokenManager().generateToken(player.getUUID());
            URI uploadUrl = UrlUtils.generateUploadUrl(token);

            if (uploadUrl != null && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.literal("Click ")
                            .append(Component.literal("here").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE).withStyle(style -> {
                                return style
                                        .withClickEvent(new ClickEvent.OpenUrl(uploadUrl))
                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open")));
                            }))
                            .append(" to upload your sound.")
                        , false);
                stack.setPopTime(20);
                cir.setReturnValue(InteractionResult.SUCCESS);
                player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @Unique
    private boolean isMusicDisc(ItemStack stack) {
        return stack.is(Items.MUSIC_DISC_5)
                || stack.is(Items.MUSIC_DISC_11)
                || stack.is(Items.MUSIC_DISC_13)
                || stack.is(Items.MUSIC_DISC_CAT)
                || stack.is(Items.MUSIC_DISC_BLOCKS)
                || stack.is(Items.MUSIC_DISC_FAR)
                || stack.is(Items.MUSIC_DISC_CHIRP)
                || stack.is(Items.MUSIC_DISC_CREATOR)
                || stack.is(Items.MUSIC_DISC_CREATOR_MUSIC_BOX)
                || stack.is(Items.MUSIC_DISC_PIGSTEP)
                || stack.is(Items.MUSIC_DISC_MALL)
                || stack.is(Items.MUSIC_DISC_MELLOHI)
                || stack.is(Items.MUSIC_DISC_STAL)
                || stack.is(Items.MUSIC_DISC_STRAD)
                || stack.is(Items.MUSIC_DISC_WARD)
                || stack.is(Items.MUSIC_DISC_WAIT)
                || stack.is(Items.MUSIC_DISC_PRECIPICE)
                || stack.is(Items.MUSIC_DISC_OTHERSIDE)
                || stack.is(Items.MUSIC_DISC_RELIC);
    }

}
