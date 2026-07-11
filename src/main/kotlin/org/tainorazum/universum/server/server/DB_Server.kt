package org.tainorazum.universum.server.server

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.utility.DatabaseUtil.dba
import java.util.concurrent.CompletableFuture

object DB_Server : Table("servers") {

    val identifier = varchar("identifier", 255)
    val type = varchar("type", 255)
    val public = bool("public")
    val online = bool("online")

    override val primaryKey = PrimaryKey(identifier)

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

    fun getPublic(id: String): CompletableFuture<Boolean> {
        return dba {
            transaction {
                DB_Server.selectAll()
                    .where {
                        identifier eq id
                    }
                    .map {
                        it[public]
                    }
                    .singleOrNull() ?: false
            }
        }
    }

}