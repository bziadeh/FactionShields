package com.cloth.inventory;


import com.cloth.util.ItemStackUtil;
import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Created by Brennan on 4/12/2020.
 */
public class ItemData implements Serializable {

    private transient ItemStack item;
    private int slot;
    private Action action;
    private String time;
    private Map<String, Object> itemAsMap;

    public ItemData(ItemStack item, int slot, Action action, String time) {
        this.item = item;
        this.slot = slot;
        this.action = action;
        this.time = time;
        this.itemAsMap = ItemStackUtil.serialize(item);
    }

    public ItemStack getItem() {
        if(item == null) {
            try {
                return item = ItemStackUtil.deserialize(itemAsMap);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return item;
    }

    public int getSlot() {
        return slot;
    }

    public Action getAction() {
        return action;
    }

    public String getTime() {
        return time;
    }
}