package org.tainorazum.universum.utility.database

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.engine.kit.database.DB_Kits
import java.util.concurrent.CompletableFuture

object DatabaseUtil {

    /**
     * @usage Database async, CompletableFuture shortcut.
     */
    fun <T> dba(block: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync {
            transaction {
                block()
            }
        }
    }

    /**
     * @usage Main thread but still async, shortcut
     */
    fun <T> mta(block: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        val plugin = JavaPlugin.getProvidingPlugin(DB_Kits::class.java)

        Bukkit.getScheduler().runTask(plugin, Runnable {
            try {
                future.complete(block())
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        })

        return future
    }

    fun disableForeignKeyChecks(): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    exec("SET FOREIGN_KEY_CHECKS = 0;")
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun enableForeignKeyChecks(): CompletableFuture<Boolean> {
        return dba {
            try {
                transaction {
                    exec("SET FOREIGN_KEY_CHECKS = 1;")
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun dropTableByName(tableName: String): CompletableFuture<Boolean> {
        return dba {
            transaction {
                try {
                    exec("SET FOREIGN_KEY_CHECKS = 0;")
                    exec("DROP TABLE IF EXISTS `$tableName`")
                    exec("SET FOREIGN_KEY_CHECKS = 1;")
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

}