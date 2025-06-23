package de.thepixel3261.easyBooster.util

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.manager.StorageManager
import org.bukkit.entity.Player

class Parse(private val plugin: Main) {
    fun parse(player: Player, text: String): String {
        val playerName = player.name
        val playerUUID = player.uniqueId.toString()
        val boosters: Map<String, Int> = StorageManager(plugin).getAllBoosters(player)
        text.replace("%player_name%", playerName)
            .replace("%player_uuid%", playerUUID)

        plugin.config.getStringList("boosters").forEach {
            if (boosters.containsKey(it)) {
                text.replace("%$it-amount%", boosters[it].toString())
            } else {
                text.replace("%$it-amount%", "0")
            }

            text.replace("%$it-displayname%", plugin.config.getString("boosters.$it.displayname", "").toString())
        }
        return text
    }
}