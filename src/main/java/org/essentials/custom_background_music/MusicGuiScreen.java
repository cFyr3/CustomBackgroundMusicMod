package org.essentials.custom_background_music;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

import java.io.File;

public class MusicGuiScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final AudioManager audioManager = AudioManager.getInstance();
    private final PlaylistManager playlistManager = PlaylistManager.getInstance();

    private Button playButton;
    private Button pauseButton;
    private Button stopButton;

    // New buttons needing updates
    private Button loopButton;
    private Button shuffleButton;

    public MusicGuiScreen() {
        super(Component.literal("Custom Music Player"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new MusicGuiScreen());
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int yPos = 30;

        // --- ROW 1: Playlist Selection ---
        this.addRenderableWidget(Button.builder(Component.literal("Playlist: " + playlistManager.getCurrentPlaylistName()), b -> {
            playlistManager.cyclePlaylist();
            // Re-init to update button text completely or just update message
            b.setMessage(Component.literal("Playlist: " + playlistManager.getCurrentPlaylistName()));
            updateButtonStates();
        }).bounds(centerX - 105, yPos, 130, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Ref"), b -> {
            playlistManager.refreshPlaylists();
            MusicGuiScreen.open(); // Re-open to refresh all states
        }).bounds(centerX + 30, yPos, 35, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("File..."), b -> openFileChooser())
                .bounds(centerX + 70, yPos, 35, 20).build());

        // --- ROW 2: Playlist Settings (Loop / Shuffle) ---
        yPos += 25;

        loopButton = this.addRenderableWidget(Button.builder(Component.literal(playlistManager.getLoopModeString()), b -> {
            playlistManager.cycleLoopMode();
            b.setMessage(Component.literal(playlistManager.getLoopModeString()));
        }).bounds(centerX - 105, yPos, 100, 20).build());

        shuffleButton = this.addRenderableWidget(Button.builder(Component.literal("Shuffle: " + (playlistManager.isShuffle() ? "On" : "Off")), b -> {
            playlistManager.toggleShuffle();
            b.setMessage(Component.literal("Shuffle: " + (playlistManager.isShuffle() ? "On" : "Off")));
        }).bounds(centerX + 5, yPos, 100, 20).build());

        // --- ROW 3: Transport Controls (<< Play Resume Stop >>) ---
        yPos += 30;

        // Previous (<<)
        this.addRenderableWidget(Button.builder(Component.literal("<<"), b -> playlistManager.previous())
                .bounds(centerX - 105, yPos, 30, 20).build());

        // Play (Shortened to 48)
        playButton = this.addRenderableWidget(Button.builder(Component.literal("Play"), b -> {
            if (playlistManager.hasPlaylistSelected() && !audioManager.hasLoadedMusic()) {
                playlistManager.startPlaylist();
            } else {
                audioManager.play();
            }
        }).bounds(centerX - 71, yPos, 48, 20).build());

        // Resume/Pause (Stays 50 to accommodate "Resume" text)
        pauseButton = this.addRenderableWidget(Button.builder(Component.literal("Pause"), b -> audioManager.togglePause())
                .bounds(centerX - 21, yPos, 50, 20).build());

        // Stop (Stays 40)
        stopButton = this.addRenderableWidget(Button.builder(Component.literal("Stop"), b -> audioManager.stop())
                .bounds(centerX + 31, yPos, 40, 20).build());

        // Next (>>)
        this.addRenderableWidget(Button.builder(Component.literal(">>"), b -> playlistManager.next())
                .bounds(centerX + 75, yPos, 30, 20).build());

        // --- ROW 4: Volume ---
        yPos += 25;
        this.addRenderableWidget(Button.builder(Component.literal("Vol -"), b -> audioManager.setVolume(audioManager.getVolume() - 0.1f))
                .bounds(centerX - 105, yPos, 105, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Vol +"), b -> audioManager.setVolume(audioManager.getVolume() + 0.1f))
                .bounds(centerX + 5, yPos, 100, 20).build());

        // --- ROW 5: Reset Everything ---
        // Row should stay as only one button to emphasize it
        yPos += 30;
        this.addRenderableWidget(Button.builder(Component.literal("Reset to Default"), b -> {
            audioManager.cleanup(); // Stops music and clears track
            if (audioManager.getVolume() == 1.0f) { // If volume is equal to 100%
                return;
            } else if (audioManager.getVolume() < 1.0f) { // If volume is less than 100%
                audioManager.setVolume(audioManager.getVolume() + (1.0f - audioManager.getVolume())); //Do X + (1.0f - X) = 1.0f
            } else if (audioManager.getVolume() > 1.0f) { // If volume is greater than 100%
                audioManager.setVolume(audioManager.getVolume() - (audioManager.getVolume() - 1.0f)); //Do X - (X - 1.0f) = 1.0f
            }
            // Reset PlaylistManager state
            while (playlistManager.hasPlaylistSelected()) {
                playlistManager.cyclePlaylist(); // Cycle until "None"
            }
            if (playlistManager.isShuffle()) {
                playlistManager.toggleShuffle();
            }
            // Reset loop to NO_LOOP (cycles until ordinal 0)
            while (!playlistManager.getLoopModeString().equals("Loop: Off")) {
                playlistManager.cycleLoopMode();
            }
            // Refresh screen to update all button labels
            MusicGuiScreen.open();
        }).bounds(centerX - 105, yPos, 210, 20).build());

        // --- ROW 6: Close ---
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(centerX - 105, this.height - 30, 210, 20).build());

        updateButtonStates();
    }

    public void openFileChooser() {
        Thread thread = new Thread(() -> {
            try {
                String filePath = TinyFileDialogs.tinyfd_openFileDialog(
                        "Select MP3", "", null, "MP3 Files (*.mp3)", false);
                if (filePath != null) {
                    File file = new File(filePath);
                    Minecraft.getInstance().execute(() -> {
                        if (audioManager.loadMusicFile(file)) {
                            LOGGER.info("Selected: {}", file.getAbsolutePath());
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("File chooser error", e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void updateButtonStates() {
        boolean hasMusic = audioManager.hasLoadedMusic();
        boolean hasPlaylist = playlistManager.hasPlaylistSelected();

        // Settings are only active if a playlist is selected
        if (loopButton != null) loopButton.active = hasPlaylist;
        if (shuffleButton != null) shuffleButton.active = hasPlaylist;

        // Transport
        if (playButton != null) playButton.active = (hasMusic || hasPlaylist) && !audioManager.isPlaying();
        if (stopButton != null) stopButton.active = hasMusic;

        if (pauseButton != null) {
            pauseButton.active = hasMusic;
            pauseButton.setMessage(Component.literal(audioManager.isPlaying() ? "Pause" : "Resume"));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 10, 0xFFFFFF);

        int statusY = this.height - 55;
        String fileName = audioManager.hasLoadedMusic() ? "§a" + audioManager.getCurrentFileName() : "§7None";
        guiGraphics.drawCenteredString(this.font, fileName, centerX, statusY, 0xFFFFFF);

        String status;
        if (audioManager.isPlaying()) status = "§6Playing";
        else if (audioManager.isPaused()) status = "§ePaused";
        else status = "§cStopped";

        guiGraphics.drawString(this.font, status, centerX - 100, statusY + 12, 0xFFFFFF);
        guiGraphics.drawString(this.font, String.format("Vol: %.0f%%", audioManager.getVolume() * 100), centerX + 60, statusY + 12, 0xFFFFFF);
    }

    @Override
    public void tick() {
        updateButtonStates();
    }
}