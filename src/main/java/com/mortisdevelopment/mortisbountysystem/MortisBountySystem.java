package com.mortisdevelopment.mortisbountysystem;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MortisBountySystem extends JavaPlugin implements Listener{

    private Economy economy;
    private Map<UUID, Double> bounties;
    private String lowAmount;
    private String noFunds;
    private String invalidPlayer;
    private String invalidAmount;
    private String bountyPlacementMessage;
    private String bountyClaimMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        bounties = new HashMap<>();

        getServer().getPluginManager().registerEvents(this, this);
        lowAmount = getConfig().getString("LowAmount");
        noFunds = getConfig().getString("LowFunds");
        invalidAmount = getConfig().getString("InvalidAmount");
        invalidPlayer = getConfig().getString("InvalidPlayer");
        bountyPlacementMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("bounty_placement_message") + "");
        bountyClaimMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("bounty_claim_message") + "");

        if (!setupEconomy()) {
            getLogger().severe("Vault not found or no compatible economy plugin found.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("bounty")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /bounty <player> <amount>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidPlayer));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', invalidAmount));
                return true;
            }

            if (amount <= 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', lowAmount));
                return true;
            }

            Player placer = (Player) sender;
            if (economy.getBalance(placer) >= amount) {
                economy.withdrawPlayer(placer, amount);
                bounties.put(target.getUniqueId(), amount);
                String message = bountyPlacementMessage
                        .replace("%bountyplacer%", placer.getName())
                        .replace("%bountyamount%", String.valueOf(amount))
                        .replace("%bountytarget%", target.getName());
                Bukkit.broadcastMessage(message);

            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', noFunds));
            }

            return true;
        }

        return false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && bounties.containsKey(victim.getUniqueId())) {
            double bountyAmount = bounties.get(victim.getUniqueId());
            bounties.remove(victim.getUniqueId());

            economy.depositPlayer(killer, bountyAmount);
            String message = bountyClaimMessage
                    .replace("%bountyamount%", String.valueOf(bountyAmount))
                    .replace("%bountyvictim%", victim.getName());
            killer.sendMessage(message);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }
}
