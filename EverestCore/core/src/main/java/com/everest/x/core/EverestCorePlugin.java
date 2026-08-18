package com.everest.x.core;

import com.everest.x.api.EverestAPI;
import com.everest.x.api.EverestProvider;
import com.everest.x.core.command.EverestCommand;
import com.everest.x.core.config.Messages;
import com.everest.x.core.config.PluginSettings;
import com.everest.x.core.hook.HookRegistry;
import com.everest.x.core.menu.ConfigMenu;
import com.everest.x.core.menu.ConfigMenuListener;
import com.everest.x.core.network.ProxyMessenger;
import com.everest.x.core.spawn.SpawnListener;
import com.everest.x.core.spawn.SpawnService;
import com.everest.x.core.storage.StorageFactory;
import com.everest.x.core.storage.UserRepository;
import com.everest.x.core.user.User;
import com.everest.x.core.user.UserCache;
import com.everest.x.core.user.UserListener;
import com.everest.x.core.util.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class EverestCorePlugin extends JavaPlugin {

    private Scheduler scheduler;
    private PluginSettings settings;
    private Messages messages;
    private UserRepository repository;
    private UserCache users;
    private HookRegistry hooks;
    private ProxyMessenger proxy;
    private SpawnService spawn;
    private ConfigMenu configMenu;
    private EverestProviderImpl provider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        scheduler = new Scheduler(this);
        messages = new Messages(this);
        settings = new PluginSettings(getConfig());
        users = new UserCache(this);
        hooks = new HookRegistry();
        proxy = new ProxyMessenger(this);
        spawn = new SpawnService(this);
        configMenu = new ConfigMenu(this);
        repository = StorageFactory.createOrDisable(this, settings);
        if (repository == null) {
            getLogger().severe("EverestCore desligado: storage indisponível.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        proxy.register();
        provider = new EverestProviderImpl(this);
        EverestAPI.register(provider);
        getServer().getServicesManager().register(EverestProvider.class, provider, this, ServicePriority.Normal);

        getServer().getPluginManager().registerEvents(new UserListener(this), this);
        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new ConfigMenuListener(this), this);
        EverestCommand command = new EverestCommand(this);
        getCommand("everest").setExecutor(command);
        getCommand("everest").setTabCompleter(command);
        getCommand("spawn").setExecutor(command);
        getCommand("spawn").setTabCompleter(command);
        getCommand("setspawn").setExecutor(command);
        getCommand("setspawn").setTabCompleter(command);

        for (Player player : getServer().getOnlinePlayers()) {
            loadOnline(player);
        }

        getLogger().info("Hub pronto · " + settings.serverId() + " (" + settings.serverType() + ")");
    }

    @Override
    public void onDisable() {
        if (users != null) {
            for (User user : users.all()) {
                saveQuiet(user);
            }
            users.clear();
        }
        if (hooks != null) {
            hooks.clear();
        }
        if (provider != null) {
            EverestAPI.unregister(provider);
            getServer().getServicesManager().unregister(EverestProvider.class, provider);
        }
        if (proxy != null) {
            proxy.unregister();
        }
        if (repository != null) {
            repository.close();
        }
    }

    public void reloadCore() {
        reloadConfig();
        settings = new PluginSettings(getConfig());
        messages.reload();
        if (spawn != null) {
            spawn.reload();
        }
        if (configMenu != null) {
            configMenu.reload();
        }
        getLogger().info("Config, mensagens e menus recarregados. Storage não é reaberto no reload.");
    }

    public void persistAsync(User user) {
        scheduler.async(() -> saveQuiet(user));
    }

    private void saveQuiet(User user) {
        try {
            repository.save(user);
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Falha ao salvar " + user.getName(), exception);
        }
    }

    private void loadOnline(Player player) {
        scheduler.async(() -> {
            try {
                User user = repository.loadOrCreate(
                        player.getUniqueId(), player.getName(), settings.startingCoins());
                scheduler.sync(() -> {
                    if (player.isOnline() && isEnabled()) {
                        users.put(user);
                    }
                });
            } catch (Exception exception) {
                getLogger().log(Level.WARNING, "Reload: falha ao carregar " + player.getName(), exception);
            }
        });
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public PluginSettings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public UserRepository repository() {
        return repository;
    }

    public UserCache users() {
        return users;
    }

    public HookRegistry hooks() {
        return hooks;
    }

    public ProxyMessenger proxy() {
        return proxy;
    }

    public SpawnService spawn() {
        return spawn;
    }

    public ConfigMenu configMenu() {
        return configMenu;
    }
}
