/*
* Copyright (C) 2014 Mazen K.
* This file is part of MCNotifier.
*
* MCNotifier for Bukkit is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 to be exact
*
* MCNotifier is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with MCNotifier. If not, see <http://www.gnu.org/licenses/>.
*/

package io.mazenmc.notifier;

import io.mazenmc.notifier.client.NotifierClient;
import io.mazenmc.notifier.event.NotifierEventHandler;
import io.mazenmc.notifier.event.NotifierListener;
import io.mazenmc.notifier.packets.Packet;
import io.mazenmc.notifier.packets.PacketServerShutdown;
import io.mazenmc.notifier.server.NotifierServer;
import io.mazenmc.notifier.util.ClassFinder;
import io.mazenmc.notifier.util.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.logging.Level;

public class NotifierPlugin extends JavaPlugin {

    /* Object instances */
    private static NotifierPlugin plugin;
    private static SettingsManager settingsManager;
    private static NotifierEventHandler notifierEventHandler;
    private static ClassFinder classFinder;
    private NotifierServer server;

    @Override
    public void onEnable() {
        //Define instances
        plugin = this;
        saveDefaultConfig();
        settingsManager = new SettingsManager();
        notifierEventHandler = new NotifierEventHandler();
        classFinder = new ClassFinder();

        //Register packet classes
        Packet.registerAll();

        //Register Notifier plugin classes
        try{
            registerListeners();
        }catch(Exception ex) {
            getLogger().log(Level.SEVERE, "Unable to register listeners, disabling plugin..", ex);
            Bukkit.getPluginManager().disablePlugin(this);
        }

        //Start the NotifierServer
        try {
            server = new NotifierServer();
            server.start();
        }catch(IOException ex) {
            getLogger().log(Level.SEVERE, "Unable to start NotifierServer!", ex);
            getLogger().info("Disabling plugin..");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        //Notify clients of server shutdown
        for(NotifierClient client : NotifierClient.getClients()) {
            client.write(new PacketServerShutdown(null));
        }

        //Shut down the NotifierServer in-case of reloading
        try{
            server.close();
        }catch(Exception ex) {
            ex.printStackTrace();
            log("Unable to shutdown the NotifierServer, shutting down..");
            Bukkit.shutdown();
        }

        //Avoiding any memory leaks..
        plugin = null;
        settingsManager = null;
        notifierEventHandler = null;
        classFinder = null;
    }

    /**
     * Gets the plugin instance
     * @return Plugin instance
     */
    public static NotifierPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the SettingsManagers instance
     * @return SettingManager instance
     */
    public static SettingsManager getSettingsManager() {
        return settingsManager;
    }

    /**
     * Gets the NotifierEventHandler instance
     * @return NotifierEventHandler instance
     */
    public static NotifierEventHandler getEventHandler() {
        return notifierEventHandler;
    }

    /**
     * Gets the ClassFinders instance
     * @return ClassFinder instance
     */
    public static ClassFinder getClassFinder() {
        return classFinder;
    }

    /**
     * Register all Notifier's listeners
     * @throws Exception
     */
    private void registerListeners() throws Exception{
        for(Class<?> cls : getClassFinder().find("io.mazenmc.notifier.listeners.bukkit")) {
            if(cls.isAssignableFrom(Listener.class)) {
                getServer().getPluginManager().registerEvents(cls.asSubclass(Listener.class).getConstructor().newInstance(), plugin);
            }
        }

        for(Class<?> cls : getClassFinder().find("io.mazenmc.notifier.listeners.notifier")) {
            if(cls.isAssignableFrom(NotifierListener.class)) {
                getEventHandler().registerListener(cls.asSubclass(NotifierListener.class).getConstructor().newInstance());
            }
        }
    }

    public static void log(String message) {
        getPlugin().getLogger().info(message);
    }
}