package org.tainorazum.universum.redis.listeners.games

import org.bukkit.Bukkit
import org.tainorazum.universum.Universum
import org.tainorazum.universum.redis.RedisSubscriber
import org.tainorazum.universum.redis.listeners.RM_Channel

class RM_GameRequest : RM_Channel("game_request") {

    init {
        RedisSubscriber.register(channel) { requestId ->
            Universum.instance.logger.info("[Redis] Received game request publish event for ID: $requestId")

            Universum.gameManager.claimRequest(requestId).thenAccept { request ->
                if (request != null) {
                    Universum.instance.logger.info("[Redis] Successfully claimed request $requestId. Creating game instance...")

                    Bukkit.getScheduler().runTask(Universum.instance, Runnable {
                        Universum.gameManager.createGame(request).thenAccept { game ->
                            if (game == null) {
                                Universum.instance.logger.warning("[Redis] Failed to create game for request: $requestId (createGame returned null)")
                            } else {
                                Universum.instance.logger.info("[Redis] Successfully created game instance for request: $requestId")
                            }
                        }.exceptionally { ex ->
                            Universum.instance.logger.severe("[Redis] Error during createGame handling:")
                            ex.printStackTrace()
                            null
                        }
                    })
                } else {
                    Universum.instance.logger.info("[Redis] Request $requestId was not claimed by this server (or key expired).")
                }
            }.exceptionally { ex ->
                Universum.instance.logger.severe("[Redis] Error processing claimRequest for $requestId:")
                ex.printStackTrace()
                null
            }
        }
    }
}