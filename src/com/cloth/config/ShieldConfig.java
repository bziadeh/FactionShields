package com.cloth.config;

import com.cloth.FactionShieldsPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Brennan on 4/11/2020.
 */
public class ShieldConfig {

    private static ShieldConfig instance = new ShieldConfig();

    // General settings.
    public static String TIMEZONE;
    public static int SHIELD_DURATION;
    public static String NO_PERMISSION;
    public static String INVALID_SYNTAX;
    public static String PLAYER_NOT_FOUND;
    public static String FACTION_NOT_FOUND;
    public static int SHIELD_BACKUP_INTERVAL;

    // Shield reset settings.
    public static int SHIELD_RESET_COOLDOWN;
    public static String SHIELD_RESET_FAIL;
    public static String SHIELD_RESET_SUCCESS;
    public static int SHIELD_RESET_RADIUS;
    public static String SHIELD_RESET_PREVENT;
    public static String SHIELD_FORCE_RESET;
    public static String SHIELD_RESET_ALL;

    // Shield messages.
    public static String NO_FACTION;
    public static String NO_FACTION_PERMISSION;
    public static String SHIELD_TITLE;
    public static String SHIELD_ACTIVE;
    public static String SHIELD_INACTIVE;
    public static String SHIELD_LOOKUP;
    public static String SHIELD_LOOKUP_FAILED;

    // Region messages.
    public static String REGION_SET;
    public static String REGION_SET_FAILURE;
    public static String REGION_SHIELD_FAILURE;
    public static String REGION_ALREADY_SET;
    public static List<String> REGION_REQUIRED;

    // Inventory settings.
    public static String INVENTORY_TITLE;
    public static int INVENTORY_SIZE;
    public static String SELECTED_BACKGROUND;
    public static String SELECTED_TIME;
    public static String ITEM_TYPES;
    public static String BORDER_COLOR;
    public static String NAMES;
    public static List<String> LORES;
    public static int START_SLOT;
    public static int END_SLOT;
    public static boolean IGNORE_SIDES;
    public static int STOP_BACKGROUND;

    // Placeholder settings.
    public static String PLACEHOLDER_SHIELD_ACTIVE;
    public static String PLACEHOLDER_SHIELD_INACTIVE;
    public static String PLACEHOLDER_SHIELD_NONE;

    private ShieldConfig() {
        // Private constructor, make sure this class is only initialized once.
        FactionShieldsPlugin.getInstance().saveDefaultConfig();
    }

    /**
     * Loads the settings from the configuration file asynchronously.
     */
    public Thread getLoadThread() {
        return new Thread(() -> {
            // General settings.
            TIMEZONE = getString("timezone");
            SHIELD_DURATION = getInt("shield-duration");
            NO_PERMISSION = getString("no-permission");
            INVALID_SYNTAX = getString("invalid-syntax");
            PLAYER_NOT_FOUND = getString("player-not-found");
            FACTION_NOT_FOUND = getString("faction-not-found");
            SHIELD_BACKUP_INTERVAL = getInt("backup-interval");

            // Shield reset settings.
            SHIELD_RESET_COOLDOWN = getInt("shield-reset-cooldown");
            SHIELD_RESET_FAIL = getString("shield-reset-fail");
            SHIELD_RESET_SUCCESS = getString("shield-reset-success");
            SHIELD_RESET_RADIUS = getInt("shield-reset-radius");
            SHIELD_RESET_PREVENT = getString("shield-reset-prevent");
            SHIELD_FORCE_RESET = getString("shield-force-reset");
            SHIELD_RESET_ALL = getString("shield-reset-all");

            // Shield messages.
            NO_FACTION = getString("no-faction");
            NO_FACTION_PERMISSION = getString("no-faction-permission");
            SHIELD_TITLE = getString("shield-title");
            SHIELD_ACTIVE = getString("shield-active");
            SHIELD_INACTIVE = getString("shield-inactive");
            SHIELD_LOOKUP = getString("shield-lookup");
            SHIELD_LOOKUP_FAILED = getString("shield-lookup-failed");

            // Region settings
            REGION_SET = getString("region-set");
            REGION_SET_FAILURE = getString("region-set-failure");
            REGION_SHIELD_FAILURE = getString("region-shield-failure");
            REGION_ALREADY_SET = getString("region-already-set");
            REGION_REQUIRED = getList("region-required");

            // Inventory settings.
            INVENTORY_TITLE = getString("inventory.title");
            INVENTORY_SIZE = getInt("inventory.size");
            SELECTED_BACKGROUND = getString("inventory.selected-background");
            SELECTED_TIME = getString("inventory.selected-time");
            ITEM_TYPES = getString("inventory.item-types");
            BORDER_COLOR = getString("inventory.border");
            NAMES = getString("inventory.names");
            LORES = getList("inventory.lores");
            START_SLOT = getInt("inventory.start-slot");
            END_SLOT = getInt("inventory.end-slot");
            IGNORE_SIDES = getBoolean("inventory.ignore-sides");
            STOP_BACKGROUND = getInt("inventory.stop-background");

            // Placeholder settings.
            PLACEHOLDER_SHIELD_ACTIVE = getString("placeholder-shield-active");
            PLACEHOLDER_SHIELD_INACTIVE = getString("placeholder-shield-inactive");
            PLACEHOLDER_SHIELD_NONE = getString("placeholder-shield-none");
        });
    }

    /**
     * Gets a boolean from the configuration file.
     *
     * @param path the path to the boolean.
     * @return the boolean.
     */
    public boolean getBoolean(String path) {
        return FactionShieldsPlugin.getInstance().getConfig().getBoolean(path);
    }

    /**
     * Gets a string from the configuration file and automatically
     * converts it's color codes.
     *
     * @param path the path to the string.
     * @return the converted string.
     */
    public String getString(String path) {
        return FactionShieldsPlugin.getInstance().getConfig().getString(path)
                .replaceAll("&", "§");
    }

    /**
     * Gets a configuration section.
     *
     * @param path the path to the configuration section.
     * @return the section.
     */
    public Set<String> getSection(String path) {
        return FactionShieldsPlugin.getInstance().getConfig().getConfigurationSection(path)
                .getKeys(false);
    }

    /**
     * Gets an integer from the configuration file.
     *
     * @param path the path to the integer.
     * @return the integer.
     */
    public int getInt(String path) {
        return FactionShieldsPlugin.getInstance().getConfig().getInt(path);
    }

    /**
     * Gets a list from the config and automatically converts its color codes.
     *
     * @param path the path to the list.
     * @return the list.
     */
    public List<String> getList(String path) {
        List<String> converted = new ArrayList<>();
        List<String> atPath = (List<String>) FactionShieldsPlugin.getInstance().getConfig().getList(path);
        atPath.forEach(string -> converted.add(string.replaceAll("&", "§")));
        return converted;
    }

    /**
     * Checks if the configuration contains the path.
     *
     * @param path the path.
     * @return whether or not the configuration file contains the specified path.
     */
    public boolean contains(String path) {
        return FactionShieldsPlugin.getInstance().getConfig().contains(path);
    }

    /**
     * Gets an instance of this class.
     *
     * @return an instance of this class.
     */
    public static ShieldConfig get() {
        return instance;
    }
}
