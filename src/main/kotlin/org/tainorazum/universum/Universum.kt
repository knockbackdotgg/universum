package org.tainorazum.universum

import gg.flyte.twilight.scheduler.delay
import gg.flyte.twilight.twilight
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.tainorazum.universum.chat.manager.ChatFilterManager
import org.tainorazum.universum.chat.manager.ChatManager
import org.tainorazum.universum.coins.CoinsManager
import org.tainorazum.universum.command.admin.commands.AdminCommandBypass
import org.tainorazum.universum.command.admin.commands.AdminCommandCoins
import org.tainorazum.universum.command.admin.commands.AdminCommandCosmetics
import org.tainorazum.universum.command.admin.commands.AdminCommandGeneral
import org.tainorazum.universum.command.admin.commands.AdminCommandKits
import org.tainorazum.universum.command.admin.commands.AdminCommandPunishments
import org.tainorazum.universum.command.admin.commands.AdminCommandWhitelist
import org.tainorazum.universum.command.default.CosmeticsCommand
import org.tainorazum.universum.command.default.CycleHoloCommand
import org.tainorazum.universum.command.default.FriendsCommand
import org.tainorazum.universum.command.default.KitEditorCommand
import org.tainorazum.universum.command.default.SettingsCommand
import org.tainorazum.universum.command.universum.commands.UniversumCommandDatabase
import org.tainorazum.universum.command.universum.commands.UniversumCommandGeneral
import org.tainorazum.universum.command.universum.commands.UniversumCommandKitEditor
import org.tainorazum.universum.command.universum.commands.UniversumCommandLobby
import org.tainorazum.universum.command.universum.commands.UniversumCommandOther
import org.tainorazum.universum.command.universum.commands.UniversumCommandServerList
import org.tainorazum.universum.cosmetic.manager.CosmeticManager
import org.tainorazum.universum.cosmetic.pet.PetManager
import org.tainorazum.universum.database.DatabaseManager
import org.tainorazum.universum.engine.arena.ArenaManager
import org.tainorazum.universum.engine.arena.database.ArenaDatabaseManager
import org.tainorazum.universum.engine.game.manager.GameManager
import org.tainorazum.universum.engine.statistics.database.DB_Stats
import org.tainorazum.universum.friends.manager.FriendsManager
import org.tainorazum.universum.kiteditor.database.DB_KitEditor
import org.tainorazum.universum.kiteditor.editor.KitEditorManager
import org.tainorazum.universum.lobby.database.DB_Lobby
import org.tainorazum.universum.lobby.manager.LobbyManager
import org.tainorazum.universum.network.manager.NetworkPlayerManager
import org.tainorazum.universum.network.nametag.NametagManager
import org.tainorazum.universum.placeholders.holo.HoloStatsPlaceholder
import org.tainorazum.universum.placeholders.stats.StatsPlaceholder
import org.tainorazum.universum.player.PlayerManager
import org.tainorazum.universum.punishment.manager.PunishmentManager
import org.tainorazum.universum.redis.RedisManager
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.server.server.Server
import org.tainorazum.universum.server.server.ServerManager
import org.tainorazum.universum.server.server.ServerType
import org.tainorazum.universum.server.setting.DB_ServerSettings
import org.tainorazum.universum.server.setting.ServerSetting
import org.tainorazum.universum.server.update.Updater
import org.tainorazum.universum.server.visual.VisualServerManager
import org.tainorazum.universum.setting.manager.PlayerSettingManager
import org.tainorazum.universum.utility.gui.AnvilInput
import org.tainorazum.universum.utility.other.parseDuration
import org.tainorazum.universum.whitelist.WhitelistManager
import revxrsal.commands.bukkit.BukkitLamp
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

class Universum : JavaPlugin() {

    companion object {
        lateinit var instance: Universum private set
        lateinit var server: Server

        var dbm: DatabaseManager? = null
            private set

        // all servers
        lateinit var adbm: ArenaDatabaseManager private set
        lateinit var arenaManager: ArenaManager private set
        lateinit var gameManager: GameManager private set
        lateinit var serverManager: ServerManager private set
        lateinit var visualServerManager: VisualServerManager private set
        lateinit var luckperms: LuckPerms private set
        lateinit var whitelistManager: WhitelistManager private set
        lateinit var playerManager: PlayerManager private set
        lateinit var punishmentManager: PunishmentManager private set
        lateinit var chatManager: ChatManager private set
        lateinit var chatFilterManager: ChatFilterManager private set
        lateinit var cosmeticManager: CosmeticManager private set
        lateinit var playerSettingManager: PlayerSettingManager private set
        lateinit var kitEditorManager: KitEditorManager private set
        lateinit var friendsManager: FriendsManager private set
        lateinit var coinsManager: CoinsManager private set
        lateinit var updater: Updater private set

        // lobby servers
        lateinit var lobbyManager: LobbyManager private set
    }

