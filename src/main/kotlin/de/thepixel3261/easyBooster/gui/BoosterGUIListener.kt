package de.thepixel3261.easyBooster.gui

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.manager.BoosterManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class BoosterGUIListener(val plugin: Main): Listener {

    @EventHandler
    fun onGUIClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        if (event.clickedInventory?.holder is BoosterGUIHolder) {
            event.isCancelled = true
            val slot = event.slot
            plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                if (plugin.config.getInt("boosters.$it.slot") == slot) {
                    BoosterManager.getInstance(plugin).applyBooster(player, it)
                    player.closeInventory()
                    BoosterGUI(plugin).openBoosterGUI(player)
                }
            }
        }
    }
}