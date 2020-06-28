package com.cloth;

import com.cloth.config.ShieldConfig;
import com.cloth.inventory.InventoryHandler;
import com.cloth.shield.FactionEventCmd;
import com.cloth.shield.ShieldCmd;
import com.cloth.shield.ShieldHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Brennan on 4/11/2020.
 */
public class FactionShieldsPlugin extends JavaPlugin {

    private static FactionShieldsPlugin instance;

    private ShieldHandler shieldHandler;
    private InventoryHandler inventoryHandler;

    public void onEnable() {
        instance = this;

        // Startup thread.
        new Thread(() -> {
            Thread loadThread = ShieldConfig.get().getLoadThread();
            loadThread.start();

            // Force this thread to wait for the config to finish loading.
            try {
                loadThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Initialize our handler classes.
            shieldHandler = new ShieldHandler();
            inventoryHandler = new InventoryHandler(this);

            // Register commands.
            new ShieldCmd();
            new FactionEventCmd();

            // Start our backups.
            new ShieldBackups().start();
        }).start();
    }

    /**
     * Executes when this plugin is disabled.
     */
    public void onDisable() {
        Thread saveShields = new Thread(() -> getShieldHandler().saveAll("shields.json"));
        saveShields.start();

        try {
            saveShields.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Registers the specified listener.
     *
     * @param listener the listener being registered.
     */
    public void registerListener(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    /**
     * Unregisters the specified listener.
     *
     * @param listener the listener being unregistered.
     */
    public void unregisterListener(Listener listener) {
        HandlerList.unregisterAll(listener);
    }

    /**
     * Gets an instance of the main class.
     *
     * @return an instance of the main class.
     */
    public static FactionShieldsPlugin getInstance() {
        return instance;
    }

    /**
     * Gets the ShieldHandler. Used for getting and adding shields.
     *
     * @return the shield handler.
     */
    public ShieldHandler getShieldHandler() {
        return shieldHandler;
    }

    /**
     * Gets the InventoryHandler.
     *
     * @return the inventory handler.
     */
    public InventoryHandler getInventoryHandler() {
        return inventoryHandler;
    }
}
