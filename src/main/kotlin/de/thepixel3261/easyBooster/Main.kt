package de.thepixel3261.easyBooster

import de.thepixel3261.easyBooster.command.BoosterCommand
import de.thepixel3261.easyBooster.gui.BoosterGUIListener
import de.thepixel3261.easyBooster.listener.PlayerListener
import de.thepixel3261.easyBooster.manager.BoosterManager
import de.thepixel3261.easyBooster.manager.StorageManager
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {

    lateinit var boosterManager: BoosterManager
        private set

    override fun onEnable() {
        //register commands + tabcompleter
        getCommand("booster")?.setExecutor(BoosterCommand(this))
        getCommand("booster")?.tabCompleter = BoosterCommand(this)

        //register listeners
        server.pluginManager.registerEvents(PlayerListener(this), this)
        server.pluginManager.registerEvents(BoosterGUIListener(this), this)

        //initialize
        StorageManager(this)
        boosterManager = BoosterManager.getInstance(this)
        saveDefaultConfig()
    }

    override fun onDisable() {
        if (::boosterManager.isInitialized) {
            boosterManager.removeAllBoosters()
        }
        StorageManager(this).closeConnection()
    }
}
