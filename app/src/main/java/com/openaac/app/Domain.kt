package com.openaac.app

import java.util.Locale

data class WordEvent(
    val at: Long,
    val word: String,
)

object RecommendationEngine {
    fun recommend(
        lastWord: String?,
        currentBoard: String?,
        visibleButtons: List<VocabButton>,
        boards: Map<String, List<VocabButton>>,
        usageCounts: Map<String, Int>,
        transitionCounts: Map<Pair<String, String>, Int>,
    ): RecommendationResult {
        val allButtons = (boards.values.flatten() + Defaults.pinned)
            .distinctBy { it.label.normalizedLabel() }
            .associateBy { it.label.normalizedLabel() }
        val scores = linkedMapOf<String, RecommendationScore>()

        if (!lastWord.isNullOrBlank()) {
            val from = lastWord.normalizedLabel()
            transitionCounts.entries.asSequence()
                .filter { (transition, _) -> transition.first == from }
                .sortedByDescending { (_, count) -> count }
                .forEach { (transition, count) ->
                    allButtons[transition.second]?.let { button ->
                        scores[button.label.normalizedLabel()] = RecommendationScore(
                            button = button,
                            score = count * 10 + 50,
                            reason = "frequent after $lastWord",
                        )
                    }
                }
        }

        ruleFallback(lastWord, currentBoard).forEachIndexed { index, label ->
            allButtons[label.normalizedLabel()]?.let { button ->
                scores.putIfAbsent(button.label.normalizedLabel(), RecommendationScore(button, 40 - index, "common path"))
            }
        }

        visibleButtons.take(5).forEachIndexed { index, button ->
            scores.putIfAbsent(button.label.normalizedLabel(), RecommendationScore(button, 25 - index, "on this board"))
        }

        usageCounts.entries.asSequence()
            .mapNotNull { (key, count) -> allButtons[key.normalizedLabel()]?.let { RecommendationScore(it, count, "frequently used") } }
            .sortedByDescending { it.score }
            .forEach { scores.putIfAbsent(it.button.label.normalizedLabel(), it.copy(score = it.score + 10)) }

        val recommendations = scores.values
            .sortedByDescending { it.score }
            .take(5)
            .map { it.button }
        val totalTaps = usageCounts.values.sum()
        val totalTransitions = transitionCounts.values.sum()
        val status = when {
            totalTransitions >= 40 -> "learning from regular use"
            totalTransitions >= 12 -> "starting to personalize"
            totalTaps >= 8 -> "collecting patterns"
            else -> "starter suggestions"
        }
        return RecommendationResult(recommendations, status)
    }

    private fun ruleFallback(lastWord: String?, currentBoard: String?): List<String> {
        return when (lastWord?.normalizedLabel()) {
            "i" -> listOf("want", "need", "go", "feel", "like")
            "you" -> listOf("want", "need", "go", "help", "stop")
            "want" -> listOf("food", "drink", "toilet", "play", "help")
            "need" -> listOf("toilet", "help", "drink", "food", "rest")
            "go" -> listOf("home", "school", "toilet", "outside", "shops")
            "food" -> listOf("apple", "banana", "bread", "snack", "finished")
            "drink" -> listOf("water", "juice", "milk", "cup", "finished")
            "feel" -> listOf("happy", "sad", "sick", "tired", "angry")
            else -> when (currentBoard) {
                "want" -> listOf("food", "drink", "toilet", "play", "help")
                "need" -> listOf("toilet", "help", "drink", "food", "rest")
                "go" -> listOf("home", "school", "toilet", "outside", "shops")
                else -> listOf("I", "want", "need", "toilet", "help")
            }
        }
    }
}

object UsageInsightsCalculator {
    const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    const val SIXTY_DAYS_MS = 60L * 24L * 60L * 60L * 1000L

