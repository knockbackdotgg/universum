package org.tainorazum.universum.security

import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import java.util.UUID
import java.util.concurrent.CompletableFuture

enum class Permission(
    val allowed: List<Allowed>
) {

    CMD_DEFAULT_SETTINGS(listOf(Allowed.DEFAULT)),
    CMD_DEFAULT_COSMETICS(listOf(Allowed.DEFAULT)),
    CMD_DEFAULT_KITEDITOR(listOf(Allowed.DEFAULT)),
    CMD_DEFAULT_INFO(listOf(Allowed.DEFAULT)),
    CMD_DEFAULT_CYCLEHOLO(listOf(Allowed.DEFAULT)),

    CMD_UNIVERSUM_GENERAL(listOf(Allowed.OWNER)),
    CMD_UNIVERSUM_DATABASE(listOf(Allowed.OWNER)),
    CMD_UNIVERSUM_LOBBY(listOf(Allowed.OWNER)),
    CMD_UNIVERSUM_TEST(listOf(Allowed.OWNER)),
    CMD_UNIVERSUM_SERVERLIST(listOf(Allowed.OWNER)),

    CMD_ADMIN_GENERAL(listOf(Allowed.OWNER, Allowed.ADMIN)),
    CMD_ADMIN_WHITELIST(listOf(Allowed.OWNER, Allowed.ADMIN)),
    CMD_ADMIN_PUNISHMENTS(listOf(Allowed.OWNER, Allowed.ADMIN)),
    CMD_ADMIN_BYPASS(listOf(Allowed.OWNER, Allowed.ADMIN)),

    ;

    private fun checkSync(uuid: UUID): Boolean {
        return allowed.any { rule ->
            when {
                rule.uuids != null -> rule.uuids.contains(uuid)
                rule.server_permission != null -> {
                    val player = Bukkit.getPlayer(uuid)
                    player?.hasPermission(rule.server_permission) ?: false
                }
                else -> false
            }
        }
    }

    fun check(uuid: UUID): CompletableFuture<Boolean> {
        val player = Bukkit.getPlayer(uuid)

        if (player != null) {
            return CompletableFuture.completedFuture(checkSync(uuid))
        }

        return Universum.luckperms.userManager.loadUser(uuid).thenApply { user ->
            val permissionData = user.cachedData.permissionData

            allowed.any { rule ->
                when {
                    rule.uuids != null -> rule.uuids.contains(uuid)
                    rule.server_permission != null -> {
                        permissionData.checkPermission(rule.server_permission).asBoolean()
                    }
                    else -> false
                }
            }
        }
    }
}