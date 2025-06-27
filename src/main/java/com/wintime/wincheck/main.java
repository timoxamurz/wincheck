package com.wintime.wincheck;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
@SuppressWarnings("ResultOfMethodCallIgnored")
public final class main extends JavaPlugin implements Listener {
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new reload(), this);
        getCommand("wincheckreload").setExecutor(new reload());
        getDataFolder().mkdirs();
        // Файл для хранения ключей
        ipsFile = new File(getDataFolder(), "ips.json");

        // Загружаем ключи
        loadIPs();

        // Регистрируем обработчики
        getServer().getPluginManager().registerEvents(this, this);

        // Запускаем таймер для автообновления айпи
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::checkIPsUpdate, 20 * 5, 20 * 5);

        getLogger().warning("WinCheck was successfully enabled and it is ready to check ips!");
    }

    // Для работы с JSON
    private final Gson gson = new Gson();
    private final Type ipListType = new TypeToken<List<IPEntry>>() {
    }.getType();

    // Хранилище ключей и сессий
    public static List<IPEntry> ipEntries = new ArrayList<>();
    public static File ipsFile;
    private long lastFileCheck = 0;

    // Для подтверждения входа
    private final Set<UUID> authorizedPlayers = new HashSet<>();

    public static class IPEntry {
        public String ip;
    }

    public void checkIPsUpdate() {
        long lastModified = ipsFile.lastModified();
        if (lastModified > lastFileCheck) {
            loadIPs();
            lastFileCheck = lastModified;
        }
    }

    // Загрузка IP из JSON
    public synchronized void loadIPs() {
        if (!ipsFile.exists()) {
            saveIPs();
            return;
        }

        try (FileReader reader = new FileReader(ipsFile)) {
            ipEntries = gson.fromJson(reader, ipListType);
            if (ipEntries == null) {
                ipEntries = new ArrayList<>();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load file ips.json: " + e.getMessage());
            ipEntries = new ArrayList<>();
        }
    }

    // Сохранение IP в JSON
    public synchronized void saveIPs() {
        try (FileWriter writer = new FileWriter(ipsFile)) {
            gson.toJson(ipEntries, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save file ips.json: " + e.getMessage());
        }
    }

    // Обработчик входа игрока
    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String ipAddress = Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress();

        // Проверяем авторизацию
        if (authorizedPlayers.contains(playerId)) {
            player.sendMessage(ChatColor.GREEN + "✅ Ваш IP уже авторизован!");
            return;
        }

        // Проверяем авторизацию IP
        boolean isAuthorized = false;
        for (IPEntry entry : ipEntries) {
            if (entry.ip.equals(ipAddress)) {
                isAuthorized = true;
                break;
            }
        }

        if (isAuthorized) {
            authorizedPlayers.add(playerId);
            player.sendMessage(ChatColor.GREEN + "✅ Ваш IP уже авторизован!");
        } else {
            // Запоминаем позицию входа
            player.kickPlayer("Не удалось зайти на сервер! Подтвердите свой айпи, установив WinCheck на нашем сайте (https://mcwintime.ru, свехру выберите WinCheck). Данная программа совершенно безопасная - можете посмотреть на VirusTotal");
        }
    }

}
