package de.thepixel3261.easyBooster.manager

import de.thepixel3261.easyBooster.Main
import de.thepixel3261.easyBooster.util.Parse
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ItemManager(private val plugin: Main): Listener {
    fun giveItem(player: Player, booster: String) {
        if (!player.isOnline) return

        val itemName = Parse(plugin).parse(player, plugin.config.getString("booster.item.name")!!, booster)

        var itemLore = listOf<String>()
        for (line in plugin.config.getStringList("booster.item.lore")) {
            itemLore += Parse(plugin).parse(player, line, booster)
        }

        val item = Material.valueOf(plugin.config.getString("booster.item.material")!!.uppercase())
        var itemStack = ItemStack(item)
        val itemMeta = itemStack.itemMeta
        itemMeta?.apply {
            setDisplayName(itemName)
            setLore(itemLore)
        }

        val nbtKey = plugin.config.getString("booster.item.nbtKey", "booster_name") ?: "booster_name"
        val container = itemMeta?.persistentDataContainer
        container?.set(NamespacedKey(plugin, nbtKey), PersistentDataType.STRING, booster)
        itemStack.itemMeta = itemMeta

        player.inventory.addItem(itemStack)
    }

    @EventHandler
    fun onItemInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        val player = event.player
        val item = player.inventory.itemInMainHand

        val materialName: String = plugin.config.getString("booster.item.material", "WRITTEN_BOOK")!!
        val material = Material.valueOf(materialName)

        if (item != null && item.type == material && item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta != null) {
                // NBT-Daten abrufen
                val nbtKey: String = plugin.config.getString("booster.item.nbtKey", "booster_name")!!
                val container = meta.persistentDataContainer

                if (container.has<String, String>(NamespacedKey(plugin, nbtKey), PersistentDataType.STRING)) {
                    val boosterName = container.get<String, String>(NamespacedKey(plugin, nbtKey), PersistentDataType.STRING)

                    player.sendMessage(Parse(plugin).getFormattedPrefix() + "Du hast einen ${BoosterManager.getInstance(plugin).getBoosterDisplayName(boosterName!!, player)} §ferhalten!")

                    // Booster hinzufügen
                    StorageManager(plugin).giveBooster(player, boosterName, 1)

                    // Buch entfernen
                    item.amount--
                    player.inventory.setItemInMainHand(item)
                    player.inventory.setItemInMainHand(if (item.amount > 0) item else null)

                    // Event abbrechen
                    event.isCancelled = true
                    return
                }


                // Old Plugin
                if (meta.displayName.endsWith("Booster-Rolle") && ChatColor.getLastColors(meta.displayName) == ChatColor.GREEN.toString()) {
                    if (meta.hasLore()) {
                        var displayName = meta.displayName
                        val boosterName = ChatColor.stripColor(displayName.replace(" Booster-Rolle", ""))!!

                        item.amount--
                        ItemManager(plugin).giveItem(player, boosterName)
                        player.sendMessage(Parse(plugin).getFormattedPrefix() + "Nach einem Booster Update gibt es ein neues Booster Item für dich!")
                    }
                }
            }
        }
    }
}