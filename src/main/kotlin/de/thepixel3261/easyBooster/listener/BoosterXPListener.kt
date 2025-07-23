package de.thepixel3261.easyBooster.listener

import de.thepixel3261.easyBooster.Main
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerExpChangeEvent
import java.util.*

class BoosterXPListener private constructor(val plugin: Main) : Listener {
    companion object {
        @Volatile
        private var INSTANCE: BoosterXPListener? = null

        fun getInstance(plugin: Main): BoosterXPListener {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BoosterXPListener(plugin).also { INSTANCE = it }
            }
        }
    }

    private val xpMultipliers: MutableMap<UUID, Double> = HashMap()

    fun setXpMultiplier(player: Player, multiplier: Double) {
        xpMultipliers[player.uniqueId] = multiplier
    }

    fun removeXpMultiplier(player: Player) {
        xpMultipliers.remove(player.uniqueId)
    }

    fun getXpMultiplier(player: Player): Double {
        return xpMultipliers.getOrDefault(player.uniqueId, 1.0)
    }

    @EventHandler
    fun onPlayerExpChange(event: PlayerExpChangeEvent) {
        val player = event.player
        val multiplier = xpMultipliers.getOrDefault(player.uniqueId, 1.0)

        if (multiplier > 1.0) {
            val originalXp = event.amount
            val boostedXp = Math.round(originalXp * multiplier).toInt()
            event.amount = boostedXp
        }
    }
}
