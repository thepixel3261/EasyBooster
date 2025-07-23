package de.thepixel3261.easyBooster.gui

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.manager.BoosterManager
import de.thepixel3261.easyBooster.util.Parse
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BoosterGUI(val plugin: Main) {
    fun openBoosterGUI(player :Player) {
        val boosterManager = BoosterManager.getInstance(plugin)
        
        val guiName = Parse(plugin).parse(plugin.config.getString("gui.name", "§6§lBooster").toString(), player)
        val rows = plugin.config.getInt("gui.rows", 3)

        var gui: Inventory = Bukkit.createInventory(BoosterGUIHolder(rows * 9, guiName), rows * 9, guiName)

        var filler = ItemStack(Material.valueOf(plugin.config.getString("gui.filler", "GRAY_STAINED_GLASS_PANE").toString()))
        var fillerMeta = filler.itemMeta
        fillerMeta?.setDisplayName(" ")
        filler.itemMeta = fillerMeta

        for (i in 0 until rows * 9) {
            gui.setItem(i, filler)
        }

        plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
            val boosterName = it
            val active = boosterManager.activeBoosters.containsKey(player) && boosterManager.activeBoosters[player]!!.contains(boosterName)
            val state = if (active) "on" else "off"

            val slot = plugin.config.getInt("boosters.$boosterName.slot")

            val boosterIcon = plugin.config.getString("boosters.${boosterName}.icon.${state}", "BARRIER")!!.uppercase()
            val boosterItem = ItemStack(Material.valueOf(boosterIcon))

            var boosterDisplayName = boosterManager.getBoosterDisplayName(boosterName, player)
            var boosterLore = boosterManager.getBoosterLore(boosterName, player)

            var boosterMeta = boosterItem.itemMeta

            boosterMeta.setDisplayName(boosterDisplayName)

            var finalBoosterLore = mutableListOf<String>()
            boosterLore.forEach { line ->
                var line2 = Parse(plugin).parse(player, line, boosterName)
                finalBoosterLore.add(line2)
            }
            boosterMeta.lore = finalBoosterLore

            boosterItem.setItemMeta(boosterMeta)

            gui.setItem(slot, boosterItem)
        }

        player.openInventory(gui)
    }
}
