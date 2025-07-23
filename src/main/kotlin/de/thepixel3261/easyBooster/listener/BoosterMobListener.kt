package de.thepixel3261.easyBooster.listener

import de.thepixel3261.easyBooster.Main
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.*

class BoosterMobListener private constructor(val plugin: Main) : Listener {
    companion object {
        @Volatile
        private var INSTANCE: BoosterMobListener? = null

        fun getInstance(plugin: Main): BoosterMobListener {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BoosterMobListener(plugin).also { INSTANCE = it }
            }
        }
    }

    private val mobMultipliers: MutableMap<UUID, Double> = HashMap()

    fun setMobMultiplier(player: Player, multiplier: Double) {
        mobMultipliers[player.uniqueId] = multiplier
    }

    fun removeMobMultiplier(player: Player) {
        mobMultipliers.remove(player.uniqueId)
    }

    fun getMobMultiplier(player: Player): Double {
        return mobMultipliers.getOrDefault(player.uniqueId, 1.0)
    }

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) return

        val entity = event.entity as? LivingEntity ?: return
        val world = entity.world
        
        // Get nearby players with mob boosters
        val nearbyPlayers = world.getNearbyEntities(
            entity.location, 
            16.0, // 16 block radius
            16.0, 
            16.0
        ).filterIsInstance<Player>()
        
        val highestMultiplier = nearbyPlayers
            .mapNotNull { player -> mobMultipliers[player.uniqueId] }
            .maxOrNull() ?: return
            
        if (Math.random() < (highestMultiplier - 1.0)) {
            // Spawn additional mobs
            val count = (Math.random() * (highestMultiplier - 1.0) + 1).toInt()
            repeat(count) {
                world.spawnEntity(entity.location, entity.type)
            }
        }
    }
}
