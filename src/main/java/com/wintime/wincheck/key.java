package com.wintime.wincheck;

import com.google.gson.Gson;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class key implements CommandExecutor, Listener {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда только для игроков!");
            return true;
        }
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        // Если игрок уже авторизован
        if (main.authorizedPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.GREEN + "✅ Ваш IP уже авторизован!");
            return true;
        }

        // Проверяем, ожидает ли игрок авторизации
        if (!main.pendingAuth.containsKey(playerId)) {
            player.sendMessage(ChatColor.RED + "❌ Сначала пройдите проверку!");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "❌ Использование: /ключ [ваш_ключ]");
            return true;
        }

        String inputKey = args[0];
        String playerIp = main.pendingAuth.get(playerId);

        // Проверяем был ли ключ уже использован
        if (reload.usedKeys.contains(inputKey)) {
            player.sendMessage(ChatColor.RED + "❌ Этот ключ уже был использован!");
            return true;
        }

        // Проверяем ключ
        boolean isValid = false;
        for (main.KeyEntry entry : main.keyEntries) {
            if (entry.ip.equals(playerIp) && entry.ip.equals(inputKey)) {
                isValid = true;
                break;
            }
        }

        if (isValid) {
            // Помечаем ключ как использованный
            reload.usedKeys.add(inputKey);

            // Удаляем ключ из системы
            removeKey(inputKey);

            // Разрешаем доступ
            main.authorizedPlayers.add(playerId);
            main.frozenPlayers.remove(playerId);
            main.pendingAuth.remove(playerId);

            player.sendTitle(
                    ChatColor.GREEN + "✅ ДОСТУП РАЗРЕШЁН",
                    ChatColor.GOLD + "Добро пожаловать на сервер!",
                    10, 70, 20
            );

            // Возвращаем игрока
            Location originalLoc = main.originalLocations.get(playerId);
            if (originalLoc != null) {
                player.teleport(originalLoc);
            }

            player.sendMessage(ChatColor.DARK_GREEN + "-------------------------------");
            player.sendMessage(ChatColor.GREEN + "✅ Авторизация успешна!");
            player.sendMessage(ChatColor.WHITE + "Теперь вы можете играть на сервере");
            player.sendMessage(ChatColor.DARK_GREEN + "-------------------------------");
        } else {
            player.sendMessage(ChatColor.RED + "❌ Неверный ключ! Проверьте клиент.");
        }

        return true;
    }
    private final Gson gson = new Gson();
    public synchronized void removeKey(String key) {
        main.keyEntries.removeIf(entry -> entry.key.equals(key));
        saveKeys();
    }
    public synchronized void saveKeys() {
        try (FileWriter writer = new FileWriter(main.keysFile)) {
            gson.toJson(main.keyEntries, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save file keys.json: " + e.getMessage());
        }
    }
    // Удаление использованного ключа
}
