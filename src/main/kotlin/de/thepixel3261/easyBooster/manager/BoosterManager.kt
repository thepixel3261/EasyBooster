package de.thepixel3261.easyBooster.manager

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.listener.BoosterMobListener
import de.thepixel3261.easyBooster.listener.BoosterXPListener
import de.thepixel3261.easyBooster.util.Parse
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandSendEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.eclipse.sisu.Priority


class BoosterManager private constructor(val plugin: Main): Listener {

    companion object {
        @Volatile
        private var INSTANCE: BoosterManager? = null

        fun getInstance(plugin: Main): BoosterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BoosterManager(plugin).also { INSTANCE = it }
            }
        }
    }

    var activeBoosters: Map<Player, List<String>> = mutableMapOf()
    var boosterStart = mapOf<Player, Map<String, Long>>()

    fun applyBooster(player: Player, boosterName: String) {
        if (StorageManager(plugin).getBoosterAmount(player, boosterName) <= 0) {
            player.sendMessage(Parse(plugin).getFormattedPrefix() + "§cDu hast keine ${getBoosterDisplayName(boosterName, player)} §cübrig!")
            return
        }

        if (activeBoosters.containsKey(player)) {
            if (activeBoosters[player]!!.contains(boosterName)) {
                player.sendMessage(Parse(plugin).getFormattedPrefix() + "§cDu hast diesen Booster bereits aktiviert!")
                return
            }
        }

        plugin.config.getStringList("boosters.$boosterName.effect").forEach { effect ->
            val effect = effect.split(":")[0].lowercase()
            if (activeBoosters.contains(player)) {
                activeBoosters[player]!!.forEach { activeBooster ->
                    plugin.config.getStringList("boosters.$activeBooster.effect").forEach { activeEffect ->
                        val activeEffect = activeEffect.split(":")[0].lowercase()
                        if (effect == activeEffect) {
                            player.sendMessage(Parse(plugin).getFormattedPrefix() + "§cDu kannst diesen Booster nicht aktivieren, da du bereits einen Booster mit diesem Effekt aktiviert hast!")
                        }
                    }
                }
            }
        }

        player.sendMessage(Parse(plugin).getFormattedPrefix() +
                "§aDu hast den Booster: §e${getBoosterDisplayName(boosterName, player)}§a aktiviert!")

        player.sendMessage(Parse(plugin).getFormattedPrefix() + "Der Booster wird beim Verlassen des CityBuilds abgebrochen!")

        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F)

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
            boosterStart = (boosterStart + (player to (boosterStart[player]!! + mutableMapOf(boosterName to (System.currentTimeMillis()))))) as MutableMap<Player, Map<String, Long>>
        } else {
            boosterStart = (boosterStart + (player to mapOf(boosterName to (System.currentTimeMillis())))) as MutableMap<Player, Map<String, Long>>
        }

        plugin.getConfig().getStringList("boosters.$boosterName.effect").forEach {
            it.lowercase()
            when (it.split(":")[0]) {
                "fly" -> {
                    player.allowFlight = true
                }
                "xp" -> {
                    val multiplier = it.split(":")[1].toDouble()
                    BoosterXPListener.getInstance(plugin).setXpMultiplier(player, multiplier)
                }
                "mob" -> {
                    val multiplier = it.split(":")[1].toDouble()
                    BoosterMobListener.getInstance(plugin).setMobMultiplier(player, multiplier)
                }
                else -> {
                    try {
                        val potionEffect = it.split(":")
                        val potionName = potionEffect.get(0)
                        val potionAmplifier = potionEffect.get(1)?.toInt()!! - 1
                        player.addPotionEffect(
                            PotionEffect(
                                PotionEffectType.getByName(potionName!!)!!,
                                boosterDuration * 20,
                                potionAmplifier
                            )
                        )
                    } catch (e: Exception) {
                        plugin.logger.severe("Error while applying booster: $boosterName§f: $e")
                        return
                    }
                }
            }
        }

        removeBoosterLater(player, boosterName, boosterDuration)
        sendMessageLater(player, boosterName, boosterDuration)
    }

    fun sendMessageLater(player: Player, boosterName: String, duration: Int) {
        val notifyBefore = plugin.config.getInt("booster.notifyExpire", 0)
        if (notifyBefore > 0) {
            object : BukkitRunnable() {
                override fun run() {
                    player.sendMessage(Parse(plugin).getFormattedPrefix() + "§cDer Booster: §e${getBoosterDisplayName(boosterName, player)} §cwird in §e$notifyBefore §cSekunden ablaufen!")
                    player.playSound(player.location, Sound.BLOCK_STONE_BREAK, 1F, 1F)
                }
            }.runTaskLater(plugin, (duration - notifyBefore) * 20L)
        }
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
        player.sendMessage(Parse(plugin).getFormattedPrefix() +
                "§cDein Booster: ${getBoosterDisplayName(boosterName, player)}§c ist abgelaufen!")
        player.playSound(player.location, Sound.BLOCK_ANVIL_LAND, 1F, 1F)

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
            when (effect.split(":")[0]) {
                "fly" -> {
                    if (player.gameMode != GameMode.CREATIVE && player.gameMode != GameMode.SPECTATOR) {
                        player.allowFlight = false
                        player.isFlying = false
                        val featherFallBack = plugin.config.getInt("booster.featherFallBack", 20)
                        if (featherFallBack != 0) {
                            player.addPotionEffect(
                                PotionEffect(
                                    PotionEffectType.SLOW_FALLING,
                                    featherFallBack * 20,
                                    1
                                )
                            )
                        }
                    }
                }
                "xp" -> {
                    BoosterXPListener.getInstance(plugin).removeXpMultiplier(player)
                }
                "mob" -> {
                    BoosterMobListener.getInstance(plugin).removeMobMultiplier(player)
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
        val boosterDisplayName: String
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

        val boosterLore: List<String>
        if (plugin.config.getString("boosters.$boosterName.lore", "")!!.isNotEmpty()) {
            boosterLore = plugin.config.getStringList("boosters.$boosterName.lore")
        } else {
            boosterLore = plugin.config.getStringList("booster.default.$state.lore")
        }
        return boosterLore
    }

    fun reapplyBoosters(player: Player) {
        if (activeBoosters.containsKey(player)) {
            activeBoosters[player]?.forEach { boosterName ->
                plugin.config.getStringList("boosters.$boosterName.effect").forEach { effect ->
                    val effect = effect.split(":")[0].lowercase()
                    when (effect) {
                        "fly" -> {

                        }
                        "xp" -> {

                        }
                        "mob" -> {

                        } else -> {
                            if (boosterStart.containsKey(player)) {
                                if (boosterStart[player]!!.containsKey(boosterName)) {
                                    if (boosterStart[player]!![boosterName] != null) {
                                        val potionEffect = effect.split(":")[0]
                                        val potionAmp = effect.split(":")[1].toIntOrNull() ?: 0
                                        player.addPotionEffect(
                                            PotionEffect(
                                                PotionEffectType.getByName(potionEffect)!!,
                                                (getBoosterTimeLeft(player, boosterName) * 20).toInt(),
                                                potionAmp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.LOWEST)
    fun onCommand(event: PlayerCommandSendEvent) {
        reapplyBoosters(event.player)
    }
}