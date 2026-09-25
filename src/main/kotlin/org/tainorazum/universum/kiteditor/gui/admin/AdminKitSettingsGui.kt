package org.tainorazum.universum.kiteditor.gui.admin

import gg.flyte.twilight.gui.GUI
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.tainorazum.universum.Universum
import org.tainorazum.universum.engine.game.manager.fields.GameSettings
import org.tainorazum.universum.engine.kit.Kit
import org.tainorazum.universum.engine.kit.database.DB_GameSettings
import org.tainorazum.universum.utility.gui.GuiUtility
import org.tainorazum.universum.utility.other.open
import org.tainorazum.universum.utility.text.Colours
import org.tainorazum.universum.utility.text.Text
import org.tainorazum.universum.utility.text.norm
import java.util.concurrent.CompletableFuture

object AdminKitSettingsGui {

    private val PATTERN = listOf(
        "#########",
        "#ABCDEFG#",
        "#HIJKLMN#",
        "#OPQRSTU#",
        "##VWXYZ##",
        "B########"
    )

    /**
     * A = ROUNDS
     * B = CAN MODIFY BLOCKS
     * C = CAN MODIFY MAP
     * D = CHANGE FOOD LEVEL
     * E = DEFAULT FOOD LEVEL
     * F = MAX HEALTH
     * H = DEFAULT HEALTH
     * I =
     * J =
     * K =
     * L =
     * M =
     * N =
     * O =
     * P =
     * Q =
     * R =
     * S =
     * T =
     * U =
     * V =
     * W =
     * X =
     * Y =
     * Z =
     */

    fun gui(
        player: Player,
        kit: Kit,
        previous: GUI? = null
    ): CompletableFuture<GUI> {

        return DB_GameSettings.get(kit.id).thenApply { settings ->

            gg.flyte.twilight.gui.gui(
                Text.toSmallTextComponent(
                    "Kit Settings » ${kit.id}",
                    Colours.GREY
                ),
                54
            ) {

                pattern(*PATTERN.toTypedArray())


                /*
                 * ROUNDS
                 */
                set(
                    'A',
                    settingItem(
                        Material.CLOCK,
                        "Rounds",
                        "${settings.rounds}"
                    )
                ) {
                    isCancelled = true

                    val newSettings = settings.copy(
                        rounds = if (settings.rounds >= 10) 1 else settings.rounds + 1
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }

                /*
                 * BLOCK MODIFICATION
                 */
                set(
                    'B',
                    settingItem(
                        Material.GRASS_BLOCK,
                        "Modify Blocks",
                        enabledText(settings.canModifyBlocks)
                    )
                ) {
                    isCancelled = true

                    val newSettings = settings.copy(
                        canModifyBlocks = !settings.canModifyBlocks
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }

                /*
                 * MAP MODIFICATION
                 */
                set(
                    'C',
                    settingItem(
                        Material.GRASS_BLOCK,
                        "Modify Map",
                        enabledText(settings.canModifyMap)
                    )
                ) {
                    isCancelled = true

                    val newSettings = settings.copy(
                        canModifyMap = !settings.canModifyMap
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }

                /*
                 * HUNGER
                 */

                set(
                    'D',
                    settingItem(
                        Material.GRASS_BLOCK,
                        "Change Food Level",
                        enabledText(settings.changeFoodLevel)
                    )
                ) {
                    isCancelled = true

                    val newSettings = settings.copy(
                        changeFoodLevel = !settings.changeFoodLevel
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }

                set(
                    'E',
                    settingItem(
                        Material.GOLDEN_APPLE,
                        "Default Food Level",
                        "${settings.defaultFoodLevel} Food Level",
                    )
                ) {
                    isCancelled = true

                    val newFoodLevel =
                        when (settings.defaultFoodLevel) {
                            0 -> 2
                            2 -> 4
                            4 -> 6
                            6 -> 8
                            8 -> 10
                            10 -> 12
                            12 -> 14
                            14 -> 16
                            16 -> 18
                            18 -> 20
                            else -> 20
                        }

                    val newSettings = settings.copy(
                        defaultFoodLevel = newFoodLevel,
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }

                /*
                 * HEALTH
                 */
                set(
                    'G',
                    settingItem(
                        Material.GOLDEN_APPLE,
                        "Default Health",
                        "${settings.defaultHealth} HP",
                        "Max Health: ${settings.maxHealth} HP"
                    )
                ) {
                    isCancelled = true

                    val newHealth =
                        when (settings.defaultHealth) {
                            10 -> 20
                            20 -> 40
                            40 -> 10
                            else -> 20
                        }

                    val newSettings = settings.copy(
                        defaultHealth = newHealth,
                        maxHealth = maxOf(settings.maxHealth, newHealth)
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }

                set(
                    'F',
                    settingItem(
                        Material.GOLDEN_APPLE,
                        "Max Health",
                        "${settings.defaultHealth} HP",
                        "Max Health: ${settings.maxHealth} HP"
                    )
                ) {
                    isCancelled = true

                    val newHealth =
                        when (settings.maxHealth) {
                            10 -> 20
                            20 -> 40
                            40 -> 10
                            else -> 20
                        }

                    val newSettings = settings.copy(
                        defaultHealth = newHealth,
                        maxHealth = maxOf(settings.maxHealth, newHealth)
                    )

                    saveAndRefresh(
                        player,
                        kit,
                        newSettings,
                        previous
                    )
                }


                /*
                 * BACK
                 */
                set(
                    'B',
                    GuiUtility.item(
                        Material.ARROW,
                        Text.toSmallTextComponent(
                            if (previous != null) "Go Back" else "Close",
                            Colours.RED
                        ),
                        "Click to return."
                    )
                ) {
                    isCancelled = true

                    if (previous != null) {
                        previous.open(player)
                    } else {
                        player.closeInventory()
                    }
                }
            }
        }
    }


    private fun settingItem(
        material: Material,
        name: String,
        vararg values: String
    ) =
        GuiUtility.item(
            material,
            Text.toSmallTextComponent(
                name,
                Colours.YELLOW
            ),
            values.map {
                Component.text(it, Colours.GREY).norm()
            }
        )


    private fun enabledText(enabled: Boolean): String =
        if (enabled) "Enabled" else "Disabled"


    private fun saveAndRefresh(
        player: Player,
        kit: Kit,
        settings: GameSettings,
        previous: GUI?
    ) {

        DB_GameSettings.save(
            kit.id,
            settings
        ).thenAccept {

            Bukkit.getScheduler().runTask(
                Universum.instance,
                Runnable {
                    openGUI(
                        player,
                        kit,
                        previous
                    )
                }
            )
        }
    }


    fun openGUI(
        player: Player,
        kit: Kit,
        previous: GUI? = null
    ) {

        gui(
            player,
            kit,
            previous
        ).thenAccept { gui ->

            Bukkit.getScheduler().runTask(
                Universum.instance,
                Runnable {
                    gui.open(player)
                }
            )
        }
    }
}