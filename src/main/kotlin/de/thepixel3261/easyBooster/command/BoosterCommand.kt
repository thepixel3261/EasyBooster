package de.thepixel3261.easyBooster.command;

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.gui.BoosterGUI
import de.thepixel3261.easyBooster.manager.StorageManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

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
                }
            }
            3 -> {
                if (args[0] == "give") {
                    if (player.hasPermission("easybooster.give")) {
                        val target = plugin.server.getPlayer(args[2])
                        if (target != null) {
                            val booster = args[1]
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
            }
            4 -> {
                if (args[0] == "give") {
                    if (player.hasPermission("easybooster.give")) {
                        val target = plugin.server.getPlayer(args[2])
                        if (target != null) {
                            val booster = args[1]
                            val amount = args[3].toInt()
                            plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                                if (it == booster) {
                                    StorageManager(plugin).giveBooster(target, it, amount)
                                    player.sendMessage("You have given $amount $booster to ${target.name}")
                                    return true
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                return false
            }
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): MutableList<String>? {
        val player = sender as Player
        var completes: MutableList<String> = mutableListOf()
        when (args?.size) {
            1 -> {
                completes.add("reload")
                completes.add("give")
            }
            2 -> {
                if (args[0] == "give" && player.hasPermission("easybooster.give")) {
                    plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
                        completes.add(it)
                    }
                }
            }
            3 -> {
                if (args[0] == "give" && player.hasPermission("easybooster.give")) {
                    plugin.server.onlinePlayers.forEach {
                        completes.add(it.name)
                    }
                }
            }
        }
        return completes
    }
}
