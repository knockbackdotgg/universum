package org.tainorazum.universum.utility.text

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration

fun Component.norm(): Component {
    return this.decoration(TextDecoration.ITALIC, false)
}