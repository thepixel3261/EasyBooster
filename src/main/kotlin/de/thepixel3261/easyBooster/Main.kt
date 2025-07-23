package de.thepixel3261.easyBooster

import de.thepixel3261.easyBooster.command.BoosterCommand
import de.thepixel3261.easyBooster.gui.BoosterGUIListener
import de.thepixel3261.easyBooster.listener.BoosterMobListener
import de.thepixel3261.easyBooster.listener.BoosterXPListener
import de.thepixel3261.easyBooster.listener.PlayerListener
import de.thepixel3261.easyBooster.manager.BoosterManager
import de.thepixel3261.easyBooster.manager.ItemManager
import de.thepixel3261.easyBooster.manager.StorageManager
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    lateinit var boosterManager: BoosterManager
    lateinit var boosterXPListener: BoosterXPListener
    lateinit var boosterMobListener: BoosterMobListener

    override fun onEnable() {
        //register commands + tabcompleter
        getCommand("booster")?.setExecutor(BoosterCommand(this))
        getCommand("booster")?.tabCompleter = BoosterCommand(this)

        //initialize
        StorageManager(this)
        boosterManager = BoosterManager.getInstance(this)
        boosterXPListener = BoosterXPListener.getInstance(this)
        boosterMobListener = BoosterMobListener.getInstance(this)
        saveDefaultConfig()

        //register listeners
        server.pluginManager.registerEvents(PlayerListener(this), this)
        server.pluginManager.registerEvents(BoosterGUIListener(this), this)
        server.pluginManager.registerEvents(ItemManager(this), this)
        server.pluginManager.registerEvents(boosterXPListener, this)
        server.pluginManager.registerEvents(boosterMobListener, this)
    }

    override fun onDisable() {
        if (::boosterManager.isInitialized) {
            boosterManager.removeAllBoosters()
        }
        StorageManager(this).closeConnection()
    }
}
