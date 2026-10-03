package org.tainorazum.universum.security

import java.util.UUID

enum class Allowed(
    val uuids: List<UUID>? = null,
    val server_permission: String? = null,
) { // uuids OR server_permission, not both

    DEFAULT(null, "universum.allowed.default"), // player rank

    PAID1(null, "universum.allowed.paid1"), // player rank

    PAID2(null, "universum.allowed.paid2"), // player rank

    PAID3(null, "universum.allowed.paid3"), // player rank

    STAFF(null, "universum.allowed.staff"), // staff rank

    HELPER(null, "universum.allowed.helper"), // staff rank

    MODERATOR(null, "universum.allowed.moderator"), // staff rank

    ADMIN(null, "universum.allowed.admin"),

    /*OWNER(listOf<UUID>(
            UUID.fromString("2a96d829-1fa5-42fb-a89d-aea20c66b777") // tainorazum
        ),
        null
    )*/
    OWNER(null, "universum.allowed.owner"), // staff rank

    ;

}