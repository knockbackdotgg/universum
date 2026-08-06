package org.tainorazum.universum.utility.other

import java.time.Duration
import java.util.regex.Pattern

fun Duration.toHumanReadable(): String {
    val days = toDays()
    val hours = toHoursPart()
    val minutes = toMinutesPart()
    val seconds = toSecondsPart()

    val parts = mutableListOf<String>()
    if (days > 0) parts.add("${days}d")
    if (hours > 0) parts.add("${hours}h")
    if (minutes > 0) parts.add("${minutes}m")
    if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")

    return parts.joinToString(" ")
}

fun parseDuration(input: String): Duration? {
    if (
        input.equals("forever", ignoreCase = true)
        || input.equals("perm", ignoreCase = true)
        || input.equals("0", ignoreCase = true)
        || input.equals("-1", ignoreCase = true)
        ) {
        return null
    }

    val matcher = Pattern.compile("(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?", Pattern.CASE_INSENSITIVE).matcher(input)
    if (!matcher.matches() || input.isBlank()) return null

    var seconds = 0L
    matcher.group(1)?.let { seconds += it.toLong() * 86400 } // Days
    matcher.group(2)?.let { seconds += it.toLong() * 3600 }  // Hours
    matcher.group(3)?.let { seconds += it.toLong() * 60 }    // Minutes
    matcher.group(4)?.let { seconds += it.toLong() }         // Seconds

    return if (seconds > 0) Duration.ofSeconds(seconds) else null
}