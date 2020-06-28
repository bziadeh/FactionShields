package com.cloth.shield;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by Brennan on 4/11/2020.
 */
public class ShieldCmd implements CommandExecutor {

    public ShieldCmd() {
        FactionShieldsPlugin.getInstance().getCommand("factionshield").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        final FactionShieldsPlugin plugin = FactionShieldsPlugin.getInstance();
        final ShieldHandler shieldHandler = plugin.getShieldHandler();

        final Player player = (Player) sender;
        final FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);

        // If the user did not specify a faction name.
        if(args.length < 1) {
            if(!hasFaction(fplayer)) {
                player.sendMessage(ShieldConfig.NO_FACTION);
                return false;
            }
            FactionShield shield;
            if((shield = shieldHandler.getShield(player)) != null) {
                player.openInventory(shield.getInventory());
            } else {
                player.openInventory(FactionShieldsPlugin.getInstance().getInventoryHandler().getDefaultInventory());
            }
            return false;
        }

        // Not setting region, must be a faction shield lookup...
        if(!args[0].equalsIgnoreCase("setregion")) {
            final String factionLookup = args[0];
            final Faction faction = Factions.getInstance().getByTag(factionLookup);

            // The specified faction has no shield set.
            if(!shieldHandler.hasShield(faction)) {
                player.sendMessage(ShieldConfig.SHIELD_LOOKUP_FAILED);
                return false;
            }

            final String time = shieldHandler.getShield(faction).getItemData().getTime();
            final String[] timeInfo = time.split(" ");
            final String endTime = plugin.getShieldHandler().getEndTime(timeInfo[1], Integer.parseInt(timeInfo[0]));
            final String timezone = ShieldConfig.TIMEZONE;

            // Send the lookup message + convert placeholders.
            player.sendMessage(ShieldConfig.SHIELD_LOOKUP
                    .replaceAll("%faction%", faction.getTag())
                    .replaceAll("%start%", time)
                    .replaceAll("%end%", endTime)
                    .replaceAll(timezone, "")
                    .replaceAll("%timezone%", timezone));
            return false;
        }

        if(!hasFaction(fplayer)) {
            player.sendMessage(ShieldConfig.NO_FACTION);
            return false;
        }

        final Role role = fplayer.getRole();
        if(role != Role.LEADER && role != Role.COLEADER) {
            player.sendMessage(ShieldConfig.NO_FACTION_PERMISSION);
            return false;
        }

        // The shield activation hours have not been setup yet.
        if(!plugin.getShieldHandler().hasShield(player)) {
            player.sendMessage(ShieldConfig.REGION_SHIELD_FAILURE);
            return false;
        }

        // Player attempted to set region in someone else's land.
        if(fplayer.getRelationToLocation() != Relation.MEMBER) {
            player.sendMessage(ShieldConfig.REGION_SET_FAILURE);
            return false;
        }

        final FactionShield shield = plugin.getShieldHandler().getShield(player);
        // Region already set.
        if(!shield.getRegion().isEmpty()) {
            player.sendMessage(ShieldConfig.REGION_ALREADY_SET);
            return false;
        }

        // Set region.
        shield.setRegion(new FLocation(player.getLocation()));
        player.sendMessage(ShieldConfig.REGION_SET);
        return false;
    }

    private boolean hasFaction(FPlayer player) {
        if(!player.hasFaction() || player.getFaction().isWilderness()) {
            return false;
        }
        return true;
    }
}
