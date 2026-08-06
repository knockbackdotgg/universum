package org.tainorazum.universum.server.visual

import org.jetbrains.exposed.sql.Table

object DB_VisualServer : Table("visual_server") {

    val identifier = varchar("identifier", 100) // name of the preset

    // Editable MOTD (up to 255 chars) and a favicon blob
    val motd = varchar("motd", 255).default("")
    val favicon = blob("favicon").nullable()

    override val primaryKey = PrimaryKey(identifier)
}