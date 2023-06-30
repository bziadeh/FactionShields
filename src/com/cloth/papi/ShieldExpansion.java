package com.cloth.papi;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import com.cloth.shield.FactionShield;
import com.cloth.shield.ShieldHandler;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Created by Brennan on 7/1/2020.
 */
public class ShieldExpansion extends PlaceholderExpansion {

    private final FactionShieldsPlugin plugin;

    public ShieldExpansion(FactionShieldsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "factionshields";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        final ShieldHandler shieldHandler = FactionShieldsPlugin.getInstance().getShieldHandler();
        final String tag = ChatColor.stripColor(params);

        if(p == null) {
            return "";
        }

        Faction faction;
        if((faction = Factions.getInstance().getByTag(tag)) == null) {
            return "";
        }

        FactionShield shield;
        if((shield = shieldHandler.getShield(faction)) == null) {
            return ShieldConfig.PLACEHOLDER_SHIELD_NONE;
        }

        final String time = shield.getItemData().getTime();
        final String[] sections = time.split(" ");

        if(!shield.isEnabled()) {
            return ShieldConfig.PLACEHOLDER_SHIELD_INACTIVE.replaceAll("%time%", time);
        }

        return ShieldConfig.PLACEHOLDER_SHIELD_ACTIVE.replaceAll("%end%",
                shieldHandler.getEndTime(sections[1], Integer.parseInt(sections[0])));
    }
}
