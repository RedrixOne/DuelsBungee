package com.redrixone.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DuelCommand extends Command {

    private final Plugin plugin;
    private final Map<ProxiedPlayer, ProxiedPlayer> duelRequests;
    private final Map<ProxiedPlayer, ScheduledTask> duelTimers;

    private static final String COMMAND_NAME = "duel";

    public DuelCommand(Plugin plugin) {
        super(COMMAND_NAME);
        this.plugin = plugin;
        duelRequests = new HashMap<>();
        duelTimers = new HashMap<>();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "Questo comando può essere eseguito solo da un giocatore.");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Correct usage: /duel <username> <mode> or /duel accept <username>");
            return;
        }

        ProxiedPlayer target = plugin.getProxy().getPlayer(args[0]);

        if (args[0].equalsIgnoreCase("accept")) {
            String requesterName = args[1];
            ProxiedPlayer requester = plugin.getProxy().getPlayer(requesterName);

            if (requester == null || !requester.isConnected()) {
                player.sendMessage(ChatColor.RED + "The player who requested the duel is no longer online.");
                return;
            }

            if (!duelRequests.containsKey(requester) || !duelRequests.get(requester).equals(player)) {
                player.sendMessage(ChatColor.RED + "You don't have a duel request from the specified player.");
                return;
            }

            ServerInfo duelServer = plugin.getProxy().getServerInfo("tb-test");

            if (duelServer == null) {
                player.sendMessage(ChatColor.RED + "There's no duel arena available at the moment.");
                return;
            }

            duelRequests.remove(requester);
            duelTimers.remove(requester);

            player.connect(duelServer);
            requester.connect(duelServer);

            player.sendMessage(ChatColor.GREEN + "You have accepted " + requester.getName() + "'s duel.");
            requester.sendMessage(ChatColor.GREEN + player.getName() + " has accepted your duel.");
        }

        if (target == null || !target.isConnected()) {
            player.sendMessage(ChatColor.RED + "Player not found or offline.");
            return;
        }

        String mode = args[1];

        if (duelRequests.containsKey(player) || duelRequests.containsValue(player)) {
            player.sendMessage(ChatColor.RED + "You already have a duel in progress.");
            return;
        }

        if (duelTimers.containsKey(player) || duelTimers.containsValue(player)) {
            player.sendMessage(ChatColor.RED + "You need to wait before sending another duel request.");
            return;
        }

        duelRequests.put(player, target);

        ScheduledTask task = plugin.getProxy().getScheduler().schedule(plugin, () -> {
            duelRequests.remove(player);
            duelTimers.remove(player);

            player.sendMessage(ChatColor.RED + "Your duel with " + target.getName() + " has expired.");
            target.sendMessage(ChatColor.RED + "The duel with " + player.getName() + " has expired.");
        }, 60, TimeUnit.SECONDS);

        duelTimers.put(player, task);

        String acceptCommand = "/duel accept " + player.getName();
        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&bYou have a duel request from &3" + player.getName() + " &bin &3" + mode.toUpperCase() + "&b! \n&bYou have 60 seconds to accept.\n&3Click &bhere &3to accept the duel request"));
        TextComponent acceptText = new TextComponent(ChatColor.translateAlternateColorCodes('&', "&3Click &bhere &3to accept"));

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[]{acceptText});
        message.setHoverEvent(hoverEvent);

        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, acceptCommand);
        message.setClickEvent(clickEvent);

        target.sendMessage(message);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&3You sent a duel request to &b" + target.getName() + " &3in &b" + mode.toUpperCase()));
    }

}
