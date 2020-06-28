package com.cloth.inventory;

import com.cloth.FactionShieldsPlugin;
import com.cloth.util.ItemBuilder;
import com.cloth.config.ShieldConfig;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Brennan on 4/11/2020.
 */
public class InventoryHandler {

    private FactionShieldsPlugin plugin;
    private Inventory defaultInventory;
    private List<ItemData> timeButtons = new ArrayList<>();

    // Information button.
    private boolean infoButtonEnabled;
    private String infoButtonName;
    private String infoButtonType;
    private int infoButtonSlot;
    private List<String> infoButtonLore;

    // Shields button.
    private boolean shieldsButtonEnabled;
    private String shieldsButtonName;
    private String shieldsButtonType;
    private int shieldsButtonSlot;
    private List<String> shieldsButtonLore;

    public InventoryHandler(FactionShieldsPlugin plugin) {
        this.plugin = plugin;

        // Load the information button settings from the config and build.
        new Thread(() -> {
            final ShieldConfig config = ShieldConfig.get();

            // Load information button.
            final String pathInfo = "inventory.info-button.";
            infoButtonEnabled = config.getBoolean(pathInfo + "enabled");
            infoButtonName = config.getString(pathInfo + "name");
            infoButtonType = config.getString(pathInfo + "type");
            infoButtonSlot = config.getInt(pathInfo + "slot");
            infoButtonLore = config.getList(pathInfo + "lore");

            // Load shields button.
            final String pathShields = "inventory.shields-button.";
            shieldsButtonEnabled = config.getBoolean(pathShields + "enabled");
            shieldsButtonName = config.getString(pathShields + "name");
            shieldsButtonType = config.getString(pathShields + "type");
            shieldsButtonSlot = config.getInt(pathShields + "slot");
            shieldsButtonLore = config.getList(pathShields + "lore");

            build();
        }).start();

        // Loads from config + handles events.
        DefaultInventory defaultInventory = new DefaultInventory();
    }

    /**
     * Builds the default shield inventory using information from the
     * configuration file.
     */
    private void build() {
        defaultInventory = Bukkit.createInventory(null, ShieldConfig.INVENTORY_SIZE, ShieldConfig.INVENTORY_TITLE);

        final Material material = Material.getMaterial(ShieldConfig.ITEM_TYPES);
        final ItemStack borderColor = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) DyeColor.valueOf(ShieldConfig.BORDER_COLOR).ordinal());

        int time = 1;
        String dayOrNight = "AM";

        for(int i = ShieldConfig.START_SLOT; i <= ShieldConfig.END_SLOT; i++,
                time = (time == 12 ? 1 : time + 1)) {

            if(ShieldConfig.IGNORE_SIDES) {
                if(i % 9 == 0) {
                    i+=2;
                } else if((i + 1) % 9 == 0) {
                    i+=2;
                }
            }

            // Moving from AM to PM and vice versa.
            if(time > 11 && dayOrNight.equalsIgnoreCase("AM")) {
                dayOrNight = "PM";
            } else if (time > 11 & dayOrNight.equalsIgnoreCase("PM")){
                dayOrNight = "AM";
            }

            // Generate a name for this button using information from the config.
            final String stringTime = time + " " + dayOrNight + " " + ShieldConfig.TIMEZONE;
            final String name = ShieldConfig.NAMES.replaceAll("%time%", stringTime);

            // Build this item and add it to the inventory.
            ItemBuilder itemBuilder;
            if(material == Material.STAINED_GLASS_PANE)
                itemBuilder = new ItemBuilder(material, DyeColor.RED);
            else
                itemBuilder = new ItemBuilder(material);
            itemBuilder.setName(name);

            if(!ShieldConfig.LORES.isEmpty())
                itemBuilder.setLore(updatePlaceholders(dayOrNight, time, stringTime, ShieldConfig.LORES));
            final ItemStack item = itemBuilder.build();

            defaultInventory.setItem(i, item);
            // Add ItemData to our timeButtons list so we can get it in defaultInventory events.
            timeButtons.add(new ItemData(item, i, null, stringTime));
        }

        // Load the information button.
        if(infoButtonEnabled) {
            loadInfoButton();
        }
        // Load the shields button.
        if(shieldsButtonEnabled) {
            loadShieldsButton();
        }
        // Load the background.
        for(int i = 0; i < defaultInventory.getSize(); i++) {
            ItemStack item = defaultInventory.getItem(i);
            if(item == null || item.getType() == Material.AIR) {
                defaultInventory.setItem(i, borderColor);
            }
            if(i == ShieldConfig.STOP_BACKGROUND)
                break;
        }

        FactionShieldsPlugin.getInstance().getShieldHandler().loadAll();
    }

    /**
     * Updates the placeholders, currently only %time%, from the list.
     *
     * @param time the selected time.
     * @param configLore the lore specified in the config.
     * @return the converted list.
     */
    private List<String> updatePlaceholders(String dayOrNight, int intTime, String time, List<String> configLore) {
        final List<String> updated = new ArrayList<>();
        configLore.forEach(line -> updated.add(line
                .replaceAll("%time%", time)
                .replaceAll("%end%", plugin.getShieldHandler().getEndTime(dayOrNight, intTime))
                .replaceAll("%duration%", String.valueOf(ShieldConfig.SHIELD_DURATION))));
        return updated;
    }

    /**
     * Loads the information button in the default GUI.
     */
    private void loadInfoButton() {
        final String duration = ShieldConfig.SHIELD_DURATION + " hours";
        defaultInventory.setItem(infoButtonSlot,
                new ItemBuilder(Material.getMaterial(infoButtonType))
                        .setName(infoButtonName)
                        .setLore(infoButtonLore, ImmutableList.of(Pair.of("%duration%", duration), Pair.of("%status%", "§cNOT ACTIVE")))
                        .build());
    }

    /**
     * Loads the shields list button in the default GUI.
     */
    private void loadShieldsButton() {
        defaultInventory.setItem(shieldsButtonSlot,
                new ItemBuilder(Material.getMaterial(shieldsButtonType))
                        .setName(shieldsButtonName)
                        .setLore(shieldsButtonLore)
                        .build());
    }

    /**
     * Gets the time buttons from the faction shield GUI.
     *
     * @return the time buttons.
     */
    public List<ItemData> getTimeButtons() {
        return timeButtons;
    }

    /**
     * Gets the default shield inventory when a player types
     * the faction shield command.
     *
     * @return the default shield inventory / gui.
     */
    public Inventory getDefaultInventory() {
        return defaultInventory;
    }

    public int getInfoButtonSlot() {
        return infoButtonSlot;
    }

    public int getShieldsButtonSlot() {
        return shieldsButtonSlot;
    }

    public List<String> getInfoButtonLore() {
        return infoButtonLore;
    }
}
