package com.cloth.inventory;

import com.cloth.config.ShieldConfig;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Created by Brennan on 4/15/2020.
 */
public abstract class ConfigInventory {

    protected String title;
    protected int size;
    protected boolean fill;
    protected Map<Integer, ItemData> items = new HashMap<>();

    private String globalName;
    private List<String> globalLore;

    public ConfigInventory(String configName) {
        load(configName);
    }

    private void load(String configName) {
        new Thread(() -> {
            final ShieldConfig config = ShieldConfig.get();
            final String path = configName + ".";

            // These contains checks aren't required, but will prevent potential errors.
            if(config.contains(path + "title"))
                title = config.getString(path + "title");
            if(config.contains(path + "size"))
                size = config.getInt(path + "size");
            if(config.contains(path + "fill"))
                fill = config.getBoolean(path + "fill");
            if(config.contains(path + "name"))
                globalName = config.getString(path + "name");
            if(config.contains(path + "lore"))
                globalLore = config.getList(path + "lore");

            if(config.contains(path + "items")) {
                // Load the "confirm selection gui" settings from config.
                for(String slot : config.getSection(path + "items")) {
                    final String itemPath = path + "items." + slot + ".";

                    String name = config.getString(itemPath + "name");
                    Material material = Material.getMaterial(config.getString(itemPath + "type"));

                    // Check if a color is specified in the config.
                    DyeColor color = config.contains(itemPath + "color")
                            ? DyeColor.valueOf(config.getString(itemPath + "color"))
                            : null;
                    // Check if an action is specified in the config.
                    Action action = config.contains(itemPath + "action")
                            ? Action.valueOf(config.getString(itemPath + "action"))
                            : null;

                    // Load slot and lore data.
                    List<String> lore = config.getList(itemPath + "lore");
                    int slotInt = Integer.parseInt(slot);

                    // Create the ItemStack.
                    ItemStack itemToAdd;
                    if(color != null) {
                        itemToAdd = new ItemStack(material, 1, (short) color.ordinal());
                    } else {
                        itemToAdd = new ItemStack(material);
                    }
                    // Set the lore and displayname.
                    ItemMeta meta = itemToAdd.getItemMeta();
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                    itemToAdd.setItemMeta(meta);

                    items.put(slotInt, new ItemData(itemToAdd, slotInt, action, null));
                }
            }
        }).start();
    }

    /**
     * Converts a placeholder in every element of a list.
     *
     * @param list the list being converted.
     * @param key the placeholder value.
     * @param value the replacement.
     * @return the converted list.
     */
    protected List<String> convertPlaceholder(List<String> list, String key, String value) {
        List<String> converted = new ArrayList<>();
        list.forEach(line -> converted.add(line.replaceAll(key, value)));
        return converted;
    }

    public List<String> getGlobalLore() {
        return globalLore;
    }

    public String getGlobalName() {
        return globalName;
    }
}
