package de.thepixel3261.easyBooster.manager

import de.thepixel3261.easyBooster.Main
import org.bukkit.OfflinePlayer
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
        storage = plugin.config.getString("storage.type", "yaml")!!.lowercase()

        if (storage == "mysql") {
            initializeMySQL()
        } else {
            // Create players folder if it doesn't exist
            val playersFolder = File(plugin.dataFolder, "players")
            if (!playersFolder.exists()) {
                playersFolder.mkdirs()
            }
        }

        // First Boot

        if (!plugin.config.getBoolean("bootedOnce", false)) {
            updatePlugin()
        }
        plugin.config.set("bootedOnce", true)
    }

    private fun initializeMySQL() {
        try {
            val host = plugin.config.getString("storage.mysql.host", "localhost")
            val port = plugin.config.getInt("storage.mysql.port", 3306)
            val database = plugin.config.getString("storage.mysql.database", "easybooster")
            val username = plugin.config.getString("storage.mysql.username", "root")
            val password = plugin.config.getString("storage.mysql.password", "")

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
        if (storage == "yaml") {
            takeBoosterYML(player, boosterName, amount)
        } else if (storage == "mysql") {
            takeBoosterMySQL(player, boosterName, amount)
        }
    }

    fun giveBooster(player: Player, boosterName: String, amount: Int) {
        if (storage == "yaml") {
            giveBoosterYML(player, boosterName, amount)
        } else if (storage == "mysql") {
            giveBoosterMySQL(player, boosterName, amount)
        }
    }

    fun giveBooster(player: OfflinePlayer, boosterName: String, amount: Int) {
        if (storage == "yaml") {
            giveBoosterYML(player, boosterName, amount)
        } else if (storage == "mysql") {
            giveBoosterMySQL(player, boosterName, amount)
        }
    }

    fun getBoosterAmount(player: Player, boosterName: String): Int {
        return when (storage) {
            "yaml" -> {
                getBoosterAmountYML(player, boosterName)
            }
            "mysql" -> {
                getBoosterAmountMySQL(player, boosterName)
            }
            else -> {
                0
            }
        }
    }

    fun getAllBoosters(player: Player): Map<String, Int> {
        return if (storage == "yaml") {
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

    private fun getPlayerFile(player: OfflinePlayer): File {
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

    private fun getPlayerConfig(player: OfflinePlayer): FileConfiguration {
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

    private fun savePlayerConfig(player: OfflinePlayer, config: FileConfiguration) {
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

    private fun giveBoosterYML(player: OfflinePlayer, boosterName: String, amount: Int) {
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

    private fun giveBoosterMySQL(player: OfflinePlayer, boosterName: String, amount: Int) {
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

    private fun getAllBoostersMySQL(player: OfflinePlayer): Map<String, Int> {
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

    private fun updateBoostersMySQL(player: OfflinePlayer, boosters: Map<String, Int>) {
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


    // Update the plugin sql


    fun updatePlugin() {
        try {
            if (storage != "mysql") {
                plugin.logger.info("Plugin update only applies to MySQL storage. Skipping migration.")
                return
            }

            plugin.logger.info("Starting plugin migration from old EasyBooster format...")

            // Check if old table exists
            val checkOldTable = "SHOW TABLES LIKE 'player_boosters'"
            val checkStatement = connection?.prepareStatement(checkOldTable)
            val checkResult = checkStatement?.executeQuery()

            if (checkResult?.next() != true) {
                plugin.logger.info("No old player_boosters table found. Migration not needed.")
                return
            }

            // Check if old table has the old structure (separate columns for booster_type and amount)
            val checkOldStructure = """
            SELECT COLUMN_NAME 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = DATABASE() 
            AND TABLE_NAME = 'player_boosters' 
            AND COLUMN_NAME IN ('booster_type', 'amount')
        """.trimIndent()

            val structureStatement = connection?.prepareStatement(checkOldStructure)
            val structureResult = structureStatement?.executeQuery()

            var hasOldStructure = false
            var columnCount = 0
            while (structureResult?.next() == true) {
                columnCount++
            }

            if (columnCount < 2) {
                plugin.logger.info("Old table structure not detected. Migration not needed.")
                return
            }

            plugin.logger.info("Old table structure detected. Starting migration...")

            // Create backup table
            val backupTable = """
            CREATE TABLE IF NOT EXISTS player_boosters_backup_${System.currentTimeMillis()} 
            AS SELECT * FROM player_boosters
        """.trimIndent()

            connection?.prepareStatement(backupTable)?.execute()
            plugin.logger.info("Created backup table for safety.")

            // Get all old data grouped by player
            val selectOldData = """
            SELECT player_uuid, booster_type, amount 
            FROM player_boosters 
            WHERE booster_type IS NOT NULL AND amount IS NOT NULL
            ORDER BY player_uuid
        """.trimIndent()

            val selectStatement = connection?.prepareStatement(selectOldData)
            val oldDataResult = selectStatement?.executeQuery()

            // Group boosters by player UUID
            val playerBoosters = mutableMapOf<String, MutableMap<String, Int>>()

            while (oldDataResult?.next() == true) {
                val playerUuid = oldDataResult.getString("player_uuid")
                val boosterType = oldDataResult.getString("booster_type")
                val amount = oldDataResult.getInt("amount")

                if (playerUuid != null && boosterType != null && amount > 0) {
                    playerBoosters.getOrPut(playerUuid) { mutableMapOf() }[boosterType] = amount
                }
            }

            plugin.logger.info("Found ${playerBoosters.size} players with booster data to migrate.")

            // Drop old table and create new structure
            connection?.prepareStatement("DROP TABLE player_boosters")?.execute()

            val createNewTable = """
            CREATE TABLE player_boosters (
                player_uuid VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                boosters JSON NOT NULL,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
        """.trimIndent()

            connection?.prepareStatement(createNewTable)?.execute()
            plugin.logger.info("Created new table structure.")

            // Insert migrated data
            val insertQuery = """
            INSERT INTO player_boosters (player_uuid, player_name, boosters) 
            VALUES (?, ?, ?)
        """.trimIndent()

            var migratedPlayers = 0

            playerBoosters.forEach { (playerUuid, boosters) ->
                try {
                    val boostersJson = createBoostersJson(boosters)

                    // Try to get player name from server (if online) or use UUID as fallback
                    val playerName = plugin.server.getPlayer(java.util.UUID.fromString(playerUuid))?.name
                        ?: "Unknown_$playerUuid"

                    val insertStatement = connection?.prepareStatement(insertQuery)
                    insertStatement?.setString(1, playerUuid)
                    insertStatement?.setString(2, playerName)
                    insertStatement?.setString(3, boostersJson)
                    insertStatement?.executeUpdate()

                    migratedPlayers++
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to migrate data for player $playerUuid: ${e.message}")
                }
            }

            plugin.logger.info("Successfully migrated $migratedPlayers players to new format.")
            plugin.logger.info("Plugin migration completed successfully!")

        } catch (e: Exception) {
            plugin.logger.severe("Failed to update plugin from old format: ${e.message}")
            e.printStackTrace()

            // If migration fails, ensure we still have a working table
            try {
                val createTable = """
                CREATE TABLE IF NOT EXISTS player_boosters (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    boosters JSON NOT NULL,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
            """.trimIndent()

                connection?.prepareStatement(createTable)?.execute()
            } catch (fallbackException: Exception) {
                plugin.logger.severe("Failed to create fallback table: ${fallbackException.message}")
            }
        }
    }
}
