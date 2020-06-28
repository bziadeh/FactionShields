package com.cloth.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class ItemStackUtil {

    public static ItemStack deserialize(Map<String, Object> map) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ItemStack i = ItemStack.deserialize(map);
        if (map.containsKey("meta")) {
            try {
                if (ITEM_META_DESERIALIZATOR != null) {
                    ItemMeta im = (ItemMeta) DESERIALIZE.invoke(i, map.get("meta"));
                    i.setItemMeta(im);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw e;
            }
        }
        return i;
    }

    public static Map<String, Object> serialize(ItemStack item) {
        HashMap<String, Object> itemDocument = new HashMap(item.serialize());
        if (item.hasItemMeta()) {
            itemDocument.put("meta", new HashMap(item.getItemMeta().serialize()));
        }
        return itemDocument;
    }

    public static Class<?> getOBCClass(String className) {
        String fullName = "org.bukkit.craftbukkit." + getVersion() + className;
        Class<?> clazz = null;
        try {
            clazz = Class.forName(fullName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazz;
    }


    private static String getVersion() {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        String version = name.substring(name.lastIndexOf('.') + 1) + ".";
        return version;
    }

    private static final Class ITEM_META_DESERIALIZATOR = getOBCClass("inventory.CraftMetaItem").getClasses()[0];
    private static final Method DESERIALIZE = getDeserialize();

    private static Method getDeserialize() {

        try {
            return ITEM_META_DESERIALIZATOR.getMethod("deserialize", new Class[]{Map.class});
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }
}
