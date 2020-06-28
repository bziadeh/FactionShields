package com.cloth.shield;

import com.cloth.FactionShieldsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Created by Brennan on 6/4/2020.
 */
public class FactionEventCmd implements Listener {

    public FactionEventCmd() {
        FactionShieldsPlugin.getInstance().registerListener(this);
    }

    @EventHandler
    public void onFactionShieldCmd(PlayerCommandPreprocessEvent event) {
        final String[] args = event.getMessage().split(" ");
        if(args.length >= 2) {
            if(args[0].equalsIgnoreCase("/f")) {
                if(args[1].equalsIgnoreCase("shield")) {
                    event.setCancelled(true);
                    if(args.length == 2) {
                        event.getPlayer().chat("/fs");
                    } else if(args.length > 2) {
                        event.getPlayer().chat("/fs " + args[2]);
                    }
                } else if (args[1].equalsIgnoreCase("setregion")) {
                    event.setCancelled(true);
                    event.getPlayer().chat("/fs setregion");
                }
            }
        }
    }
}
