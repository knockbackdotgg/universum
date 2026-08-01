package org.tainorazum.universum.security

import java.util.UUID

enum class Permission(
    val allowed: List<Allowed>
) {

    CMD_UNIVERSUM_ALL(
        listOf(Allowed.OWNER)
    )


    ;

    fun check(uuid: UUID) = allowed.any { it.uuids.contains(uuid) }
}