    override fun onEnable() {
        instance = this
        saveDefaultConfig()
        twilight(this)

        // 1. Initialize Database
        try {
            dbm = DatabaseManager()
            dbm?.init()
            logger.info("Successfully connected to the database pool.")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to initialize database connection! Disabling plugin...", e)
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        // 2. Initialize Services & Managers FIRST before Redis listeners
        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            luckperms = provider.provider
        }

        updater = Updater()
        lobbyManager = LobbyManager()
        adbm = ArenaDatabaseManager()
        arenaManager = ArenaManager()
        gameManager = GameManager()
        serverManager = ServerManager()
        visualServerManager = VisualServerManager()
        whitelistManager = WhitelistManager()
        playerManager = PlayerManager()
        punishmentManager = PunishmentManager()
        PetManager.registerEvents()
        cosmeticManager = CosmeticManager()
        chatFilterManager = ChatFilterManager()
        chatManager = ChatManager()
        playerSettingManager = PlayerSettingManager()
        kitEditorManager = KitEditorManager()
        friendsManager = FriendsManager()
        coinsManager = CoinsManager()

        AnvilInput.init(this)

        // 3. Initialize Redis NOW (after friendsManager exists)
        val redisEnabled = config.getBoolean("redis.enabled", false)
        if (redisEnabled) {
            val uri = config.getString("redis.uri") ?: "redis://localhost:6379"
            if (RedisManager.init(uri)) {
                RedisSubscriber.start(this)
                NetworkPlayerManager.loadInitialPlayers()
            } else {
                logger.warning("Redis is enabled in config but failed to connect.")
            }
        }
        DB_ServerSettings.set(ServerSetting.REDIS, redisEnabled)

        // 4. Register Commands & Features
        val lamp = BukkitLamp.builder(this)
            .parameterTypes { types ->
                types.addParameterType(Duration::class.java) { input, _ ->
                    val text = input.readString()
                    parseDuration(text) ?: throw IllegalArgumentException("Invalid duration format!")
                }
            }
            .build()

        NametagManager.init()

        delay(20) {
            try {
                DB_ServerSettings.loadAllSettingsIntoCache().get()
                logger.info("Successfully loaded server settings into cache.")
            } catch (e: Exception) {
                logger.warning("Failed to load server settings into cache: ${e.message}")
            }

            if (Companion.server.type == ServerType.LOBBY) {
                if (!DB_Lobby.loadAndCacheBoundary()) {
                    logger.warning("Failed to load lobby world from database; lobby checks will be disabled.")
                }
                if (!DB_KitEditor.loadAndCacheBoundary()) {
                    logger.warning("Failed to load kit editor world from database; kit editor won't work.")
                }
            }
        }

        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            StatsPlaceholder().register()
            HoloStatsPlaceholder().register()
            logger.info("Successfully registered PlaceholderAPI expansions!")
        } else {
            logger.warning("PlaceholderAPI not found! Hologram & stat placeholders will not function.")
        }

        // default commands
        lamp.register(SettingsCommand())
        lamp.register(CycleHoloCommand())
        lamp.register(CosmeticsCommand())
        lamp.register(KitEditorCommand())
        lamp.register(FriendsCommand())

        // universum commands
        lamp.register(UniversumCommandGeneral())
        lamp.register(UniversumCommandOther())
        lamp.register(UniversumCommandLobby())
        lamp.register(UniversumCommandDatabase())
        lamp.register(UniversumCommandServerList())
        lamp.register(UniversumCommandKitEditor())

        // admin commands
        lamp.register(AdminCommandGeneral())
        lamp.register(AdminCommandWhitelist())
        lamp.register(AdminCommandBypass())
        lamp.register(AdminCommandPunishments())
        lamp.register(AdminCommandKits())
        lamp.register(AdminCommandCosmetics())
        lamp.register(AdminCommandCoins())

        delay(40) {
            open().thenAccept { success ->
                if (success) {
                    server.setWhitelist(false)
                    serverManager.online()
                    update()
                }
            }
        }
    }

    override fun onDisable() {
        DB_Stats.shutdownAndFlush()

        RedisSubscriber.stop()
        RedisManager.close()

        dbm?.close()
        if (dbm != null) {
            logger.info("Database pool safely disconnected.")
        }
    }

    private fun update() {
        val interval = config.getInt("update-interval").or(60)
        Bukkit.getScheduler().runTaskTimer(this, Runnable {
            updater.update()
        }, 10L, interval * 20L)
    }

    private fun open(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        val arenaManagerFuture = arenaManager.success
        val lobbyManagerFuture = lobbyManager.success
        val kitEditorManagerFuture = kitEditorManager.success

        CompletableFuture.allOf(arenaManagerFuture, lobbyManagerFuture, kitEditorManagerFuture).thenAccept {
            val allSuccess = arenaManagerFuture.join() && lobbyManagerFuture.join() && kitEditorManagerFuture.join()
            future.complete(allSuccess)
        }

        return future
    }
}