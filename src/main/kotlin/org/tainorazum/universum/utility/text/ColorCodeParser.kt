package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

object ColorCodeParser {

    private val colorMap = mapOf(
        '1' to Colours.SKY_BLUE,
        '2' to Colours.DARK_GREEN,
        '5' to Colours.PURPLE,
        '6' to Colours.GOLD,
        '7' to Colours.GREY,
        '8' to Colours.DARK_GREY,
        '9' to Colours.BLUE,
        'a' to Colours.GREEN,
        'b' to Colours.AQUA,
        'c' to Colours.RED,
        'd' to Colours.LIGHT_PURPLE,
        'e' to Colours.YELLOW,
        'f' to Colours.WHITE
    )

    fun parse(text: String): Component {
        if (text.isEmpty()) return Component.text("")

        val result = mutableListOf<Component>()
        var currentText = StringBuilder()

        // Default color starts as WHITE so reset (&r) cleanly defaults to white
        var currentColor: TextColor? = null
        val currentDecorations = mutableSetOf<TextDecoration>()

        var i = 0
        while (i < text.length) {
            if (i < text.length - 1 && text[i] == '&') {
                val rawCode = text[i + 1]
                val code = rawCode.lowercaseChar()

                // If it's an escaped '&&', append single '&' and advance by 2
                if (rawCode == '&') {
                    currentText.append('&')
                    i += 2
                    continue
                }

                // Check if valid color/decoration code
                val isColor = code in colorMap
                val isFormat = code in setOf('l', 'm', 'n', 'o', 'r')

                if (isColor || isFormat) {
                    // Flush current text as a component
                    if (currentText.isNotEmpty()) {
                        val component = addFormattingToComponent(
                            Component.text(currentText.toString()),
                            currentColor,
                            currentDecorations
                        )
                        result.add(component)
                        currentText = StringBuilder()
                    }

                    when {
                        isColor -> {
                            currentColor = colorMap[code]
                            // Standard Minecraft behavior: color code clears decorations
                            currentDecorations.clear()
                        }
                        code == 'l' -> currentDecorations.add(TextDecoration.BOLD)
                        code == 'm' -> currentDecorations.add(TextDecoration.STRIKETHROUGH)
                        code == 'n' -> currentDecorations.add(TextDecoration.UNDERLINED)
                        code == 'o' -> currentDecorations.add(TextDecoration.ITALIC)
                        code == 'r' -> {
                            // Reset explicitly sets color to WHITE to break inheritance
                            currentColor = NamedTextColor.WHITE
                            currentDecorations.clear()
                        }
                    }
                    i += 2
                } else {
                    // Not a valid color code, treat '&' as literal text
                    currentText.append(text[i])
                    i++
                }
            } else {
                currentText.append(text[i])
                i++
            }
        }

        // Add remaining text
        if (currentText.isNotEmpty()) {
            val component = addFormattingToComponent(
                Component.text(currentText.toString()),
                currentColor,
                currentDecorations
            )
            result.add(component)
        }

        return if (result.isEmpty()) {
            Component.text("")
        } else {
            result.reduce { acc, comp -> acc.append(comp) }
        }
    }

    private fun addFormattingToComponent(
        component: Component,
        color: TextColor?,
        decorations: Set<TextDecoration>
    ): Component {
        var result = component
        color?.let { result = result.color(it) }

        // Ensure decorations not in the active set are explicitly turned off
        TextDecoration.entries.forEach { dec ->
            if (decorations.contains(dec)) {
                result = result.decorate(dec)
            } else {
                result = result.decoration(dec, TextDecoration.State.FALSE)
            }
        }
        return result
    }
}