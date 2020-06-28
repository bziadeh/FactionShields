package com.cloth;

import com.cloth.config.ShieldConfig;

/**
 * Created by Brennan on 6/3/2020.
 */
public class ShieldBackups extends Thread {


    // How often are the collectors saved to JSON.
    private static final int BACKUP_INTERVAL = 1000 * 60 * ShieldConfig.SHIELD_BACKUP_INTERVAL;

    @Override
    public void run() {
        while(true) {
            try {
                Thread.sleep(BACKUP_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            FactionShieldsPlugin.getInstance().getShieldHandler().backup();
        }
    }
}
