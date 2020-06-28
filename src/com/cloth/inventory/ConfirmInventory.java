package com.cloth.inventory;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.cloth.shield.FactionShield;
import com.massivecraft.factions.FPlayers;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Created by Brennan on 4/12/2020.
 */
public class ConfirmInventory extends ConfigInventory implements Listener {

    private Inventory inventory;
    private Map<UUID, ItemData> confirmData = new HashMap<>();

    public ConfirmInventory(String configName) {
        super(configName);
        FactionShieldsPlugin.getInstance().registerListener(this);
    }

    /**
     * Builds a confirmation inventory based on the specified time.
     *
     * @param player the player this inventory is being built for.
     * @param time the faction shield time.
     * @return the new inventory.
     */
    public void buildInventory(Player player, String time, ItemData data) {
        inventory = Bukkit.createInventory(null, size, title);

        items.keySet().forEach(slot -> {
            // Update placeholders values.
            ItemStack converted = items.get(slot).getItem().clone();
            ItemMeta meta = converted.getItemMeta();
            meta.setLore(convertPlaceholder(meta.getLore(), "%time%", time));
            converted.setItemMeta(meta);

            // Add the menu item to the inventory.
            inventory.setItem(slot, converted);
        });

        if(fill) {
            for(int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if(item == null || item.getType() == Material.AIR) {
                    inventory.setItem(i, new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) DyeColor.BLACK.ordinal()));
                }
            }
        }

        confirmData.put(player.getUniqueId(), data);
        player.openInventory(inventory);
    }

    /**
     * Handles when a player clicks a button in the shield select confirmation
     * inventory.
     *
     * @param event the InventoryClickEvent.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();

        if(event.getClickedInventory() == null) {
            return;
        }

        if(event.getClickedInventory().getTitle().equalsIgnoreCase(title)) {
            event.setCancelled(true);
            final Player player = (Player) event.getWhoClicked();
            if(confirmData.containsKey(player.getUniqueId())) {
                ItemData itemData = confirmData.get(player.getUniqueId());
                ItemData buttonData = items.get(event.getSlot());
                Action action;
                if(buttonData != null && (action = buttonData.getAction()) != null) {
                    switch(action) {
                        case CONFIRM:
                            confirmShield((Player) event.getWhoClicked(), itemData);
                            break;
                        case CANCEL:
                            event.getWhoClicked().openInventory(plugin.getInventoryHandler().getDefaultInventory());
                            break;
                        default:
                            System.out.println("Not confirm or cancel?");
                            break;
                    }
                    confirmData.remove(player.getUniqueId());
                }
            }
        }
    }

    /**
     * Executed when a player clicks the confirm button.
     *
     * @param player the player who clicked.
     * @param data the data of the button they clicked.
     */
    private void confirmShield(Player player, ItemData data) {
        FactionShield factionShield = new FactionShield(FPlayers.getInstance().getByPlayer(player).getFactionId(), data);

        FactionShieldsPlugin.getInstance().getShieldHandler().addShield(factionShield);

        player.closeInventory();

        ShieldConfig.REGION_REQUIRED.forEach(player::sendMessage);
    }

    /**
     * Gets the menu items for the confirm inventory.
     *
     * @return the menu items.
     */
    public Map<Integer, ItemData> getMenuItems() {
        return items;
    }
}
