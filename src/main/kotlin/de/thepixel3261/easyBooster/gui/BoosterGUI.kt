package de.thepixel3261.easyBooster.gui;

import de.thepixel3261.easyBooster.Main
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class BoosterGUI(val plugin: Main) {
    fun openBoosterGUI(player :Player) {
        val guiName = plugin.config.getString("gui.name", "§6§lBooster").toString()
        val rows = plugin.config.getInt("gui.rows", 3)

        var gui: Inventory = Bukkit.createInventory(BoosterGUIHolder(rows * 9, guiName), rows * 9, guiName)


        val filler = ItemStack(Material.valueOf(plugin.config.getString("gui.filler", "GRAY_STAINED_GLASS_PANE").toString()))
        for (i in 0 until rows * 9) {
            gui.setItem(i, filler)
        }

        plugin.config.getConfigurationSection("boosters")?.getKeys(false)?.forEach {
            val boosterName = it
            val boosterDisplayName = plugin.config.getString("boosters.$boosterName.displayname")
            val boosterLore: List<String> = plugin.config.getStringList("boosters.$boosterName.lore")
            val boosterIcon = plugin.config.getString("boosters.$boosterName.icon")!!.uppercase()
            val boosterItem = ItemStack(Material.valueOf(boosterIcon))
            val slot = plugin.config.getInt("boosters.$boosterName.slot")

            boosterItem.itemMeta = boosterItem.itemMeta!!.apply {
                this.setDisplayName(boosterDisplayName)
                this.lore = boosterLore
            }

            gui.setItem(slot, boosterItem)
        }

        player.openInventory(gui)
    }
}
