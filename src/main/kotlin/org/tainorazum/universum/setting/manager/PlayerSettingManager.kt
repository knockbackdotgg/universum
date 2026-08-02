package org.tainorazum.universum.setting.manager

import org.bukkit.entity.Player
import org.tainorazum.universum.setting.DB_PlayerSettings
import org.tainorazum.universum.setting.PlayerSetting
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerSettingManager {

    fun cacheSettings(uuid: UUID): CompletableFuture<Unit> {
        return DB_PlayerSettings.cacheAllSettings(uuid)
    }

}