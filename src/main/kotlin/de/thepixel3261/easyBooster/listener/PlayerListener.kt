package de.thepixel3261.easyBooster.listener

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.manager.BoosterManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerListener(plugin: Main) : Listener {
    val plugin: Main = plugin
    @EventHandler
    fun onPlayerJoin(event: PlayerQuitEvent) {
        BoosterManager.getInstance(plugin).removeAllBoosters(event.player)
    }
}