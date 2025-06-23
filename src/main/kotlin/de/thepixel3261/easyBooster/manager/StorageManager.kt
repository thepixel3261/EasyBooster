package de.thepixel3261.easyBooster.manager

import de.thepixel3261.easyBooster.Main
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

class StorageManager(private val plugin: Main) {
    private var storage = ""
    private var connection: Connection? = null

    init {
        storage = plugin.config.getString("storage-type", "yml").toString()

        if (storage == "mysql") {
            initializeMySQL()
        } else {
            // Create players folder if it doesn't exist
            val playersFolder = File(plugin.dataFolder, "players")
            if (!playersFolder.exists()) {
                playersFolder.mkdirs()
            }
        }
    }

    private fun initializeMySQL() {
        try {
            val host = plugin.config.getString("mysql.host", "localhost")
            val port = plugin.config.getInt("mysql.port", 3306)
            val database = plugin.config.getString("mysql.database", "easybooster")
            val username = plugin.config.getString("mysql.username", "root")
            val password = plugin.config.getString("mysql.password", "")

            connection = DriverManager.getConnection(
                "jdbc:mysql://$host:$port/$database?useSSL=false&autoReconnect=true",
                username,
                password
            )

            // Create table if not exists
            val createTable = """
                CREATE TABLE IF NOT EXISTS player_boosters (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    boosters JSON NOT NULL,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """.trimIndent()

            connection?.prepareStatement(createTable)?.execute()

        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize MySQL connection: ${e.message}")
            e.printStackTrace()
        }
    }

    fun takeBooster(player: Player, boosterName: String, amount: Int) {
        if (storage == "yml") {
            takeBoosterYML(player, boosterName, amount)
        } else if (storage == "mysql") {
            takeBoosterMySQL(player, boosterName, amount)
        }
    }

    fun giveBooster(player: Player, boosterName: String, amount: Int) {
        if (storage == "yml") {
            giveBoosterYML(player, boosterName, amount)
        } else if (storage == "mysql") {
            giveBoosterMySQL(player, boosterName, amount)
        }
    }

    fun getBoosterAmount(player: Player, boosterName: String): Int {
        return if (storage == "yml") {
            getBoosterAmountYML(player, boosterName)
        } else if (storage == "mysql") {
            getBoosterAmountMySQL(player, boosterName)
        } else {
            0
        }
    }

    fun getAllBoosters(player: Player): Map<String, Int> {
        return if (storage == "yml") {
            getAllBoostersYML(player)
        } else if (storage == "mysql") {
            getAllBoostersMySQL(player)
        } else {
            emptyMap()
        }
    }

    // YML Storage Implementation
    private fun getPlayerFile(player: Player): File {
        return File(plugin.dataFolder, "players/${player.name}.yml")
    }

