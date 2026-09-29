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
import org.tainorazum.universum.command.admin.commands.AdminCommandGames
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
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_Kits
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

        logger.info("========================================")
        logger.info("        Starting Universum Plugin       ")
        logger.info("========================================")

        // ------------------------------------------------------------
        // Database
        // ------------------------------------------------------------

        try {
            dbm = DatabaseManager()
            dbm?.init()

            logger.info("Database initialized successfully.")
        } catch (ex: Exception) {
            logger.log(
                Level.SEVERE,
                "Failed to initialize database! Disabling plugin...",
                ex
            )

            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        // ------------------------------------------------------------
        // LuckPerms
        // ------------------------------------------------------------

        val provider =
            Bukkit.getServicesManager()
                .getRegistration(LuckPerms::class.java)

        if (provider != null) {
            luckperms = provider.provider
            logger.info("LuckPerms initialized successfully.")
        } else {
            logger.severe("LuckPerms API was not found!")
        }

        // ------------------------------------------------------------
        // General managers
        //
        // IMPORTANT:
        // These must be initialized BEFORE RedisSubscriber because
        // Redis listeners may access Universum.server and other
        // managers during registration.
        // ------------------------------------------------------------

        updater = Updater()

        adbm = ArenaDatabaseManager()
        arenaManager = ArenaManager()

        gameManager = GameManager()

        /*
         * ServerManager initializes Universum.server.
         *
         * RedisSubscriber MUST NOT start before this.
         */
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

        /*
         * LobbyManager registers player listeners, so its dependencies
         * must already exist.
         */
        friendsManager = FriendsManager()
        coinsManager = CoinsManager()

        lobbyManager = LobbyManager()

        AnvilInput.init(this)

        // ------------------------------------------------------------
        // Redis
        //
        // IMPORTANT:
        // Redis initialization is intentionally AFTER the managers
        // above. RedisSubscriber.registerListeners() accesses
        // Universum.server.
        // ------------------------------------------------------------

        val redisEnabled =
            config.getBoolean("redis.enabled", false)

        if (redisEnabled) {

            val uri =
                config.getString("redis.uri")
                    ?: "redis://localhost:6379"

            if (RedisManager.init(uri)) {

                logger.info(
                    "[RedisManager] Successfully connected to Redis!"
                )

                RedisSubscriber.start(this)

                NetworkPlayerManager.loadInitialPlayers()

            } else {

                logger.warning(
                    "Redis is enabled in config but failed to connect."
                )
            }
        }

        DB_ServerSettings.set(
            ServerSetting.REDIS,
            redisEnabled
        )

        // ------------------------------------------------------------
        // Command framework
        // ------------------------------------------------------------

        val lamp = BukkitLamp.builder(this)
            .parameterTypes { types ->

                types.addParameterType(
                    Duration::class.java
                ) { input, _ ->

                    val text = input.readString()

                    parseDuration(text)
                        ?: throw IllegalArgumentException(
                            "Invalid duration format!"
                        )
                }

                types.addParameterType(
                    Kit::class.java
                ) { input, _ ->

                    val kitId = input.readString()

                    DB_Kits.getKit(kitId).join()
                        ?: throw IllegalArgumentException(
                            "Kit '$kitId' does not exist!"
                        )
                }
            }
            .suggestionProviders { providers ->

                providers.addProvider(Kit::class.java) {

                    DB_Kits.getAllDefaultKits()
                        .join()
                        .map { it.id }
                }
            }
            .build()

        // ------------------------------------------------------------
        // Other services
        // ------------------------------------------------------------

        NametagManager.init()

        // ------------------------------------------------------------
        // PlaceholderAPI
        // ------------------------------------------------------------

        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {

            StatsPlaceholder().register()
            HoloStatsPlaceholder().register()

            logger.info(
                "Successfully registered PlaceholderAPI expansions!"
            )

        } else {

            logger.warning(
                "PlaceholderAPI not found! " +
                        "Hologram & stat placeholders will not function."
            )
        }

        // ------------------------------------------------------------
        // Commands
        // ------------------------------------------------------------

        // Default commands
        lamp.register(SettingsCommand())
        lamp.register(CycleHoloCommand())
        lamp.register(CosmeticsCommand())
        lamp.register(KitEditorCommand())
        lamp.register(FriendsCommand())

        // Universum commands
        lamp.register(UniversumCommandGeneral())
        lamp.register(UniversumCommandOther())
        lamp.register(UniversumCommandLobby())
        lamp.register(UniversumCommandDatabase())
        lamp.register(UniversumCommandServerList())
        lamp.register(UniversumCommandKitEditor())

        // Admin commands
        lamp.register(AdminCommandGeneral())
        lamp.register(AdminCommandWhitelist())
        lamp.register(AdminCommandBypass())
        lamp.register(AdminCommandPunishments())
        lamp.register(AdminCommandKits())
        lamp.register(AdminCommandCosmetics())
        lamp.register(AdminCommandCoins())
        lamp.register(AdminCommandGames())

        logger.info("Commands registered successfully.")

        // ------------------------------------------------------------
        // Delayed database/cache initialization
        //
        // KitEditorManager is intentionally created here because its
        // constructor immediately starts build(), which needs the
        // KitEditor database/cache values.
        // ------------------------------------------------------------

        delay(20) {

            try {

                logger.info(
                    "Loading persistent Universum settings/boundaries..."
                )

                // ----------------------------------------------------
                // Server settings
                // ----------------------------------------------------

                try {

                    DB_ServerSettings
                        .loadAllSettingsIntoCache()
                        .get()

                    logger.info(
                        "Successfully loaded server settings into cache."
                    )

                } catch (ex: Exception) {

                    logger.warning(
                        "Failed to load server settings into cache: " +
                                ex.message
                    )
                }

                // ----------------------------------------------------
                // Lobby boundaries
                // ----------------------------------------------------

                if (Companion.server.type == ServerType.LOBBY) {

                    val lobbyLoaded =
                        DB_Lobby.loadAndCacheBoundary()

                    if (lobbyLoaded) {

                        logger.info(
                            "Lobby boundary loaded successfully."
                        )

                    } else {

                        logger.warning(
                            "Failed to load lobby boundary."
                        )
                    }

                    // ------------------------------------------------
                    // Kit Editor boundary
                    // ------------------------------------------------

                    val kitEditorLoaded =
                        DB_KitEditor.loadAndCacheBoundary()

                    if (kitEditorLoaded) {

                        logger.info(
                            "Kit editor boundary/cache loaded successfully."
                        )

                    } else {

                        logger.warning(
                            "Failed to load kit editor boundary/cache."
                        )
                    }

                    // ------------------------------------------------
                    // Kit Editor Manager
                    // ------------------------------------------------

                    logger.info(
                        "Initializing KitEditorManager..."
                    )

                    kitEditorManager =
                        KitEditorManager()

                    logger.info(
                        "KitEditorManager initialization started."
                    )
                }

            } catch (ex: Exception) {

                logger.severe(
                    "Failed during delayed Universum initialization: " +
                            ex.message
                )

                ex.printStackTrace()

                server.setWhitelist(true)
                serverManager.offline()
            }
        }

        // ------------------------------------------------------------
        // Final startup
        //
        // Wait for ArenaManager, LobbyManager and KitEditorManager.
        // ------------------------------------------------------------

        delay(40) {

            try {

                logger.info(
                    "Waiting for Universum managers to finish initialization..."
                )

                val arenaManagerFuture =
                    arenaManager.success

                val lobbyManagerFuture =
                    lobbyManager.success

                /*
                 * KitEditorManager is only constructed by the 20-tick
                 * task above on lobby servers.
                 */
                val kitEditorManagerFuture =
                    if (Companion.server.type == ServerType.LOBBY) {
                        kitEditorManager.success
                    } else {
                        CompletableFuture.completedFuture(true)
                    }

                CompletableFuture.allOf(
                    arenaManagerFuture,
                    lobbyManagerFuture,
                    kitEditorManagerFuture
                ).thenAccept {

                    try {

                        val arenaSuccess =
                            arenaManagerFuture.join()

                        val lobbySuccess =
                            lobbyManagerFuture.join()

                        val kitEditorSuccess =
                            kitEditorManagerFuture.join()

                        logger.info(
                            "Manager initialization results: " +
                                    "arena=$arenaSuccess, " +
                                    "lobby=$lobbySuccess, " +
                                    "kitEditor=$kitEditorSuccess"
                        )

                        val allSuccess =
                            arenaSuccess &&
                                    lobbySuccess &&
                                    kitEditorSuccess

                        if (allSuccess) {

                            logger.info(
                                "All Universum managers initialized successfully."
                            )

                            server.setWhitelist(false)

                            serverManager.online()

                            update()

                            logger.info(
                                "========================================"
                            )

                            logger.info(
                                "          Universum is ONLINE           "
                            )

                            logger.info(
                                "========================================"
                            )

                        } else {

                            logger.severe(
                                "One or more Universum managers " +
                                        "failed to initialize."
                            )

                            server.setWhitelist(true)

                            serverManager.offline()
                        }

                    } catch (ex: Exception) {

                        logger.severe(
                            "Failed while finalizing Universum startup: " +
                                    ex.message
                        )

                        ex.printStackTrace()

                        server.setWhitelist(true)
                        serverManager.offline()
                    }

                }.exceptionally { ex ->

                    logger.severe(
                        "Universum startup future failed: " +
                                ex.message
                    )

                    ex.printStackTrace()

                    server.setWhitelist(true)
                    serverManager.offline()

                    null
                }

            } catch (ex: Exception) {

                logger.severe(
                    "Failed during Universum startup: " +
                            ex.message
                )

                ex.printStackTrace()

                server.setWhitelist(true)
                serverManager.offline()
            }
        }

        logger.info(
            "Universum onEnable() initialization scheduled successfully."
        )
    }

    override fun onDisable() {

        DB_Stats.shutdownAndFlush()

        RedisSubscriber.stop()
        RedisManager.close()

        dbm?.close()

        if (dbm != null) {

            logger.info(
                "Database pool safely disconnected."
            )
        }
    }

    private fun update() {

        val interval =
            config.getInt("update-interval").or(60)

        Bukkit.getScheduler().runTaskTimer(
            this,
            Runnable {
                updater.update()
            },
            10L,
            interval * 20L
        )
    }

    private fun open(): CompletableFuture<Boolean> {

        val future =
            CompletableFuture<Boolean>()

        val arenaManagerFuture =
            arenaManager.success

        val lobbyManagerFuture =
            lobbyManager.success

        val kitEditorManagerFuture =
            kitEditorManager.success

        CompletableFuture.allOf(
            arenaManagerFuture,
            lobbyManagerFuture,
            kitEditorManagerFuture
        ).thenAccept {

            val allSuccess =
                arenaManagerFuture.join() &&
                        lobbyManagerFuture.join() &&
                        kitEditorManagerFuture.join()

            future.complete(allSuccess)
        }

        return future
    }
}
