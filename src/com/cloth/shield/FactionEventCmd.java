package com.cloth.shield;

import com.cloth.FactionShieldsPlugin;
import com.cloth.config.ShieldConfig;
import org.bukkit.entity.Player;
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
            final Player player = event.getPlayer();
            if(args[0].equalsIgnoreCase("/f")) {
                if(args[1].equalsIgnoreCase("shield")) {
                    event.setCancelled(true);
                    if(args.length == 2) {
                        // Normal command, open GUI.
                        player.chat("/fs");
                    } else if(args.length > 2) {
                        if(args[2].equalsIgnoreCase("reset") || args[2].equalsIgnoreCase("resetall") || args[2].equalsIgnoreCase("forcereset")) {
                            player.sendMessage("§cTry using: §7/fs " + args[2] + " §cinstead.");
                            return;
                        }
                        // Shield lookup.
                        player.chat("/fs " + args[2]);
                    }
                } else if (args[1].equalsIgnoreCase("setregion")) {
                    // Set the shield region.
                    event.setCancelled(true);
                    player.chat("/fs setregion");
                }
            }
        }
    }
}
