package org.tainorazum.universum.server.npc.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.PrimaryKey

object DB_LobbyNPC : Table("lobby_npcs") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 64)
    val world = varchar("world", 64)
    val x = double("x")
    val y = double("y")
    val z = double("z")
    val yaw = float("yaw")
    val pitch = float("pitch")
    val skinTexture = text("skin_texture").nullable()
    val skinSignature = text("skin_signature").nullable()
    val lookAtPlayer = bool("look_at_player").default(false)
    val copyViewerSkin = bool("copy_viewer_skin").default(false)
    val created = long("created_at").clientDefault { System.currentTimeMillis() }

    override val primaryKey = PrimaryKey(id)
}