    private fun getPlayerConfig(player: Player): FileConfiguration {
        val file = getPlayerFile(player)
        if (!file.exists()) {
            file.createNewFile()
            val config = YamlConfiguration.loadConfiguration(file)
            config.set("uuid", player.uniqueId.toString())
            config.set("name", player.name)
            config.set("boosters", mapOf<String, Int>())
            config.save(file)
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    private fun savePlayerConfig(player: Player, config: FileConfiguration) {
        try {
            config.save(getPlayerFile(player))
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save player data for ${player.name}: ${e.message}")
        }
    }

    private fun takeBoosterYML(player: Player, boosterName: String, amount: Int) {
        val config = getPlayerConfig(player)
        val currentAmount = config.getInt("boosters.$boosterName", 0)
        val newAmount = maxOf(0, currentAmount - amount)
        config.set("boosters.$boosterName", newAmount)
        savePlayerConfig(player, config)
    }

    private fun giveBoosterYML(player: Player, boosterName: String, amount: Int) {
        val config = getPlayerConfig(player)
        val currentAmount = config.getInt("boosters.$boosterName", 0)
        config.set("boosters.$boosterName", currentAmount + amount)
        savePlayerConfig(player, config)
    }

    private fun getBoosterAmountYML(player: Player, boosterName: String): Int {
        val config = getPlayerConfig(player)
        return config.getInt("boosters.$boosterName", 0)
    }

    private fun getAllBoostersYML(player: Player): Map<String, Int> {
        val config = getPlayerConfig(player)
        val boostersSection = config.getConfigurationSection("boosters")
        val boosters = mutableMapOf<String, Int>()

        boostersSection?.getKeys(false)?.forEach { key ->
            boosters[key] = config.getInt("boosters.$key", 0)
        }

        return boosters
    }

    // MySQL Storage Implementation
    private fun takeBoosterMySQL(player: Player, boosterName: String, amount: Int) {
        try {
            val currentBoosters = getAllBoostersMySQL(player).toMutableMap()
            val currentAmount = currentBoosters[boosterName] ?: 0
            val newAmount = maxOf(0, currentAmount - amount)

            if (newAmount == 0) {
                currentBoosters.remove(boosterName)
            } else {
                currentBoosters[boosterName] = newAmount
            }

            updateBoostersMySQL(player, currentBoosters)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to take booster for ${player.name}: ${e.message}")
        }
    }

    private fun giveBoosterMySQL(player: Player, boosterName: String, amount: Int) {
        try {
            val currentBoosters = getAllBoostersMySQL(player).toMutableMap()
            val currentAmount = currentBoosters[boosterName] ?: 0
            currentBoosters[boosterName] = currentAmount + amount

            updateBoostersMySQL(player, currentBoosters)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to give booster to ${player.name}: ${e.message}")
        }
    }

    private fun getBoosterAmountMySQL(player: Player, boosterName: String): Int {
        return getAllBoostersMySQL(player)[boosterName] ?: 0
    }

    private fun getAllBoostersMySQL(player: Player): Map<String, Int> {
        try {
            val query = "SELECT boosters FROM player_boosters WHERE player_uuid = ?"
            val statement: PreparedStatement = connection?.prepareStatement(query) ?: return emptyMap()
            statement.setString(1, player.uniqueId.toString())

            val result: ResultSet = statement.executeQuery()

            if (result.next()) {
                val boostersJson = result.getString("boosters")
                return parseBoostersJson(boostersJson)
            }

            return emptyMap()
        } catch (e: Exception) {
            plugin.logger.severe("Failed to get boosters for ${player.name}: ${e.message}")
            return emptyMap()
        }
    }

    private fun updateBoostersMySQL(player: Player, boosters: Map<String, Int>) {
        try {
            val boostersJson = createBoostersJson(boosters)

            val query = """
                INSERT INTO player_boosters (player_uuid, player_name, boosters) 
                VALUES (?, ?, ?) 
                ON DUPLICATE KEY UPDATE 
                player_name = VALUES(player_name), 
                boosters = VALUES(boosters)
            """.trimIndent()

            val statement: PreparedStatement = connection?.prepareStatement(query) ?: return
            statement.setString(1, player.uniqueId.toString())
            statement.setString(2, player.name)
            statement.setString(3, boostersJson)

            statement.executeUpdate()
        } catch (e: Exception) {
            plugin.logger.severe("Failed to update boosters for ${player.name}: ${e.message}")
        }
    }

    // Simple JSON parsing (you might want to use Gson or similar)
    private fun parseBoostersJson(json: String): Map<String, Int> {
        val boosters = mutableMapOf<String, Int>()
        if (json.isBlank() || json == "{}") return boosters

        // Simple JSON parsing - consider using Gson for production
        val content = json.trim('{', '}')
        if (content.isNotEmpty()) {
            content.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().trim('"')
                    val value = parts[1].trim().toIntOrNull() ?: 0
                    boosters[key] = value
                }
            }
        }

        return boosters
    }

    private fun createBoostersJson(boosters: Map<String, Int>): String {
        if (boosters.isEmpty()) return "{}"

        val jsonPairs = boosters.map { "\"${it.key}\":${it.value}" }
        return "{${jsonPairs.joinToString(",")}}"
    }

    fun closeConnection() {
        try {
            connection?.close()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to close MySQL connection: ${e.message}")
        }
    }
}
