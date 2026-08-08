package org.tainorazum.universum.utility.permissions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.event.group.GroupDataRecalculateEvent
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.luckperms.api.node.Node
import net.luckperms.api.node.matcher.NodeMatcher
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.entity.Player
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object LuckpermsUtil {

    private val lp get() = LuckPermsProvider.get()

    /**
     * Holds cached values for a user.
     */
    data class CachedUserData(
        val weight: Int,
        val prefix: String?,
        val rankComponent: Component,
        val nodes: List<Node>
    )

    // Cache stored as futures to prevent duplicate concurrent network/DB loads for the same UUID
    private val userCache = ConcurrentHashMap<UUID, CompletableFuture<CachedUserData>>()

    /**
     * Lazily registers LuckPerms event listeners for automatic cache invalidation.
     */
    private val registerEvents by lazy {
        val bus = lp.eventBus

        // Invalidate specific user cache when their permissions, groups, or metadata recalculate
        bus.subscribe(UserDataRecalculateEvent::class.java) { event ->
            invalidate(event.user.uniqueId)
        }

        // Invalidate all user caches if a group's data/inheritance is recalculated
        bus.subscribe(GroupDataRecalculateEvent::class.java) { _ ->
            invalidateAll()
        }
    }

    private fun ensureEventsRegistered() {
        registerEvents
    }

    /**
     * Explicitly invalidates the cache for a specific player UUID.
     */
    fun invalidate(uuid: UUID) {
        userCache.remove(uuid)
    }

    /**
     * Explicitly invalidates the entire cache.
     */
    fun invalidateAll() {
        userCache.clear()
    }

    /**
     * Fetches cached user data, or loads and caches it from LuckPerms if missing/invalidated.
     */
    private fun getUserData(uuid: UUID): CompletableFuture<CachedUserData> {
        ensureEventsRegistered()

        return userCache.computeIfAbsent(uuid) { id ->
            val future = lp.userManager.loadUser(id).thenApply { user ->
                val queryOptions = lp.contextManager.getQueryOptions(user)
                    .orElseGet { lp.contextManager.staticQueryOptions }

                val metaData = user.cachedData.getMetaData(queryOptions)
                val prefix = metaData.prefix
                val group = lp.groupManager.getGroup(user.primaryGroup)
                val weight = group?.weight?.orElse(0) ?: 0
                val rankComp = parsePrefix(prefix ?: "")
                val nodes = user.nodes.toList()

                CachedUserData(weight, prefix, rankComp, nodes)
            }

            // Remove from cache if loading failed to allow retries
            future.whenComplete { _, throwable ->
                if (throwable != null) {
                    userCache.remove(id)
                }
            }

            future
        }
    }

    // =========================================================================
    // Getters (Utilizes Cache)
    // =========================================================================

    fun getWeight(uuid: UUID): CompletableFuture<Int> {
        return getUserData(uuid).thenApply { it.weight }
    }

    fun getWeight(player: Player): CompletableFuture<Int> {
        return getWeight(player.uniqueId)
    }

    fun getPrefix(uuid: UUID): CompletableFuture<String?> {
        return getUserData(uuid).thenApply { it.prefix }
    }

    fun getPrefix(player: Player): CompletableFuture<String?> {
        return getPrefix(player.uniqueId)
    }

    fun getRankComponent(uuid: UUID): CompletableFuture<Component> {
        return getUserData(uuid).thenApply { it.rankComponent }
    }

    fun hasPermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return getUserData(uuid).thenApply { data ->
            data.nodes.any { it.key.equals(permission, ignoreCase = true) && it.value && !it.hasExpired() }
        }
    }

    fun getExpiry(uuid: UUID, permission: String): CompletableFuture<Duration?> {
        return getUserData(uuid).thenApply { data ->
            val node = data.nodes.firstOrNull {
                it.key.equals(permission, ignoreCase = true) && it.value && !it.hasExpired()
            }

            if (node != null && node.hasExpiry()) {
                node.expiryDuration
            } else {
                null
            }
        }
    }

    // =========================================================================
    // Mutators (Invalidates Cache On Change)
    // =========================================================================

    fun givePermission(
        uuid: UUID,
        permission: String,
        duration: Duration? = null,
        value: Boolean = true
    ): CompletableFuture<Boolean> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val nodeBuilder = PermissionNode.builder(permission).value(value)
            duration?.let { nodeBuilder.expiry(it) }

            val node = nodeBuilder.build()
            val result = user.data().add(node)

            if (result.wasSuccessful()) {
                lp.userManager.saveUser(user)
                invalidate(uuid) // Invalidate cache immediately
                true
            } else {
                false
            }
        }
    }

    fun givePermission(
        player: Player,
        permission: String,
        duration: Duration? = null,
        value: Boolean = true
    ): CompletableFuture<Boolean> {
        return givePermission(player.uniqueId, permission, duration, value)
    }

    fun removePermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val matchingNodes = user.nodes.filter { it.key.equals(permission, ignoreCase = true) }

            if (matchingNodes.isEmpty()) return@thenApply false

            var removedAny = false
            for (node in matchingNodes) {
                if (user.data().remove(node).wasSuccessful()) {
                    removedAny = true
                }
            }

            if (removedAny) {
                lp.userManager.saveUser(user)
                invalidate(uuid) // Invalidate cache immediately
            }

            removedAny
        }
    }

    fun removePermission(player: Player, permission: String): CompletableFuture<Boolean> {
        return removePermission(player.uniqueId, permission)
    }

    fun getPlayersWithPermission(permission: String): CompletableFuture<List<UUID>> {
        val matcher = NodeMatcher.key(PermissionNode.builder(permission).build())
        return lp.userManager.searchAll(matcher).thenApply { results ->
            results.keys.toList()
        }
    }

    // =========================================================================
    // Internal Helpers
    // =========================================================================

    private fun parsePrefix(prefix: String): Component {
        if (prefix.isBlank()) return Component.empty()

        return if (prefix.contains("<") && prefix.contains(">")) {
            MiniMessage.miniMessage().deserialize(prefix)
        } else {
            LegacyComponentSerializer.legacyAmpersand().deserialize(prefix)
        }
    }
}