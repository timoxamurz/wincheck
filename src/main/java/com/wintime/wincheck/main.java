package com.wintime.wincheck;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@SuppressWarnings("ResultOfMethodCallIgnored")
public final class main extends JavaPlugin implements Listener{
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new reload(), this);
        getServer().getPluginManager().registerEvents(new key(), this);
        getCommand("wincheckreload").setExecutor(new reload());
        getCommand("key").setExecutor(new key());
        getDataFolder().mkdirs();
        // Файл для хранения ключей
        keysFile = new File(getDataFolder(), "keys.json");

        // Загружаем ключи
        loadKeys();

        // Настраиваем изолированную зону
        setupWaitingWorld();

        // Регистрируем обработчики
        getServer().getPluginManager().registerEvents(this, this);

        // Запускаем таймер для автообновления ключей
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::checkKeysUpdate, 20 * 5, 20 * 5);

        getLogger().warning("WinCheck was successfully enabled and it is ready to check keys!");
    }
    // Для работы с JSON
    private final Gson gson = new Gson();
    private final Type keyListType = new TypeToken<List<KeyEntry>>() {}.getType();

    // Хранилище ключей и сессий
    public static List<KeyEntry> keyEntries = new ArrayList<>();
    public static final Map<UUID, String> pendingAuth = new ConcurrentHashMap<>();
    public static File keysFile;
    private long lastFileCheck = 0;

    // Для изолированной зоны
    private Location waitingLocation;
    public static final Map<UUID, Location> originalLocations = new HashMap<>();
    public static final Set<UUID> frozenPlayers = new HashSet<>();
    public static final Set<UUID> authorizedPlayers = new HashSet<>();

    private void setupWaitingWorld() {
        World world = Bukkit.getWorlds().get(0);
        waitingLocation = new Location(world, 1000, 200, 1000);

        // Создаем платформу (асинхронно)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        world.getBlockAt(waitingLocation.clone().add(x, -1, z)).setType(Material.BEDROCK);
                    }
                }

                // Очищаем область вокруг
                for (int x = -10; x <= 10; x++) {
                    for (int y = 60; y < 70; y++) {
                        for (int z = -10; z <= 10; z++) {
                            Location loc = waitingLocation.clone().add(x, y - 65, z);
                            if (!loc.getBlock().isEmpty()) {
                                loc.getBlock().setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }.runTask(this);
    }


    public static class KeyEntry {
        public String ip;
        public String key;
    }

    public void checkKeysUpdate() {
        long lastModified = keysFile.lastModified();
        if (lastModified > lastFileCheck) {
            loadKeys();
            lastFileCheck = lastModified;
        }
    }

    // Загрузка ключей из JSON
    public synchronized void loadKeys() {
        if (!keysFile.exists()) {
            saveKeys();
            return;
        }

        try (FileReader reader = new FileReader(keysFile)) {
            keyEntries = gson.fromJson(reader, keyListType);
            if (keyEntries == null) {
                keyEntries = new ArrayList<>();
            }
        } catch (Exception e) {
            getLogger().warning("Failed to load file keys.json: " + e.getMessage());
            keyEntries = new ArrayList<>();
        }
    }

    // Сохранение ключей в JSON
    public synchronized void saveKeys() {
        try (FileWriter writer = new FileWriter(keysFile)) {
            gson.toJson(keyEntries, writer);
        } catch (Exception e) {
            getLogger().warning("Failed to save file keys.json: " + e.getMessage());
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
        for (KeyEntry entry : keyEntries) {
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

    // Обработчик выхода игрока
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        frozenPlayers.remove(playerId);
        pendingAuth.remove(playerId);
        originalLocations.remove(playerId);
    }

    // Запрещаем движение замороженным игрокам
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (frozenPlayers.contains(player.getUniqueId())) {
            // Отменяем движение
            event.setTo(waitingLocation);
        }
    }

    @Override
    public void onDisable() {
// Plugin shutdown logic
    }
}
