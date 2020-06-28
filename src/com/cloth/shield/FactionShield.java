package com.cloth.shield;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.cloth.inventory.InventoryHandler;
import com.cloth.inventory.ItemData;
import com.cloth.util.InvUtil;
import com.cloth.util.ItemBuilder;
import com.connorlinfoot.titleapi.TitleAPI;
import com.google.common.collect.ImmutableList;
import com.massivecraft.factions.*;
import com.sun.org.apache.bcel.internal.generic.FLOAD;
import io.netty.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.Inventory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.FutureTask;

/**
 * Created by Brennan on 4/11/2020.
 */
public class FactionShield implements Listener {

    private static final SimpleDateFormat sdf;

    static {
        sdf = new SimpleDateFormat("h:mm a z");
        sdf.setTimeZone(TimeZone.getTimeZone(ShieldConfig.TIMEZONE.replace("EST", "America/New_York")));
    }

    private transient Inventory inventory;
    private String factionId;
    private ItemData itemData;
    private boolean enabled;
    private long lastActivation;
    private String waiting;
    private String inventoryBase64;
    private String lastElapsedString;
    private boolean destroyed;
    private List<FLocation> region;

    public FactionShield(String factionId, ItemData data) {
        this.factionId = factionId;
        this.itemData = data;
        this.enabled = false;
        this.lastActivation = -1;
        this.waiting = " §8- §7Waiting for " + this.itemData.getTime() + "§7...";
        this.destroyed = false;
        this.region = new ArrayList<>();
        this.buildInventory();

        // Register all events in this object.
        FactionShieldsPlugin.getInstance().registerListener(this);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(isEnabled()) {
            if(region.contains(new FLocation(event.getLocation()))) {
                EntityType entityType = event.getEntityType();
                if(entityType == EntityType.PRIMED_TNT || entityType == EntityType.CREEPER) {
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Destroys this faction shield object.
     */
    public void destroy() {
        final FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
        final Faction faction = getFaction();

        plugin.unregisterListener(this);
        plugin.getShieldHandler().removeShield(this);
        this.destroyed = true;
    }

    /**
     * Builds the custom inventory for this faction shield.
     */
    public void buildInventory() {
        FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
        inventory = Bukkit.createInventory(null, ShieldConfig.INVENTORY_SIZE, ShieldConfig.INVENTORY_TITLE);
        InventoryHandler inventoryHandler = plugin.getInventoryHandler();
        Inventory defaultInventory = inventoryHandler.getDefaultInventory();
        for(int i = 0; i < defaultInventory.getSize(); i++) {
            inventory.setItem(i, defaultInventory.getItem(i));
            if(itemData.getSlot() == i) {
                inventory.getItem(i).setType(Material.getMaterial(ShieldConfig.SELECTED_TIME));
            } else if(i >= ShieldConfig.START_SLOT && i <= ShieldConfig.END_SLOT) {
                inventory.getItem(i).setType(Material.getMaterial(ShieldConfig.SELECTED_BACKGROUND));
            }
            if(inventoryHandler.getInfoButtonSlot() == i) {
                updateInfoButton(enabled, waiting, inventoryHandler);
            }
        }
        this.inventoryBase64 = InvUtil.toBase64(inventory);
    }

    /**
     * Updates the contents of the faction shield GUI.
     */
    public void update() {
        updateStatus();
    }

    /**
     * Calculates and updates the status.
     */
    public void updateStatus() {
        final InventoryHandler inventoryHandler = FactionShieldsPlugin.getInstance()
                .getInventoryHandler();
        final Date current = new Date(System.currentTimeMillis());
        if(!isEnabled()) {
            String date = sdf.format(current);
            String[] elements = date.split(" ");
            String[] time = elements[0].split(":");
            String hour = time[0]; // 9:00 AM, this would equal 9
            String minutes = time[1]; // 2:30 PM this would equal 30
            String amOrPm = elements[1];
            String[] currentTime = this.itemData.getTime().split(" ");
            String currentHour = currentTime[0];
            String currentDayOrNight = currentTime[1];
            // Is it the same hour?
            if(amOrPm.equalsIgnoreCase(currentDayOrNight) && hour.equalsIgnoreCase(currentHour) && minutes.equalsIgnoreCase("00")) {
                activate();
            }
            return;
        }
        // If the shield has been activated before...
        if(lastActivation != -1) {
            long timeElapsed = System.currentTimeMillis() - lastActivation;
            long seconds = timeElapsed / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if((int) hours >= ShieldConfig.SHIELD_DURATION) {
                deactivate();
                return;
            }
            seconds = seconds - (minutes * 60);
            minutes = minutes - (hours * 60);

            int hoursRemaining = (ShieldConfig.SHIELD_DURATION - 1) - (int) hours;
            int minutesRemaining = 59 - (int) minutes;
            int secondsRemaining = 59 - (int) seconds;

            String time = String.format("%dh %dm %ds", hoursRemaining, minutesRemaining, secondsRemaining);
            updateInfoButton(enabled, " §8- §7" + time, inventoryHandler);

            this.lastElapsedString = time;
        }
    }

    /**
     * Get the amount of elapsed time (since activated) as a string.
     *
     * @return the amount of elapsed time as a formatted string.
     */
    public String getLastElapsedString() {
        return lastElapsedString;
    }

    private void updateInfoButton(boolean active, String addon, InventoryHandler inventoryHandler) {
        Inventory defaultInventory = inventoryHandler.getDefaultInventory();

        final ItemBuilder itemBuilder = new ItemBuilder(defaultInventory.getItem(inventoryHandler.getInfoButtonSlot()).clone());
        final String status = active ? "§aACTIVE" : "§cNOT ACTIVE";

        // Set lore with new placeholders.
        itemBuilder.setLore(inventoryHandler.getInfoButtonLore(), ImmutableList.of(
                Pair.of("%duration%", ShieldConfig.SHIELD_DURATION + " hours"),
                Pair.of("%status%", status + addon)));

        getInventory().setItem(inventoryHandler.getInfoButtonSlot(), itemBuilder.build());
    }

    private void deactivate() {
        enabled = false;

        notifyStateChange(ShieldConfig.SHIELD_INACTIVE);

        updateInfoButton(false, waiting, FactionShieldsPlugin.getInstance().getInventoryHandler());
    }

    /**
     * Activates this shield.
     */
    private void activate() {
        enabled = true;

        notifyStateChange(ShieldConfig.SHIELD_ACTIVE);

        lastActivation = System.currentTimeMillis();
    }

    private void notifyStateChange(String message) {
        try {
            getFaction().getOnlinePlayers().forEach(player -> {
                if(inventory.getViewers().contains(player.getPlayer())) {
                    player.closeInventory();
                }

                TitleAPI.sendTitle(player.getPlayer(), 20, 20, 20,
                        ShieldConfig.SHIELD_TITLE, message);
            });
        } catch (NullPointerException error) {
            // faction removed from system? no players online?
        }
    }

    /**
     * Gets the faction with this shield.
     *
     * @return the faction with this shield.
     */
    public Faction getFaction() {
        return Factions.getInstance().getFactionById(factionId);
    }

    /**
     * Gets the faction id of the faction with this shield.
     *
     * @return the faction id of the faction with this shield.
     */
    public String getFactionId() {
        return factionId;
    }

    /**
     * Checks if this faction shield is enabled.
     *
     * @return whether or not this shield is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the faction shield inventory.
     *
     * @return the inventory.
     */
    public Inventory getInventory() {
        if(inventory == null) {
            try {
                inventory = InvUtil.fromBase64(inventoryBase64, ShieldConfig.INVENTORY_TITLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return inventory;
    }

    public ItemData getItemData() {
        return itemData;
    }

    public void setInventoryBase64(Inventory inventory) {
        this.inventoryBase64 = InvUtil.toBase64(inventory);
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public List<FLocation> getRegion() {
        return region;
    }

    public void setRegion(FLocation start) {
        // Add the original chunk.
        region.add(start);

        // Add all connected chunks.
        loop(start);
    }

    public void loop(FLocation loc) {
        // Get nearby chunks...
        List<FLocation> nearby = getRelativeLocations(loc);

        for(FLocation chunk : nearby) {
            if(Board.getInstance().getFactionAt(chunk).getId().equalsIgnoreCase(factionId)) {
                if(!region.contains(chunk)) {
                    region.add(chunk);
                    loop(chunk);
                }
            }
        }
    }

    private List<FLocation> getRelativeLocations(FLocation location) {

        FLocation a = location.getRelative(1, 0);
        FLocation b = location.getRelative(-1, 0);
        FLocation c = location.getRelative(0, 1);
        FLocation d = location.getRelative(0, -1);

        return Arrays.asList(a, b, c, d);
    }
}
