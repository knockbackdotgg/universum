package org.tainorazum.universum.utility.permissions

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.matcher.NodeMatcher
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.entity.Player
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

object LuckpermsUtil {

    private val lp get() = LuckPermsProvider.get()

    /**
     * Asynchronously gets the primary group weight for a player by UUID.
     */
    fun getWeight(uuid: UUID): CompletableFuture<Int> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val group = lp.groupManager.getGroup(user.primaryGroup)
            group?.weight?.orElse(0) ?: 0
        }
    }

    /**
     * Asynchronously gets the primary group weight for an online Player.
     */
    fun getWeight(player: Player): CompletableFuture<Int> {
        return getWeight(player.uniqueId)
    }

    /**
     * Asynchronously gets the raw prefix string for a player by UUID.
     */
    fun getPrefix(uuid: UUID): CompletableFuture<String?> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val queryOptions = lp.contextManager.getQueryOptions(user)
                .orElseGet { lp.contextManager.staticQueryOptions }
            user.cachedData.getMetaData(queryOptions).prefix
        }
    }

    /**
     * Asynchronously gets the raw prefix string for an online Player.
     */
    fun getPrefix(player: Player): CompletableFuture<String?> {
        return getPrefix(player.uniqueId)
    }

    fun getRankComponent(uuid: UUID): CompletableFuture<Component> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val queryOptions = lp.contextManager.getQueryOptions(user)
                .orElseGet { lp.contextManager.staticQueryOptions }

            val metaData = user.cachedData.getMetaData(queryOptions)
            val prefix = metaData.prefix ?: ""

            parsePrefix(prefix)
        }
    }

    private fun parsePrefix(prefix: String): Component {
        if (prefix.isBlank()) return Component.empty()

        return if (prefix.contains("<") && prefix.contains(">")) {
            MiniMessage.miniMessage().deserialize(prefix)
        } else {
            LegacyComponentSerializer.legacyAmpersand().deserialize(prefix)
        }
    }

    fun givePermission(
        uuid: UUID,
        permission: String,
        duration: Duration? = null,
        value: Boolean = true
    ): CompletableFuture<Boolean> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val nodeBuilder = PermissionNode.builder(permission)
                .value(value)

            duration?.let { nodeBuilder.expiry(it) }

            val node = nodeBuilder.build()
            val result = user.data().add(node)

            if (result.wasSuccessful()) {
                lp.userManager.saveUser(user)
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
            }

            removedAny
        }
    }

    fun removePermission(player: Player, permission: String): CompletableFuture<Boolean> {
        return removePermission(player.uniqueId, permission)
    }

    fun hasPermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            user.nodes.asSequence().any { it.key == permission && it.value }
        }
    }

    fun getExpiry(uuid: UUID, permission: String): CompletableFuture<Duration?> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val node = user.nodes.asSequence()
                .filter { it.key == permission && it.value }
                .firstOrNull()

            if (node != null && node.hasExpiry()) {
                node.expiryDuration
            } else {
                null
            }
        }
    }

    fun getPlayersWithPermission(permission: String): CompletableFuture<List<UUID>> {
        val matcher = NodeMatcher.key(PermissionNode.builder(permission).build())
        return lp.userManager.searchAll(matcher).thenApply { results ->
            results.keys.toList()
        }
    }
}