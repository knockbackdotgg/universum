package org.tainorazum.universum.security

import java.util.UUID

enum class Allowed(
    val uuids: List<UUID>,
) {

    OWNER(
        listOf<UUID>(
            UUID.fromString("2a96d829-1fa5-42fb-a89d-aea20c66b777") // tainorazum
        )

    )

    ;

}