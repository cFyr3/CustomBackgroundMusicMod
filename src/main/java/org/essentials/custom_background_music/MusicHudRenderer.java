package org.essentials.custom_background_music;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import static org.essentials.custom_background_music.CustomBackgroundMusic.LOGGER;

public class MusicHudRenderer {
    public static int playIconSize = 12;
    public static int stopIconSize = 12;
    public static int forwardIconSize = 12;
    public static int reverseIconSize = 12;
    private static final ResourceLocation SPRITE_PLAY = ResourceLocation.fromNamespaceAndPath("custom_background_music", "icons/play");
    private static final ResourceLocation SPRITE_PAUSE = ResourceLocation.fromNamespaceAndPath("custom_background_music", "icons/pause");
    private static final ResourceLocation SPRITE_FORWARD = ResourceLocation.fromNamespaceAndPath("custom_background_music", "icons/forward");
    private static final ResourceLocation SPRITE_REVERSE = ResourceLocation.fromNamespaceAndPath("custom_background_music", "icons/reverse");
    private static final ResourceLocation SPRITE_STOP = ResourceLocation.fromNamespaceAndPath("custom_background_music", "icons/stop");

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!ModConfigs.SPEC.isLoaded()) return;
        if (!ModConfigs.SHOW_HUD.get()) return;

        Minecraft mc = Minecraft.getInstance();
        AudioManager audio = AudioManager.getInstance();
        PlaylistManager playlist = PlaylistManager.getInstance();

        GuiGraphics graphics = event.getGuiGraphics();

        int x = ModConfigs.HUD_X.get();
        int y = ModConfigs.HUD_Y.get();
        int iconY = y - 2;
        int width;
        if (audio.hasLoadedMusic() && (audio.isPlaying() || audio.isPaused())) {

            String fullText = (audio.isPaused() ? "Paused: " : "Now Playing: ") + audio.getCurrentFileName().replace(".mp3", "");
            ResourceLocation currentIcon = audio.isPaused() ? SPRITE_PAUSE : SPRITE_PLAY;

            width = mc.font.width(fullText);

            int color;
            try {
                String hex = ModConfigs.HUD_COLOR.get().replace("#", "");
                color = Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                color = 0xFFFFFF;
            }

            // Draw background box
            if (ModConfigs.SHOW_BACKGROUND.get()) {
                if (ModConfigs.SHOW_HUD_ICONS.get()) {
                    graphics.fill(x - 4, y - 4, x + width + 4 + (12 * 3), y + 12, 0x99000000);
                } else {
                    graphics.fill(x - 4, y - 4, x + width + 4, y + 12, 0x99000000);
                }
            }

            // Draw Text
            graphics.drawString(mc.font, fullText, x, y, color, true);

            // Draw Icons
            if (ModConfigs.SHOW_HUD_ICONS.get()) {
                try {
                    graphics.blitSprite(currentIcon, x + width + 16, iconY + 1, playIconSize - 3, playIconSize - 3);
                    graphics.blitSprite(SPRITE_REVERSE, x + width + 4, iconY, reverseIconSize, reverseIconSize);
                    graphics.blitSprite(SPRITE_FORWARD, x + width + 25, iconY, forwardIconSize, forwardIconSize);
                } catch (Exception e) {
                    LOGGER.warn("Error rendering icons", e);
                }
            }
        }
        else if (audio.hasLoadedMusic() || playlist.hasPlaylistSelected()) {
            try {
                int color;
                try {
                    String hex = ModConfigs.HUD_COLOR.get().replace("#", "");
                    color = Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    color = 0xFFFFFF;
                }
                width = mc.font.width("Stopped ");
                if (ModConfigs.SHOW_HUD_ICONS.get()) {
                    graphics.fill(x - 4, y - 4, x + width + 14, y + 12, 0x99000000);
                    graphics.blitSprite(SPRITE_STOP, x + width, iconY + 1, stopIconSize - 2, stopIconSize - 2);
                } else {
                    graphics.fill(x - 4, y - 4, x + width, y + 12, 0x99000000);
                }
                graphics.drawString(mc.font, "Stopped", x, y, color, true);
            } catch (Exception e) {
                LOGGER.warn("Error rendering stop icon", e);
            }
        }
    }
}