package org.tainorazum.universum.server.server

import org.jetbrains.exposed.sql.SqlExpressionBuilder.coalesce
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.booleanLiteral
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import org.tainorazum.universum.Universum
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

object DB_Server : Table("servers") {

    val identifier = varchar("identifier", 255)
    val type = varchar("type", 255)
    val public = bool("public")
    val online = bool("online")

    override val primaryKey = PrimaryKey(identifier)

    @Volatile
    private var cachedPublic: Boolean? = null

    fun currentPublic(): Boolean? = cachedPublic

    fun register(id: String, type: ServerType): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Server.upsert {
                    it[DB_Server.identifier] = id
                    it[DB_Server.type] = type.name

                    it[DB_Server.public] = coalesce(DB_Server.public, booleanLiteral(true))
                    it[DB_Server.online] = coalesce(DB_Server.online, booleanLiteral(true))
                }
                true
            }
        }.thenApply { success ->
            if (success) {
                cachedPublic = null
            }
            success
        }
    }

    fun preloadPublic(id: String): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Server.selectAll()
                    .where { DB_Server.identifier eq id }
                    .map { it[public] }
                    .singleOrNull() ?: false
            }
        }.thenApply { value ->
            cachedPublic = value
            value
        }
    }

    fun update(id: String, type: ServerType, public: Boolean, online: Boolean): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Server.deleteWhere {
                    DB_Server.identifier eq id
                }
                DB_Server.insert {
                    it[DB_Server.identifier] = id
                    it[DB_Server.type] = type.name
                    it[DB_Server.public] = public
                    it[DB_Server.online] = online
                }
                true
            }
        }.thenApply { success ->
            if (success) {
                cachedPublic = public
            }
            success
        }
    }

    fun getServer(id: String): CompletableFuture<Server?> {
        return dba {
            transaction {
                DB_Server.selectAll()
                    .where {
                        identifier eq id
                    }
                    .map {
                        Server(
                            it[identifier],
                            ServerType.valueOf(it[type])
                        )
                    }
                    .singleOrNull()
            }
        }
    }

    /*
     val exists = DB_Lobby.selectAll()
                    .where { DB_Lobby.spawnpoint_world eq worldName }
                    .count() > 0

                if (exists) {
                    DB_Lobby.update(
                        where = { DB_Lobby.spawnpoint_world eq worldName }
                    ) {
                        it[center_x] = location.x
                        it[center_y] = location.y
                        it[center_z] = location.z
                    }
                } else {
                    DB_Lobby.insert {
                        it[spawnpoint_world] = worldName
     */

    /**
     * @return Always returns true.
     */
    fun setPublic(id: String, bool: Boolean): CompletableFuture<Boolean> {
        return dba {
            transaction {
                cachedPublic = bool
                val exists = DB_Server.selectAll()
                    .where { DB_Server.identifier eq id }
                    .count() > 0
                if(exists) {
                    DB_Server.update(where = { DB_Server.identifier eq id}) {
                        it[public] = bool
                    }
                    true
                } else {
                    DB_Server.insert {
                        it[identifier] = id
                        it[type] = Universum.server.type.name
                        it[public] = bool
                        it[online] = true
                    }
                    true
                }
            }
        }.thenApply { success ->
            if (success) {
                cachedPublic = bool
            }
            success
        }
    }

    fun getPublic(id: String): CompletableFuture<Boolean> {
        cachedPublic?.let { return CompletableFuture.completedFuture(it) }
        return preloadPublic(id)
    }

}
