package com.wintime.wincheck;

import org.bukkit.event.Listener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class reload implements CommandExecutor, Listener {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("wincheckreload")) {
            if (!sender.hasPermission("wincheck.reload")) {
                sender.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            loadKeys();
            sender.sendMessage(ChatColor.GREEN + "Ключи успешно перезагружены!");
            return true;
        }
        return true;
    }
    private final Gson gson = new Gson();
    private final Type ipListType = new TypeToken<List<main.IPEntry>>() {}.getType();

    private List<main.IPEntry> ipEntries = new ArrayList<>();

    private synchronized void loadKeys() {
        if (!main.ipsFile.exists()) {
            saveKeys();
            return;
        }

        try (FileReader reader = new FileReader(main.ipsFile)) {
            ipEntries = gson.fromJson(reader, ipListType);
            if (ipEntries == null) {
                ipEntries = new ArrayList<>();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load file keys.json: " + e.getMessage());
            ipEntries = new ArrayList<>();
        }
    }
    private synchronized void saveKeys() {
        try (FileWriter writer = new FileWriter(main.ipsFile)) {
            gson.toJson(ipEntries, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save file keys.json: " + e.getMessage());
        }
    }
}
