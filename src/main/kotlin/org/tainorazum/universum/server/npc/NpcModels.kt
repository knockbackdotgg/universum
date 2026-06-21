package org.tainorazum.universum.server.npc

data class NpcData(
    val id: Long,
    val name: String,
    val world: String,
    val x: Double, val y: Double, val z: Double,
    val yaw: Float, val pitch: Float,
    val skinTexture: String?, val skinSignature: String?,
    val lookAtPlayer: Boolean, val copyViewerSkin: Boolean
)

data class NpcActionModel(
    val id: Long,
    val npcId: Long,
    val actionType: String,
    val actionData: String,
    val order: Int
)

data class HologramLine(
    val id: Long,
    val npcId: Long,
    val lineIndex: Int,
    val type: String,
    val content: String,
    val offsetY: Double
)
