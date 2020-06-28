package com.cloth.shield;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.FactionDisbandEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Brennan on 4/11/2020.
 */
public class ShieldHandler implements Listener {

    private static final SimpleDateFormat backupFormat;

    static {
        backupFormat = new SimpleDateFormat("hh mm ss");
    }

    private List<FactionShield> factionShields = new ArrayList<>();

    public ShieldHandler() {
        updateShields();
        FactionShieldsPlugin.getInstance().registerListener(this);
    }

    private void updateShields() {
        new BukkitRunnable() {
            @Override
            public void run() {
                factionShields.forEach(FactionShield::update);
            }
        }.runTaskTimerAsynchronously(FactionShieldsPlugin.getInstance(),0, 20);
    }

    /**
     * Serializes all faction shield objects to the shields.json file.
     */
    public void saveAll(String path) {
        final FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
        factionShields.forEach(shield -> shield.setInventoryBase64(shield.getInventory()));
        try(Writer writer = new FileWriter(plugin.getDataFolder() + "/" + path)) {
            Gson gson = new Gson();
            gson.toJson(factionShields, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadAll() {
        FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
        File file = new File(plugin.getDataFolder() + "/shields.json");
        if(!file.exists()) {
            return;
        }
        try (Reader reader = new FileReader(plugin.getDataFolder() + "/shields.json")) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<FactionShield>>(){}.getType();
            factionShields = gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        factionShields.forEach(shield -> {
            plugin.registerListener(shield);
            shield.update();
        });
    }


    /**
     * Creates a backup JSON of all saved shields.
     */
    public void backup() {
        final FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();

        File file = new File(plugin.getDataFolder() + "/backups");

        if(!file.exists()) {
            file.mkdirs();
        }

        final String time = backupFormat.format(new Date());
        final String fileName = String.format("%s.json", time);

        saveAll("backups/" + fileName);
    }

    @EventHandler
    public void onFactionDisband(FactionDisbandEvent event) {
        if(event.getFaction() == null) {
            removeNullShields();
            return;
        }

        // Normal disband using command...
        for(int i = factionShields.size() - 1; i >= 0; i--) {
            FactionShield factionShield;
            if((factionShield = factionShields.get(i)).getFaction().getId().equalsIgnoreCase(event.getFaction().getId())) {
                factionShield.destroy();
            }
        }
    }

    private void removeNullShields() {
        for(int i = factionShields.size() - 1; i >= 0; i--) {
            FactionShield factionShield = factionShields.get(i);
            if(factionShield.getFaction().getFPlayers().isEmpty()) {
                factionShield.destroy();
            }
        }
    }

    /**
     * Checks if the specified player has a faction shield.
     *
     * @param player the player being checked.
     * @return whether or not the player has a faction shield.
     */
    public boolean hasShield(Player player) {
        return factionShields.stream().anyMatch(shield -> shield.getFaction()
                .equals(FPlayers.getInstance().getByPlayer(player).getFaction()));
    }

    /**
     * Checks if the specified faction has a faction shield.
     *
     * @param faction the faction being checked.
     * @return whether or not the faction has a faction shield.
     */
    public boolean hasShield(Faction faction) {
        return factionShields.stream().anyMatch(shield -> shield.getFaction().equals(faction));
    }

    /**
     * Gets the faction shield of the specified player.
     *
     * @param player the player whose shield is being requested.
     * @return the faction shield.
     */
    public FactionShield getShield(Player player) {
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
        return factionShields.stream().filter(shield -> shield.getFaction()
                .getFPlayers().contains(fplayer)).findFirst().orElse(null);
    }

    /**
     * Gets the faction shield of the specified faction.
     *
     * @param faction the faction whose shield is being requested.
     * @return the faction shield.
     */
    public FactionShield getShield(Faction faction) {
        return factionShields.stream().filter(shield -> shield.getFaction().equals(faction))
                .findFirst().orElse(null);
    }


    /**
     * Gets the time that the shield will end after activation.
     *
     * For example, if the start time is 5 AM and the duration is 8 hours,
     * what time will the shield activation be finished? This function will
     * return 1 PM.
     *
     * @param dayOrNight is the start time AM or PM?
     * @param start what hour of the day is the start time?
     * @return the end time.
     */
    public String getEndTime(String dayOrNight, int start) {
        int localTime = start;
        for(int i = 0; i < ShieldConfig.SHIELD_DURATION; i++) {
            localTime++;
            if(localTime == 12) {
                dayOrNight = dayOrNight.equals("AM") ? "PM" : "AM";
            } else if(localTime > 12) {
                localTime = 1;
            }
        }
        return String.format("%d %s %s", localTime, dayOrNight, ShieldConfig.TIMEZONE);
    }

    /**
     * Adds a faction shield to memory. Everything in our factionShields list
     * will be saved to JSON when the server stops.
     *
     * @param shield the shield being added.
     */
    public void addShield(FactionShield shield) {
        factionShields.add(shield);
    }

    /**
     * Removes a faction shield from memory. This shield will no longer be saved
     * to the JSON file when the server stops.
     *
     * @param shield the shield being removed.
     */
    public void removeShield(FactionShield shield) {
        factionShields.remove(shield);
    }

    /**
     * Gets a read-only list of the faction shields.
     *
     * @return a read-only list of the faction shields.
     */
    public List<FactionShield> getFactionShields() {
        return Collections.unmodifiableList(factionShields);
    }
}
