package de.thepixel3261.easyBooster.util

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.manager.BoosterManager
import de.thepixel3261.easyBooster.manager.StorageManager
import org.bukkit.entity.Player

class Parse(private val plugin: Main) {
    fun parse(player: Player, text: String, boosterName: String): String {
        val playerName = player.name
        val playerUUID = player.uniqueId.toString()
        var text = text
        var parsedText = text.replace("%player_name%", playerName)
            .replace("%player_uuid%", playerUUID)

        var boosterDisplayName = ""
        if (text.contains("%booster-display-name%")) {
            boosterDisplayName = BoosterManager.getInstance(plugin).getBoosterDisplayName(boosterName, player)
        }

        val boosterAmount = StorageManager(plugin).getBoosterAmount(player, boosterName)
        val boosterTimeLeft = BoosterManager.getInstance(plugin).getBoosterTimeLeft(player, boosterName)
        val boosterDescription = plugin.config.getString("boosters.$boosterName.description", "")!!
        var boosterEffects = plugin.config.getStringList("boosters.$boosterName.effect")
        if (plugin.config.getStringList("boosters.$boosterName.effectTranslation").isNotEmpty()) {
            boosterEffects = plugin.config.getStringList("boosters.$boosterName.effectTranslation")
        }
        val effectString = boosterEffects.joinToString(", ")

        var parsedText2 = parsedText.replace("%amount-left%", boosterAmount.toString())
            .replace("%booster-name%", boosterName)
            .replace("%booster-display-name%", boosterDisplayName)
            .replace("%time-left%", parseTime(boosterTimeLeft))
            .replace("%description%", boosterDescription)
            .replace("%booster-effects%", effectString)
            .replace("%booster-duration%", plugin.config.getInt("boosters.$boosterName.duration", 60).toString())

        return parsedText2
    }

    fun parse(text: String, player: Player): String {
        val playerName = player.name
        val playerUUID = player.uniqueId.toString()
        var text = text
        var parsedText = text.replace("%player_name%", playerName)
            .replace("%player_uuid%", playerUUID)
        return parsedText
    }

    fun parseTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val seconds = seconds % 60

        var secondsString = seconds.toString()
        if (seconds < 10L) {
            secondsString = "0$seconds"
        }

        if (hours != 0L) {
            return "$hours:$minutes:$secondsString"
        } else {
            return "$minutes:$secondsString"
        }
    }

    fun getFormattedPrefix(): String {
        return plugin.config.getString("prefix", "[EasyBooster] ")!! + "§f"
    }
}