package org.tainorazum.universum.server.npc.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.PrimaryKey

object DB_NPCHologram : Table("npc_holograms") {
    val id = long("id").autoIncrement()
    val npcId = long("npc_id")
    val lineIndex = integer("line_index")
    val type = varchar("type", 16).default("TEXT")
    val content = text("content")
    val offsetY = double("offset_y").default(0.0)

    override val primaryKey = PrimaryKey(id)
}

