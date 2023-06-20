package com.ashkiano.signcarousel;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;

// Main plugin class that extends JavaPlugin.
public class SignCarousel extends JavaPlugin {

    // This method is called when the plugin is enabled.
    @Override
    public void onEnable() {
        // Save the default configuration if it doesn't exist yet.
        saveDefaultConfig();

        Metrics metrics = new Metrics(this, 18812);

        // Load the configuration.
        FileConfiguration config = getConfig();
        // Get list of sign configurations from the config file.
        List<Map<?, ?>> signConfigs = config.getMapList("signs");

        // Loop over each sign configuration.
        for (Map<?, ?> signConfig : signConfigs) {
            // Get the list of messages for this sign.
            List<List<String>> messages = (List<List<String>>) signConfig.get("messages");
            // Get the delay between messages for this sign.
            int delay = (int) signConfig.get("delay");
            // Check if typewriting effect is enabled for this sign.
            boolean useTypewritingEffect = (boolean) signConfig.get("typewriting_effect");

            // Get the location of this sign.
            Map<?, ?> locationData = (Map<?, ?>) signConfig.get("location");
            String worldName = (String) locationData.get("world");
            World world = getServer().getWorld(worldName);

            int x = (int) locationData.get("x");
            int y = (int) locationData.get("y");
            int z = (int) locationData.get("z");

            Location signLocation = new Location(world, x, y, z);
            // Check if the block at the given location is a sign.
            if (!(signLocation.getBlock().getState() instanceof Sign)) {
                // If not, log a warning and skip this sign.
                getLogger().warning("Block at " + signLocation + " is not a sign!");
                continue;
            }
            // Get the sign at the given location.
            Sign sign = (Sign) signLocation.getBlock().getState();

            // Create a new SignCarouselTask for this sign and schedule it to run periodically.
            new SignCarouselTask(sign, messages, delay, useTypewritingEffect).runTaskTimer(this, 0L, delay * 20L);
        }
    }

    // This class represents a task that updates a sign's text periodically.
    private class SignCarouselTask extends BukkitRunnable {

        // The sign that this task updates.
        private final Sign sign;
        // The list of messages for this sign.
        private final List<List<String>> messages;
        // Whether to use the typewriting effect when displaying messages.
        private final boolean useTypewritingEffect;
        // The index of the current message in the list of messages.
        private int currentIndex = 0;

        // Create a new SignCarouselTask.
        public SignCarouselTask(Sign sign, List<List<String>> messages, int delay, boolean useTypewritingEffect) {
            this.sign = sign;
            this.messages = messages;
            this.useTypewritingEffect = useTypewritingEffect;
        }

        // This method is called when the task is run.
        @Override
        public void run() {
            // If we've reached the end of the list of messages, start over.
            if (currentIndex >= messages.size()) {
                currentIndex = 0;
            }

            // Get the current message.
            List<String> currentMessage = messages.get(currentIndex);

            // Clear the old message from the sign.
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, "");
            }
            sign.update();

            // Write the new message to the sign.
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

            // Move on to the next message.
            currentIndex++;
        }
    }

    // This class represents a task that displays a message on a sign using a typewriter effect.
    private class TypewriterEffect extends BukkitRunnable {

        // The sign that this task updates.
        private final Sign sign;
        // The lines of the message to display.
        private final List<String> lines;
        // The index of the current line in the list of lines.
        private int currentLineIndex = 0;
        // The index of the current character in the current line.
        private int currentCharIndex = 0;

        // Create a new TypewriterEffect.
        public TypewriterEffect(Sign sign, List<String> lines) {
            this.sign = sign;
            this.lines = lines;
        }

        // This method is called when the task is run.
        @Override
        public void run() {
            // If we've reached the end of the list of lines, cancel the task.
            if (currentLineIndex >= lines.size()) {
                cancel();
                return;
            }

            // Get the current line.
            String currentLine = lines.get(currentLineIndex);

            // If we've reached the end of the current line, move on to the next line.
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
