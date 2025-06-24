package de.thepixel3261.easyBooster.manager;

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.util.Parse
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable


class BoosterManager private constructor(val plugin: Main) {

    companion object {
        @Volatile
        private var INSTANCE: BoosterManager? = null

        fun getInstance(plugin: Main): BoosterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BoosterManager(plugin).also { INSTANCE = it }
            }
        }
    }

    var activeBoosters: Map<Player, List<String>> = mapOf()
    var boosterStart = mapOf<Player, Map<String, Long>>()

    fun applyBooster(player: Player, boosterName: String) {
        if (StorageManager(plugin).getBoosterAmount(player, boosterName) <= 0) {
            player.sendMessage("§cYou don't have any ${getBoosterDisplayName(boosterName, player)} left!")
            return
        }

        if (activeBoosters.containsKey(player)) {
            if (activeBoosters[player]!!.contains(boosterName)) {
                player.sendMessage("§cYou already have this booster applied!")
                return
            }
        }

        player.sendMessage(
            "§aYou applied the booster §e${getBoosterDisplayName(boosterName, player)}§a!"
        )
        StorageManager(plugin).takeBooster(player, boosterName, 1)
        val boosterDuration = plugin.getConfig().getInt("boosters.$boosterName.duration", 60)

        activeBoosters = if (activeBoosters.containsKey(player)) {
            if (!activeBoosters[player]!!.contains(boosterName)) {
                activeBoosters + (player to (activeBoosters[player]!! + boosterName))
            } else {
                activeBoosters
            }
        } else {
            activeBoosters + (player to listOf(boosterName))
        }

        if (boosterStart.containsKey(player)) {
            boosterStart = boosterStart + (player to (boosterStart[player]!! + mapOf(boosterName to (System.currentTimeMillis()))))
        } else {
            boosterStart = boosterStart + (player to mapOf(boosterName to (System.currentTimeMillis())))
        }

        plugin.getConfig().getStringList("boosters.$boosterName.effect")?.forEach {
            it.lowercase()
            when (it) {
                "fly" -> {
                    player.allowFlight = true

                }
                "xp" -> {
                    TODO(": Implement XP Booster")
                }
                else -> {
                    try {
                        val potionEffect = it.split(":")
                        val potionName = potionEffect?.get(0)
                        val potionAmplifier = potionEffect?.get(1)?.toInt()!! - 1
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.getByName(potionName!!)!!,
                                boosterDuration * 20,
                                potionAmplifier!!
                            )
                        )
                    } catch (e: Exception) {
                        plugin.logger.severe("Error while applying booster: $boosterName: $e")
                        return
                    }
                }
            }
        }

        removeBoosterLater(player, boosterName, boosterDuration)
    }

    fun removeBoosterLater(player: Player, boosterName: String, duration: Int) {
        object : BukkitRunnable() {
            override fun run() {
                removeBooster(player, boosterName)
            }
        }.runTaskLater(plugin, duration * 20L)
    }

    fun removeAllBoosters(player: Player) {
        if (activeBoosters.containsKey(player)) {
            activeBoosters[player]?.forEach { boosterName ->
                removeBooster(player, boosterName)
            }
        }
    }

    fun removeAllBoosters() {
        activeBoosters.forEach { (player, boosterList) ->
            boosterList.forEach { boosterName ->
                removeBooster(player, boosterName)
            }
        }
    }

    fun removeBooster(player: Player, boosterName: String) {
        player.sendMessage(
            "§cYou removed the booster §e${getBoosterDisplayName(boosterName, player)}§c!"
        )

        activeBoosters = if (activeBoosters.containsKey(player)) {
            if (activeBoosters[player]!!.contains(boosterName)) {
                activeBoosters + (player to (activeBoosters[player]!! - boosterName))
            } else {
                activeBoosters
            }
        } else {
            activeBoosters
        }

        boosterStart = if (boosterStart.containsKey(player)) {
            if (boosterStart[player]!!.containsKey(boosterName)) {
                boosterStart + (player to (boosterStart[player]!!.minus(boosterName)))
            } else {
                boosterStart
            }
        } else {
            boosterStart
        }

        plugin.getConfig().getStringList("boosters.$boosterName.effect").forEach { effect ->
            effect.lowercase()
            when (effect) {
                "fly" -> {
                    if (player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
                        player.allowFlight = false
                        player.isFlying = false
                    }
                }
                "xp" -> {
                    TODO(": Implement XP Booster")
                }
            }
        }
    }

    fun getBoosterTimeLeft(player: Player, boosterName: String): Long {
        if (boosterStart.containsKey(player)) {
            val boosterStart = boosterStart[player]
            if (boosterStart?.containsKey(boosterName) == true) {
                val boosterDuration = plugin.getConfig().getInt("boosters.$boosterName.duration", 60)
                val boosterStartTime = boosterStart[boosterName]
                if (boosterStartTime != null) {
                    val boosterEndTime = boosterStartTime + (boosterDuration * 1000)
                    val currentTime = System.currentTimeMillis()
                    val timeLeft = boosterEndTime - currentTime
                    return (timeLeft / 1000)
                }
            }
        }
        return 0
    }

    fun getBoosterDisplayName(boosterName: String, player: Player): String {
        var boosterDisplayName: String
        if (plugin.config.getString("boosters.$boosterName.displayname", "")!!.isNotEmpty()) {
            boosterDisplayName = plugin.config.getString("boosters.$boosterName.displayname")!!
        } else {
            boosterDisplayName = plugin.config.getString("booster.default.displayname", "")!!
        }

        return Parse(plugin).parse(player, boosterDisplayName, boosterName)
    }

    fun getBoosterLore(boosterName: String, player: Player): List<String> {
        var active = false

        if (activeBoosters.containsKey(player)) {
            active = activeBoosters[player]?.contains(boosterName) == true
        }

        val state = if (active) "on" else "off"

        var boosterLore: List<String>
        if (plugin.config.getString("boosters.$boosterName.displayname", "")!!.isNotEmpty()) {
            boosterLore = plugin.config.getStringList("boosters.$boosterName.lore")
        } else {
            boosterLore = plugin.config.getStringList("booster.default.$state.lore")
        }
        return boosterLore
    }
}