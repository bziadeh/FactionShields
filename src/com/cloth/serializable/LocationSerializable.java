package com.cloth.serializable;


import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.Serializable;

/**
 * Created by Brennan on 4/24/2020.
 */
public class LocationSerializable implements Serializable {

    private transient Location location;

    private String world;
    private double x;
    private double y;
    private double z;

    public LocationSerializable(Location location) {
        this.world = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
    }

    public Location getLocation() {
        if(location == null) {
            location = new Location(Bukkit.getWorld(world), x, y, z);
        }
        return location;
    }
}
