package org.tainorazum.universum.chat

import org.bukkit.configuration.file.YamlConfiguration
import org.tainorazum.universum.Universum
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

class ChatFilterManager {

    private val configFile = File(Universum.instance.dataFolder, "chatfilter.yml")
    private var blockedPatterns: List<Pattern> = emptyList()

    init {
        loadFilterConfig()
    }

    fun loadFilterConfig() {
        if (!configFile.exists()) {
            Universum.instance.saveResource("chatfilter.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(configFile)
        val rawWords = config.getStringList("blocked-words")

        blockedPatterns = rawWords.mapNotNull { word ->
            val cleaned = word.trim().lowercase()
            if (cleaned.isEmpty()) null else buildWordRegex(cleaned)
        }
    }

    fun filter(uuid: UUID, message: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            val normalizedMessage = normalizeText(message)

            for (pattern in blockedPatterns) {
                if (pattern.matcher(normalizedMessage).find()) {
                    return@supplyAsync true
                }
            }

            val deduplicatedMessage = removeDuplicates(normalizedMessage)
            for (pattern in blockedPatterns) {
                if (pattern.matcher(deduplicatedMessage).find()) {
                    return@supplyAsync true
                }
            }

            false
        }
    }

    private fun normalizeText(text: String): String {
        var lower = text.lowercase()

        // Leetspeak replacements
        lower = lower
            .replace('@', 'a')
            .replace('4', 'a')
            .replace('8', 'b')
            .replace('3', 'e')
            .replace('1', 'i')
            .replace('!', 'i')
            .replace('|', 'i')
            .replace('0', 'o')
            .replace('5', 's')
            .replace('$', 's')
            .replace('7', 't')
            .replace('v', 'u')

        return lower
    }

    private fun removeDuplicates(text: String): String {
        val builder = StringBuilder()
        var lastChar = ' '

        for (ch in text) {
            if (ch != lastChar) {
                builder.append(ch)
                lastChar = ch
            }
        }
        return builder.toString()
    }

    private fun buildWordRegex(word: String): Pattern {
        val regexBuilder = StringBuilder()

        for (i in word.indices) {
            val char = word[i]

            regexBuilder.append(Pattern.quote(char.toString())).append("+")

            if (i < word.length - 1) {
                regexBuilder.append("[\\s._\\-*]*")
            }
        }

        return Pattern.compile(regexBuilder.toString(), Pattern.CASE_INSENSITIVE)
    }
}