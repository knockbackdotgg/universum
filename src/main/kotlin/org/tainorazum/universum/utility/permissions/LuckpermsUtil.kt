package org.tainorazum.universum.utility.permissions

import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.matcher.NodeMatcher
import net.luckperms.api.node.types.PermissionNode
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture

object LuckpermsUtil {

    private val lp = Universum.luckperms

    fun givePermission(
        uuid: UUID,
        permission: String,
        duration: Duration? = null,
        value: Boolean = true
    ): CompletableFuture<Boolean> {
        val luckPerms = LuckPermsProvider.get()

        return luckPerms.userManager.loadUser(uuid).thenApply { user ->
            val nodeBuilder = PermissionNode.builder(permission)
                .value(value)

            duration?.let {
                nodeBuilder.expiry(it)
            }

            val node = nodeBuilder.build()
            val result = user.data().add(node)

            if (result.wasSuccessful()) {
                luckPerms.userManager.saveUser(user)
                true
            } else {
                false
            }
        }
    }

    fun givePermission(
        player: Player,
        permission: String,
        duration: Duration? = null, // Optional duration parameter
        value: Boolean = true
    ): CompletableFuture<Boolean> {
        val luckPerms = LuckPermsProvider.get()

        return luckPerms.userManager.loadUser(player.uniqueId).thenApply { user ->
            val nodeBuilder = PermissionNode.builder(permission)
                .value(value)

            duration?.let {
                nodeBuilder.expiry(it)
            }

            val node = nodeBuilder.build()
            val result = user.data().add(node)

            if (result.wasSuccessful()) {
                luckPerms.userManager.saveUser(user)
                true
            } else {
                false
            }
        }
    }

    fun removePermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            // Find all nodes matching the permission key (ignores expiry/context differences)
            val matchingNodes = user.nodes.filter { it.key.equals(permission, ignoreCase = true) }

            if (matchingNodes.isEmpty()) {
                return@thenApply false
            }

            var removedAny = false
            for (node in matchingNodes) {
                val result = user.data().remove(node)
                if (result.wasSuccessful()) {
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
        return lp.userManager.loadUser(player.uniqueId).thenApply { user ->
            // Find all nodes matching the permission key (ignores expiry/context differences)
            val matchingNodes = user.nodes.filter { it.key.equals(permission, ignoreCase = true) }

            if (matchingNodes.isEmpty()) {
                return@thenApply false
            }

            var removedAny = false
            for (node in matchingNodes) {
                val result = user.data().remove(node)
                if (result.wasSuccessful()) {
                    removedAny = true
                }
            }

            if (removedAny) {
                lp.userManager.saveUser(user)
            }

            removedAny
        }
    }

    fun hasPermission(uuid: UUID, permission: String): CompletableFuture<Boolean> {
        return lp.userManager.loadUser(uuid).thenApply { user ->
            val node = user.nodes.asSequence()
                .filter { it.key == permission && it.value }
                .firstOrNull()
            (node != null && node.value)
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
                null // Permission doesn't exist or is permanent
            }
        }
    }

    fun getPlayersWithPermission(permission: String): CompletableFuture<List<UUID>> {
        val luckPerms = LuckPermsProvider.get()

        val matcher = NodeMatcher.key(PermissionNode.builder(permission).build())
        return luckPerms.userManager.searchAll(matcher).thenApply { results ->
            results.keys.toList()
        }
    }

}