package com.ashkiano.signcarousel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;

// Main class of the SignCarousel plugin
public class SignCarousel extends JavaPlugin {

    // Called when the plugin is enabled
    @Override
    public void onEnable() {
        ConfigurationSection messagesConfig = null;

        // Set language to English
        String language = "en";

        try {
            // Save default configuration if it doesn't exist
            saveDefaultConfig();

            // Initialize Metrics for plugin analytics
            Metrics metrics = new Metrics(this, 18812);

            // Load the main configuration
            FileConfiguration config = getConfig();

            // Retrieve the list of sign configurations from the main configuration
            List<Map<?, ?>> signConfigs = config.getMapList("signs");

            // Load the messages configuration
            messagesConfig = getConfig().getConfigurationSection("messages");

            // Loop over each sign configuration
            for (Map<?, ?> signConfig : signConfigs) {
                // Process each sign configuration inside a try-catch block.
                try {
                    // Retrieve the messages, delay, and typewriting effect settings from the sign configuration
                    List<List<String>> messages = (List<List<String>>) signConfig.get("messages");
                    // Get the delay between messages for this sign.
                    int delay = (int) signConfig.get("delay");
                    // Check if typewriting effect is enabled for this sign.
                    boolean useTypewritingEffect = (boolean) signConfig.get("typewriting_effect");

                    // Retrieve the location of the sign from the sign configuration
                    Map<?, ?> locationData = (Map<?, ?>) signConfig.get("location");
                    String worldName = (String) locationData.get("world");
                    World world = getServer().getWorld(worldName);

                    int x = (int) locationData.get("x");
                    int y = (int) locationData.get("y");
                    int z = (int) locationData.get("z");

                    Location signLocation = new Location(world, x, y, z);

                    // If the block at the location is not a sign, log a warning and continue with the next configuration
                    if (!(signLocation.getBlock().getState() instanceof Sign)) {
                        getLogger().warning(String.format(messagesConfig.getString("not_a_sign." + language), signLocation));
                        continue;
                    }

                    // Get the sign at the location
                    Sign sign = (Sign) signLocation.getBlock().getState();

                    // Create a new SignCarouselTask for the sign and schedule it to run periodically
                    new SignCarouselTask(sign, messages, delay, useTypewritingEffect).runTaskTimer(this, 0L, delay * 20L);
                } catch (Exception e) {
                    // If there is an error processing the sign configuration, log a warning
                    getLogger().warning(String.format(messagesConfig.getString("config_error." + language), e.getMessage()));
                }
            }
        } catch (Exception e) {
            // If there is an error initializing the plugin, log a severe error and disable the plugin
            getLogger().severe(String.format(messagesConfig.getString("init_error." + language), e.getMessage()));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // Task that updates a sign's text periodically
    private class SignCarouselTask extends BukkitRunnable {

        // The sign that this task updates.
        private final Sign sign;
        // The list of messages for this sign.
        private final List<List<String>> messages;
        // Whether to use the typewriting effect when displaying messages.
        private final boolean useTypewritingEffect;
        // The index of the current message in the list of messages.
        private int currentIndex = 0;

        // Create a new SignCarouselTask with the given sign, messages, delay, and typewriting effect setting
        public SignCarouselTask(Sign sign, List<List<String>> messages, int delay, boolean useTypewritingEffect) {
            this.sign = sign;
            this.messages = messages;
            this.useTypewritingEffect = useTypewritingEffect;
        }

        // Called periodically to update the sign's text
        @Override
        public void run() {
            // Reset the current index if it's past the end of the messages list
            if (currentIndex >= messages.size()) {
                currentIndex = 0;
            }

            // Get the current message
            List<String> currentMessage = messages.get(currentIndex);

            // Clear the sign's text
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, "");
            }
            sign.update();

            // If the typewriting effect is enabled, start a new TypewriterEffect task
            // Otherwise, set the sign's text to the current message
            if (useTypewritingEffect) {
                // If the typewriting effect is enabled, use a TypewriterEffect task to display the message.
                new TypewriterEffect(sign, currentMessage).runTaskTimer(SignCarousel.this, 0L, 2L);
            } else {
                // Otherwise, just set the lines of the sign directly.
                for (int i = 0; i < currentMessage.size(); i++) {
                    sign.setLine(i, ChatColor.translateAlternateColorCodes('&', currentMessage.get(i)));
                }
                sign.update();
            }

            // Move on to the next message
            currentIndex++;
        }
    }

    // Task that applies the typewriting effect to a sign's text
    private class TypewriterEffect extends BukkitRunnable {

        // The sign that this task updates.
        private final Sign sign;
        // The lines of the message to display.
        private final List<String> lines;
        // The index of the current line in the list of lines.
        private int currentLineIndex = 0;
        // The index of the current character in the current line.
        private int currentCharIndex = 0;

        // Create a new TypewriterEffect with the given sign and lines
        public TypewriterEffect(Sign sign, List<String> lines) {
            this.sign = sign;
            this.lines = lines;
        }

        // Called periodically to add one character at a time to the sign's text
        @Override
        public void run() {
            // If all lines have been processed, cancel the task
            if (currentLineIndex >= lines.size()) {
                cancel();
                return;
            }

            // Get the current line
            String currentLine = lines.get(currentLineIndex);

            // If all characters in the current line have been processed, move on to the next line
            // Otherwise, add the next character to the sign's text
            if (currentCharIndex > currentLine.length()) {
                currentCharIndex = 0;
                currentLineIndex++;
            } else {
                // Otherwise, add the next character to the line on the sign.
                sign.setLine(currentLineIndex, ChatColor.translateAlternateColorCodes('&', currentLine.substring(0, currentCharIndex)));
                sign.update();
                currentCharIndex++;
            }
        }
    }
}
