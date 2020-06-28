package com.cloth.inventory;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.cloth.shield.FactionShield;
import com.cloth.shield.ShieldHandler;
import com.cloth.util.ItemBuilder;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

/**
 * Created by Brennan on 4/15/2020.
 */
public class ListInventory extends ConfigInventory implements Listener {

    private Inventory inventory;

    private Map<Integer, FactionShield> shields = new HashMap<>();

    private static final ItemStack placeholder = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) DyeColor.BLACK.ordinal());

    public ListInventory(String configName) {
        super(configName);
        FactionShieldsPlugin.getInstance().registerListener(this);
        update();
    }

    public void open(Player player) {
        if(inventory == null) {
            player.openInventory(build());
        } else {
            player.openInventory(inventory);
        }
    }

    private void update() {
        new BukkitRunnable() {
            public void run() {
                updateSlots();
            }
        }.runTaskTimerAsynchronously(FactionShieldsPlugin.getInstance(), 0, 20);
    }

    private void updateSlots() {
        if(inventory == null) {
            return;
        }
        List<Integer> slots = Arrays.asList(shields.keySet().toArray(new Integer[0]));
        boolean removed = false;

        for(int i = slots.size() - 1; i >= 0; i--) {
            FactionShield shield = shields.get(i);
            // Removing shields that are no longer active from our map.
            if(shield == null || shield.isDestroyed() || !shield.isEnabled()) {
                shields.remove(i);
                removed = true;
                break;
            }
            ItemBuilder itemBuilder = new ItemBuilder(inventory.getItem(i));
            itemBuilder.setLore(getGlobalLore(), ImmutableList.of(Pair.of("%status%", shield.getLastElapsedString())));
            inventory.setItem(i, itemBuilder.build());
        }

        final ShieldHandler shieldHandler = FactionShieldsPlugin.getInstance()
                .getShieldHandler();

        boolean noMatch = shields.values().size() != shieldHandler.getFactionShields().stream().filter(FactionShield::isEnabled).count();

        if(removed || noMatch) {
            // Now we repopulate the inventory in the correct order.
            inventory.clear();
            shields.clear();
            populate();
        }
    }

    private Inventory build() {
        inventory = Bukkit.createInventory(null, size, title);
        populate();
        return inventory;
    }

    private void populate() {
        if(inventory == null) {
            return;
        }
        // Create our return button.
        inventory.setItem(inventory.getSize() - 5, new ItemBuilder(Material.BOOK)
                .setName("§7Go Back")
                .setLore("§eClick to return to the previous page.")
                .build());
        // Fill the inventory with active faction shields.
        String name;
        List<String> lore;
        if((name = getGlobalName()) != null && (lore = getGlobalLore()) != null) {
            FactionShieldsPlugin.getInstance().getShieldHandler().getFactionShields().forEach(shield -> {
                if(shield != null && !shield.isDestroyed() && shield.isEnabled()) {
                    int emptySlot = 0;
                    int lastRow = inventory.getSize() - 8;

                    while(emptySlot < lastRow && inventory.getItem(emptySlot) != null) {
                        emptySlot++;
                    }

                    if(shield.getLastElapsedString() != null) {
                        if(emptySlot < inventory.getSize() - 1) {
                            ItemBuilder item = new ItemBuilder(Material.getMaterial(ShieldConfig.ITEM_TYPES))
                                    .setName(name.replaceAll("%faction%", shield.getFaction().getTag()))
                                    .setLore(lore, ImmutableList.of(Pair.of("%status%", shield.getLastElapsedString())));
                            inventory.setItem(emptySlot, item.build());
                            shields.put(emptySlot, shield);
                        }
                    }
                }
            });
        }
        if(fill) {
            fillBackground();
        }
    }

    private void fillBackground() {
        for(int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if(item == null || item.getType() == Material.AIR) {
                inventory.setItem(i, placeholder);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(event.getClickedInventory() == null) {
            return;
        }

        if(event.getClickedInventory().getTitle().equalsIgnoreCase(title)) {
            event.setCancelled(true);

            final FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
            final Player player = (Player) event.getWhoClicked();
            final FactionShield shield = plugin.getShieldHandler().getShield(player);

            // This is the slot that the return button is located.
            if(event.getSlot() == inventory.getSize() - 5) {
                Inventory returnInventory;

                if(shield != null) {
                    returnInventory = shield.getInventory();
                } else {
                    returnInventory = plugin.getInventoryHandler().getDefaultInventory();
                }

                player.openInventory(returnInventory);
            }
        }
    }
}
