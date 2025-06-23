package de.thepixel3261.easyBooster.manager;

import de.thepixel3261.easyBooster.Main
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable


class BoosterManager(val plugin: Main) {
    var activeBoosters: MutableMap<Player, List<String>> = mutableMapOf()

    fun applyBooster(player: Player, boosterName: String) {
        if (StorageManager(plugin).getBoosterAmount(player, boosterName) <= 0) {
            player.sendMessage("§cYou don't have any $boosterName left!")
            return
        }

        player.sendMessage("§aYou applied the booster §e${plugin.getConfig().getString("boosters.$boosterName.displayname", "")}§a!")
        StorageManager(plugin).takeBooster(player, boosterName, 1)
        val boosterDuration = plugin.getConfig().getInt("boosters.$boosterName.duration", 60)

        plugin.getConfig().getStringList("boosters.$boosterName.effect")?.forEach {
            it.lowercase()
            when(it) {
                "fly" -> {
                    player.allowFlight = true
                    if (activeBoosters.containsKey(player)) {
                        activeBoosters[player]?.plus(boosterName)
                    } else {
                        activeBoosters.put(player, listOf(boosterName))
                    }
                }
                "xp" -> {
                    TODO(": Implement XP Booster")
                }
                else -> {
                    try {
                        val potionEffect = it.split(":")
                        val potionName = potionEffect?.get(0)
                        val potionAmplifier = potionEffect?.get(1)?.toInt()!! - 1
                        player.addPotionEffect(PotionEffect(PotionEffectType.getByName(potionName!!)!!, boosterDuration * 20, potionAmplifier!!))
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
        player.sendMessage("§cYou removed the booster §e${plugin.getConfig().getString("boosters.$boosterName.displayname", "")}§c!")
        if (activeBoosters.containsKey(player)) {
            activeBoosters[player]?.minus(boosterName)
        }
        plugin.getConfig().getStringList("boosters.$boosterName.effect").forEach {effect ->
            effect.lowercase()
            when(effect) {
                "fly" -> {
                        player.allowFlight = false
                        player.isFlying = false
                }
                "xp" -> {
                    TODO(": Implement XP Booster")
                }
            }
        }
    }
}