    fun summarize(
        usageCounts: Map<String, Int>,
        wordEvents: List<WordEvent>,
        spokenSentences: List<String>,
        nowMs: Long,
    ): UsageInsights {
        val currentStart = nowMs - THIRTY_DAYS_MS
        val previousStart = nowMs - SIXTY_DAYS_MS
        val currentWords = mutableSetOf<String>()
        val previousWords = mutableSetOf<String>()
        wordEvents.forEach { event ->
            val word = event.word.trim()
            if (word.isBlank()) return@forEach
            when {
                event.at >= currentStart -> currentWords.add(word.normalizedLabel())
                event.at >= previousStart -> previousWords.add(word.normalizedLabel())
            }
        }

        val sentenceCounts = linkedMapOf<String, Int>()
        spokenSentences.map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { spoken -> sentenceCounts[spoken] = (sentenceCounts[spoken] ?: 0) + 1 }

        return UsageInsights(
            uniqueWordsThisMonth = currentWords.size,
            uniqueWordsPreviousMonth = previousWords.size,
            uniqueTrend = currentWords.size - previousWords.size,
            topWords = usageCounts.entries.asSequence()
                .filter { (label, count) -> label.isNotBlank() && count > 0 }
                .sortedByDescending { (_, count) -> count }
                .take(3)
                .map { (label, count) -> label to count }
                .toList(),
            topSentences = sentenceCounts.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key to it.value },
        )
    }
}

object BoardEditor {
    const val MAX_BOARD_BUTTONS = 20

    fun moveButton(
        boards: Map<String, List<VocabButton>>,
        boardId: String,
        fromIndex: Int,
        toIndex: Int,
        action: DropAction,
    ): Map<String, List<VocabButton>> {
        val boardButtons = boards[boardId].orEmpty()
        if (fromIndex !in boardButtons.indices || toIndex !in 0 until MAX_BOARD_BUTTONS || fromIndex == toIndex) return boards
        val target = boardButtons.getOrNull(toIndex)
        val moved = boardButtons[fromIndex]
        return if (action == DropAction.MoveIntoFolder && target != null && target.boardId != null && target.id != moved.id) {
            boards.toMutableMap().apply {
                this[boardId] = boardButtons.filterIndexed { index, _ -> index != fromIndex }
                this[target.boardId] = (this[target.boardId].orEmpty() + moved).take(MAX_BOARD_BUTTONS)
            }
        } else {
            val next = boardButtons.toMutableList()
            val item = next.removeAt(fromIndex)
            val insertionIndex = if (action == DropAction.MoveAfter) toIndex + 1 else toIndex
            val adjustedIndex = if (fromIndex < insertionIndex) insertionIndex - 1 else insertionIndex
            next.add(adjustedIndex.coerceIn(0, next.size), item)
            boards + (boardId to next)
        }
    }

    fun addButton(
        boards: Map<String, List<VocabButton>>,
        boardId: String,
        button: VocabButton,
    ): Map<String, List<VocabButton>> {
        val nextBoards = boards.toMutableMap()
        nextBoards[boardId] = (nextBoards[boardId].orEmpty() + button).take(MAX_BOARD_BUTTONS)
        button.boardId?.let { nextBoards.putIfAbsent(it, emptyList()) }
        return applySharedVisuals(nextBoards, button)
    }

    fun applySharedVisuals(boards: Map<String, List<VocabButton>>, source: VocabButton): Map<String, List<VocabButton>> {
        val targetLabel = source.label.normalizedLabel()
        if (targetLabel.isBlank()) return boards
        return boards.mapValues { (_, buttons) ->
            buttons.map { button ->
                if (button.label.normalizedLabel() == targetLabel) {
                    button.copy(
                        icon = source.icon,
                        imagePath = source.imagePath,
                        addToSentence = if (source.boardId != null) source.addToSentence else button.addToSentence,
                    )
                } else {
                    button
                }
            }
        }
    }
}

object SentenceEditor {
    fun moveToken(
        sentence: List<SentenceToken>,
        fromIndex: Int,
        toIndex: Int,
        action: DropAction,
    ): List<SentenceToken> {
        if (fromIndex !in sentence.indices || toIndex !in sentence.indices || fromIndex == toIndex) return sentence
        val next = sentence.toMutableList()
        val item = next.removeAt(fromIndex)
        val insertionIndex = if (action == DropAction.MoveAfter) toIndex + 1 else toIndex
        val adjustedIndex = if (fromIndex < insertionIndex) insertionIndex - 1 else insertionIndex
        next.add(adjustedIndex.coerceIn(0, next.size), item)
        return next
    }
}

fun String.normalizedLabel(): String = lowercase(Locale.ROOT).trim()
