package com.redrixone;

import com.redrixone.commands.DuelCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class Duels extends Plugin {

    public void onEnable() {
        System.out.println("[DuelsCore] Enabled!");
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new DuelCommand(this));
    }

}
