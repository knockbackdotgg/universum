package org.tainorazum.universum.friends.database

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.tainorazum.universum.utility.database.DatabaseUtil.dba
import java.util.UUID
import java.util.concurrent.CompletableFuture

object DB_Friends : Table("friends") {

    val uuid = varchar("uuid", 36)
    val uuid_friend = varchar("uuid_friend", 36)

    override val primaryKey = PrimaryKey(uuid, uuid_friend)

    fun addFriend(playerUUID: UUID, friendUUID: UUID): CompletableFuture<Boolean> {
        val p1 = playerUUID.toString()
        val p2 = friendUUID.toString()
        return dba {
            transaction {
                DB_Friends.insertIgnore {
                    it[uuid] = p1
                    it[uuid_friend] = p2
                }
                DB_Friends.insertIgnore {
                    it[uuid] = p2
                    it[uuid_friend] = p1
                }
                true
            }
        }
    }

    fun removeFriend(playerUUID: UUID, friendUUID: UUID): CompletableFuture<Boolean> {
        val p1 = playerUUID.toString()
        val p2 = friendUUID.toString()
        return dba {
            transaction {
                DB_Friends.deleteWhere {
                    ((uuid eq p1) and (uuid_friend eq p2)) or
                            ((uuid eq p2) and (uuid_friend eq p1))
                } > 0
            }
        }
    }

    fun isFriend(uuid1: UUID, uuid2: UUID): CompletableFuture<Boolean> {
        val p1 = uuid1.toString()
        val p2 = uuid2.toString()
        return dba {
            transaction {
                DB_Friends.selectAll()
                    .where {
                        ((uuid eq p1) and (uuid_friend eq p2)) or
                                ((uuid eq p2) and (uuid_friend eq p1))
                    }.count() > 0
            }
        }
    }

    fun getFriends(playerUUID: UUID): CompletableFuture<List<UUID>> {
        val pUuid = playerUUID.toString()
        return dba {
            transaction {
                DB_Friends.selectAll()
                    .where { uuid eq pUuid }
                    .map { UUID.fromString(it[uuid_friend]) }
            }
        }
    }

    fun removeAllFriends(playerUUID: UUID): CompletableFuture<Boolean> {
        val playerUuidString = playerUUID.toString()
        return dba {
            transaction {
                DB_Friends.deleteWhere {
                    (uuid eq playerUuidString) or (uuid_friend eq playerUuidString)
                } > 0
            }
        }
    }
}