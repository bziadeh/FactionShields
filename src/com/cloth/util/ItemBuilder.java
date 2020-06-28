package com.cloth.util;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Brennan on 4/11/2020.
 */
public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Material material) {
        item = new ItemStack(material);
        meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, DyeColor color) {
        item = new ItemStack(material, 1, (short) color.ordinal());
        meta = item.getItemMeta();
    }

    public ItemBuilder setLore(List<String> lore, List<Pair<String, String>> placeholders) {
        List<String> updated = new ArrayList<>();
        List<String> loreCopy = new ArrayList<>(lore);
        for(String line : loreCopy) {
            String temp = line;
            for(Pair<String, String> pair : placeholders) {
                temp = temp.replaceAll(pair.getKey(), pair.getValue());
            }
            updated.add(temp);
        }
        meta.setLore(updated);
        return this;
    }

    public ItemBuilder setName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setLore(String lore) {
        meta.setLore(Collections.singletonList(lore));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
