package fr.freebuild.playerjoingroup.spigot.utils;

import fr.freebuild.playerjoingroup.spigot.PlayerJoinGroup;
import org.bukkit.ChatColor;

public class Utils {

    /**
     * Change String too support color
     *
     * @param toChange String to convert colors
     * @return
     */
    public static String toColor(String toChange) {
        return ChatColor.translateAlternateColorCodes('&', toChange);
    }

    /**
     * Get config string variable into colors
     *
     * @param path Path to config
     * @return
     */
    public static String getConfigString(String path) {
        return Utils.toColor(PlayerJoinGroup.plugin.getConfig().getString(path));
    }

    /**
     * Replace a format parameter inside a message
     *
     * @param message Message
     * @param param Key of parameter to replace
     * @param value Value of parameter to replace
     * @return Message modified
     */
    public static String format(String message, FormatParam param, String value) {
        return message.replace(param.getValue(), value);
    }

    /**
     * Increase the counter and save config file
     *
     * @param path Path to counter
     * @return Return counter increased of 1
     */
    public static Integer increaseCounter(String path) {
        Integer counter = PlayerJoinGroup.plugin.getConfig().getInt(path, 0);
        counter += 1;
        PlayerJoinGroup.plugin.getConfig().set(path, counter);
        PlayerJoinGroup.plugin.saveConfig();
        return counter;
    }

    public static String getFirstConnectionMessage(String playerName) {
        final Integer counter = increaseCounter("PlayerCounter");
        String message = getConfigString("FirstJoinMessage");
        message = Utils.format(message, FormatParam.COUNTER, counter.toString());
        message = Utils.format(message, FormatParam.PLAYER, playerName);
        return message;
    }

    public static String getHasPlayedBeforeMessage(String playerName) {
        String message = getConfigString("JoinMessage");
        return Utils.format(message, FormatParam.PLAYER, playerName);
    }

    public static String getPlayerLeaveMessage(String playerName) {
        String message = getConfigString("QuitMessage");
        return Utils.format(message, FormatParam.PLAYER, playerName);
    }
}
