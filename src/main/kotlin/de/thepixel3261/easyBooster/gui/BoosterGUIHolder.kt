package de.thepixel3261.easyBooster.gui

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class BoosterGUIHolder(private val size: Int, private val title: String) : InventoryHolder {
    private val inventory: Inventory = Bukkit.createInventory(this, size, title)

    override fun getInventory(): Inventory {
        return inventory
    }
}
