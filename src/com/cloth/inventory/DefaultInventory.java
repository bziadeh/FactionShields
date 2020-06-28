package com.cloth.inventory;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Role;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import java.util.List;

/**
 * Created by Brennan on 4/12/2020.
 */
public class DefaultInventory implements Listener {

    private ConfirmInventory confirmInventory;
    private ListInventory listInventory;

    public DefaultInventory() {
        confirmInventory = new ConfirmInventory("confirm-menu");
        listInventory = new ListInventory("list-menu");
        FactionShieldsPlugin.getInstance().registerListener(this);
    }

    /**
     * Handles when a player clicks a button in the default
     * shield inventory (selecting a shield).
     *
     * @param event the InventoryClickEvent.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getClickedInventory();

        if(inventory != null && inventory.getTitle() != null && inventory.getTitle()
                .equalsIgnoreCase(ShieldConfig.INVENTORY_TITLE)) {

            event.setCancelled(true);

            if(event.getCurrentItem() == null) {
                return;
            }

            final Player player = (Player) event.getWhoClicked();
            final FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
            final FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);

            if(plugin.getShieldHandler().hasShield(player)) {
                if(plugin.getInventoryHandler().getTimeButtons().stream()
                        .anyMatch(itemData -> itemData.getSlot() == event.getSlot())) {
                    return;
                }
            }

            final InventoryHandler inventoryHandler = FactionShieldsPlugin.getInstance()
                    .getInventoryHandler();

            if(inventoryHandler.getShieldsButtonSlot() == event.getSlot()) {
                listInventory.open(player);
                return;
            }

            final List<ItemData> buttons = inventoryHandler.getTimeButtons();

            buttons.forEach(item -> {
                if(item.getSlot() == event.getSlot()) {
                    if(fplayer.getRole() != Role.LEADER && fplayer.getRole() != Role.COLEADER) {
                        player.sendMessage(ShieldConfig.NO_FACTION_PERMISSION);
                        player.closeInventory();
                        return;
                    }
                    confirmInventory.buildInventory((Player) event.getWhoClicked(), item.getTime(), item);
                }
            });
        }
    }
}
