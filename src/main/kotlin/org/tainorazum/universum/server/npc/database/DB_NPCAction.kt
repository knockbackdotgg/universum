package org.tainorazum.universum.server.npc.database

import org.jetbrains.exposed.sql.Table

object DB_NPCAction : Table("npc_actions") {
    val id = long("id").autoIncrement()
    val npcId = long("npc_id")
    val actionType = varchar("action_type", 32)
    val actionData = text("action_data")
    val order = integer("order").default(0)

    override val primaryKey = PrimaryKey(id)
}

