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
    private final Type keyListType = new TypeToken<List<main.KeyEntry>>() {}.getType();

    private List<main.KeyEntry> keyEntries = new ArrayList<>();
    public static final Set<String> usedKeys = new HashSet<>();

    private synchronized void loadKeys() {
        if (!main.keysFile.exists()) {
            saveKeys();
            return;
        }

        try (FileReader reader = new FileReader(main.keysFile)) {
            keyEntries = gson.fromJson(reader, keyListType);
            if (keyEntries == null) {
                keyEntries = new ArrayList<>();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load file keys.json: " + e.getMessage());
            keyEntries = new ArrayList<>();
        }
    }
    private synchronized void saveKeys() {
        try (FileWriter writer = new FileWriter(main.keysFile)) {
            gson.toJson(keyEntries, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save file keys.json: " + e.getMessage());
        }
    }
}
