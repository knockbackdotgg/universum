package org.tainorazum.universum.server.server

import java.util.concurrent.CompletableFuture

data class Server(
    val identifier: String,
    val type: ServerType,

) {

    val public: CompletableFuture<Boolean> get() = DB_Server.getPublic(identifier)

}