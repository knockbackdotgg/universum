package org.tainorazum.universum.setting.manager

import org.tainorazum.universum.setting.database.DB_PlayerSettings
import java.util.UUID
import java.util.concurrent.CompletableFuture

class PlayerSettingManager {

    fun cacheSettings(uuid: UUID): CompletableFuture<Unit> {
        return DB_PlayerSettings.cacheAllSettings(uuid)
    }

    fun getSetting(uuid: UUID, setting: PlayerSetting): CompletableFuture<String> {
        return DB_PlayerSettings.getSetting(uuid, setting)
    }

}