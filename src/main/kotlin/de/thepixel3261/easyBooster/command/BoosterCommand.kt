package de.thepixel3261.easyBooster.command

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.gui.BoosterGUI
import de.thepixel3261.easyBooster.manager.ItemManager
import de.thepixel3261.easyBooster.manager.StorageManager
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.*

class BoosterCommand(plugin: Main) : CommandExecutor, TabCompleter {
    val plugin: Main = plugin
    override fun onCommand(player: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (player !is Player && args?.size == 0) {
            player.sendMessage("You have to be a Player to execute this command!")
            return true
        }

        when(args?.size) {
            0 -> {
                BoosterGUI(plugin).openBoosterGUI(player as Player)
                return true
            }
            1 -> {
                when(args[0]) {
                    "reload" -> {
                        plugin.reloadConfig()
                        player.sendMessage("Config reloaded!")
                        return true
                    }
                    "give" -> {
                        if (player.hasPermission("easybooster.give")) {
                            player.sendMessage("Usage: /booster give <player> <booster> (amount)")
                            return true
                        }
                    }
                    "item" -> {
                        if (player.hasPermission("easybooster.item")) {
                            player.sendMessage("Usage: /booster item <player> <booster> (amount)")
                            return true
                        }
                    }
                }
            }
            3 -> {
                if (args[0] == "give") {
                    if (player.hasPermission("easybooster.give")) {
                        val target = plugin.server.getPlayer(args[1])
                        if (target != null) {
                            val booster = args[2]
                            plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                                if (it == booster) {
                                    StorageManager(plugin).giveBooster(target, booster, 1)
                                    player.sendMessage("You have given 1 $booster to ${target.name}")
                                    return true
                                }
                            }
                        }
                    }
                }
                if (args[0] == "item") {
                    if (player.hasPermission("easybooster.item")) {
                        val boosterName = args[2]
                        val target = plugin.server.getPlayer(args[1])
                        if (target != null) {
                            ItemManager(plugin).giveItem(target, boosterName)
                            return true
                        }
                    }
                }
            }
            4 -> {
                if (args[0] == "give") {
                    if (player.hasPermission("easybooster.give")) {
                        val booster = args[2]
                        val amount = args[3].toInt()

                        var target: OfflinePlayer

                        try {
                            target = plugin.server.getOfflinePlayer(UUID.fromString(args[1]))
                        } catch (e: IllegalArgumentException) {
                            target = plugin.server.getOfflinePlayer(args[1])
                        }

                        if (!target.hasPlayedBefore()) {
                            player.sendMessage("Unbekannter Spieler!")
                            return true
                        }

                        plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                            if (it == booster) {
                                if (target.isOnline) {
                                    StorageManager(plugin).giveBooster(target.player!!, it, amount)
                                } else {
                                    StorageManager(plugin).giveBooster(target, it, amount)
                                }

                                player.sendMessage("You have given $amount $booster to ${target.name}")
                                return true
                            }
                        }
                    }
                }
                if (args[0] == "item") {
                    if (player.hasPermission("easybooster.item")) {
                        val boosterName = args[2]
                        val target = plugin.server.getPlayer(args[1])
                        for (i in 1..args[3].toInt()) {
                            ItemManager(plugin).giveItem(target as Player, boosterName)
                        }
                        return true
                    }
                }
            }
            else -> {
                return false
            }
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String> {
        val player = sender as Player
        var completes: MutableList<String> = mutableListOf()
        when (args?.size) {
            1 -> {
                completes.add("reload")
                completes.add("give")
                completes.add("item")
            }
            2 -> {
                if (args[0] == "give" && player.hasPermission("easybooster.give")) {
                    plugin.server.onlinePlayers.forEach {
                        completes.add(it.name)
                    }
                }
                if (args[0] == "item" && player.hasPermission("easybooster.item")) {
                    plugin.server.onlinePlayers.forEach {
                        completes.add(it.name)
                    }
                }
            }
            3 -> {
                if (args[0] == "give" && player.hasPermission("easybooster.give")) {
                    plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                        completes.add(it)
                    }
                }
                if (args[0] == "item" && player.hasPermission("easybooster.item")) {
                    plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                        completes.add(it)
                    }
                }
            }
        }
        return completes
    }
}
