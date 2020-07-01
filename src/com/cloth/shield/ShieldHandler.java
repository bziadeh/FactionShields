package com.cloth.shield;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.struct.Role;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Brennan on 4/11/2020.
 */
public class ShieldHandler implements Listener {

    private static final SimpleDateFormat backupFormat;

    static {
        backupFormat = new SimpleDateFormat("hh mm ss");
    }

    private List<FactionShield> factionShields = new ArrayList<>();
    private Map<UUID, Long> resetPending = new HashMap<>();

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

    /**
     * Loads the serialized JSON data from shields.json into memory.
     */
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
     * Checks if the specified player is in a faction (other than Wilderness)
     *
     * @param player the player being checked.
     * @return whether or not the player has a faction.
     */
    public boolean hasFaction(FPlayer player) {
        if(!player.hasFaction() || player.getFaction().isWilderness()) {
            return false;
        }
        return true;
    }

    /**
     * Called when a player attempts to reset their shield.
     *
     * @param commandExecutor the player executing the command.
     */
    public void reset(Player commandExecutor) {
        if(commandExecutor == null) {
            return;
        }

        final FPlayer fplayer = FPlayers.getInstance().getByPlayer(commandExecutor);

        if(!hasFaction(fplayer)) {
            commandExecutor.sendMessage(ShieldConfig.NO_FACTION);
            return;
        }

        final Role role = fplayer.getRole();
        if(role != Role.LEADER && role != Role.COLEADER) {
            commandExecutor.sendMessage(ShieldConfig.NO_FACTION_PERMISSION);
            return;
        }

        final ShieldHandler shieldHandler = FactionShieldsPlugin.getInstance().getShieldHandler();
        if(!shieldHandler.hasShield(commandExecutor)) {
            commandExecutor.sendMessage(ShieldConfig.REGION_SHIELD_FAILURE);
            return;
        }

        FactionShield shield = shieldHandler.getShield(commandExecutor);
        long total = (1000 * 60 * 60 * ShieldConfig.SHIELD_RESET_COOLDOWN);
        long elapsed = System.currentTimeMillis() - shield.getSelectionTime();
        long difference = total - elapsed;

        if(difference > 0) {
            String timeRemaining = String.format("%dh %dm %ds",
                    TimeUnit.MILLISECONDS.toHours(difference) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(difference)),
                    TimeUnit.MILLISECONDS.toMinutes(difference) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference)),
                    TimeUnit.MILLISECONDS.toSeconds(difference) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(difference))
            );
            commandExecutor.sendMessage(ShieldConfig.SHIELD_RESET_FAIL.replaceAll("%time%", timeRemaining));
            return;
        }

        if(nearbyEnemyPlayers(shield)) {
            commandExecutor.sendMessage(ShieldConfig.SHIELD_RESET_PREVENT);
            return;
        }

        commandExecutor.sendMessage(ShieldConfig.SHIELD_RESET_SUCCESS);
        shield.destroy();
    }

    /**
     * Force reset's a players faction shield, ignores the required 24 hour cooldown.
     *
     * @param commandSender the player attempting to force reset someone's shield.
     * @param target the faction whose shield is being reset.
     */
    public void forceReset(Player commandSender, Faction target) {
        if(target == null) {
            return;
        }

        FactionShield shield;
        if((shield = getShield(target)) == null) {
            if(commandSender != null) {
                commandSender.sendMessage(ShieldConfig.SHIELD_LOOKUP_FAILED);
            }
            return;
        }

        if(commandSender != null) {
            commandSender.sendMessage(ShieldConfig.SHIELD_FORCE_RESET.replaceAll("%faction%", shield.getFaction().getTag()));
        }

        shield.destroy();
    }

    /**
     * Reset's ALL faction shields on the server.
     *
     * @param commandSender the player attempting to reset all shields.
     */
    public void resetAll(Player commandSender) {
        if(commandSender != null) {
            final UUID uuid = commandSender.getUniqueId();
            if(resetPending.containsKey(uuid)) {
                long elapsed = (System.currentTimeMillis() - resetPending.get(uuid)) / 1000;
                if(elapsed <= 10) {
                    for(int i = factionShields.size() - 1; i >= 0; i--) {
                        FactionShield shield = factionShields.get(i);
                        shield.destroy();
                    }
                    resetPending.remove(uuid);
                    commandSender.sendMessage(ShieldConfig.SHIELD_RESET_ALL);
                    return;
                }
            }
            resetPending.put(uuid, System.currentTimeMillis());
            commandSender.sendMessage("§cAre you sure you want to reset ALL shields?");
            commandSender.sendMessage("§cRetype the command within 10 seconds to confirm.");
        }
    }

    /**
     * Checks if there are any enemy players near the specified faction shield.
     *
     * @param shield the shield whose shield region is being checked.
     * @return whether or not enemy players are nearby.
     */
    private boolean nearbyEnemyPlayers(FactionShield shield) {
        // The location object at index 0 is the center.
        FLocation chunk = shield.getRegion().get(0);

        // Create a location object using an arbitrary y-axis.
        Location region = new Location(chunk.getWorld(), chunk.getX(), 100, chunk.getZ());

        final int radius = ShieldConfig.SHIELD_RESET_RADIUS;
        final List<Player> online = shield.getFaction().getOnlinePlayers();

        // Loop through nearby players. Are any of them NOT in the faction?
        for(Entity entity : chunk.getWorld().getNearbyEntities(region, radius, radius, radius)) {
            if(entity instanceof Player && !online.contains(entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the specified player has admin permission.
     *
     * @param player the player whose permission is being checked.
     * @return whether or not the player has admin permission.
     */
    public boolean hasAdminPermission(Player player) {
        return player.isOp() || player.hasPermission("factionshields.admin");
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
