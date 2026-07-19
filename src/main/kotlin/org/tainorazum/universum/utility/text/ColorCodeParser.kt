package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

/**
 * Parses legacy color codes (&c, &a, etc.) and formatting codes into Adventure Components.
 * Supports:
 * - Color codes: &0-&9, &a-&f
 * - Formatting: &l (bold), &m (strikethrough), &n (underline), &o (italic), &r (reset)
 */
object ColorCodeParser {

    private val colorMap = mapOf(
        '0' to NamedTextColor.BLACK,
        '1' to NamedTextColor.DARK_BLUE,
        '2' to NamedTextColor.DARK_GREEN,
        '3' to NamedTextColor.DARK_AQUA,
        '4' to NamedTextColor.DARK_RED,
        '5' to NamedTextColor.DARK_PURPLE,
        '6' to NamedTextColor.GOLD,
        '7' to NamedTextColor.GRAY,
        '8' to NamedTextColor.DARK_GRAY,
        '9' to NamedTextColor.BLUE,
        'a' to NamedTextColor.GREEN,
        'b' to NamedTextColor.AQUA,
        'c' to NamedTextColor.RED,
        'd' to NamedTextColor.LIGHT_PURPLE,
        'e' to NamedTextColor.YELLOW,
        'f' to NamedTextColor.WHITE
    )

    /**
     * Parse a string with legacy color codes (&c, &a, etc.) into an Adventure Component.
     * Example: "&cRed &aGreen &lBold" becomes a Component with proper formatting.
     */
    fun parse(text: String): Component {
        if (text.isEmpty()) return Component.text("")

        val result = mutableListOf<Component>()
        var currentText = StringBuilder()
        var currentColor: NamedTextColor? = null
        val currentDecorations = mutableSetOf<TextDecoration>()
        
        var i = 0
        while (i < text.length) {
            if (i < text.length - 1 && text[i] == '&') {
                val code = text[i + 1].lowercaseChar()
                
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

                when (code) {
                    in colorMap -> {
                        currentColor = colorMap[code]
                        currentDecorations.clear()
                    }
                    'l' -> currentDecorations.add(TextDecoration.BOLD)
                    'm' -> currentDecorations.add(TextDecoration.STRIKETHROUGH)
                    'n' -> currentDecorations.add(TextDecoration.UNDERLINED)
                    'o' -> currentDecorations.add(TextDecoration.ITALIC)
                    'r' -> {
                        currentColor = null
                        currentDecorations.clear()
                    }
                    '&' -> currentText.append('&')
                    else -> {
                        currentText.append('&').append(code)
                    }
                }
                i += 2
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
        color: NamedTextColor?,
        decorations: Set<TextDecoration>
    ): Component {
        var result = component
        color?.let { result = result.color(it) }
        decorations.forEach { result = result.decorate(it) }
        return result
    }
}

