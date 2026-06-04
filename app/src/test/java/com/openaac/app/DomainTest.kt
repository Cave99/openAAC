package com.openaac.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    @Test
    fun recommendationUsesLearnedTransitionsBeforeFallbacks() {
        val result = RecommendationEngine.recommend(
            lastWord = "want",
            currentBoard = "want",
            visibleButtons = Defaults.boards.getValue("want"),
            boards = Defaults.allBoards(),
            usageCounts = emptyMap(),
            transitionCounts = mapOf(("want" to "drink") to 3),
        )

        assertEquals("drink", result.buttons.first().label)
        assertEquals("starter suggestions", result.status)
    }

    @Test
    fun recommendationStatusReflectsEnoughTransitionHistory() {
        val result = RecommendationEngine.recommend(
            lastWord = "go",
            currentBoard = "go",
            visibleButtons = Defaults.boards.getValue("go"),
            boards = Defaults.allBoards(),
            usageCounts = emptyMap(),
            transitionCounts = mapOf(("go" to "school") to 12),
        )

        assertEquals("starting to personalize", result.status)
    }

    @Test
    fun usageInsightsSeparateCurrentAndPreviousMonthUniqueWords() {
        val now = 100L * 24L * 60L * 60L * 1000L
        val insights = UsageInsightsCalculator.summarize(
            usageCounts = mapOf("want" to 4, "drink" to 2, "help" to 1),
            wordEvents = listOf(
                WordEvent(now - 1_000L, "Want"),
                WordEvent(now - 2_000L, "want"),
                WordEvent(now - UsageInsightsCalculator.THIRTY_DAYS_MS - 1_000L, "drink"),
                WordEvent(now - UsageInsightsCalculator.SIXTY_DAYS_MS - 1_000L, "old"),
            ),
            spokenSentences = listOf("I want drink", "I want drink", "help"),
            nowMs = now,
        )

        assertEquals(1, insights.uniqueWordsThisMonth)
        assertEquals(1, insights.uniqueWordsPreviousMonth)
        assertEquals(0, insights.uniqueTrend)
        assertEquals("want" to 4, insights.topWords.first())
        assertEquals("I want drink" to 2, insights.topSentences.first())
    }

    @Test
    fun boardEditorMovesButtonIntoFolderBoard() {
        val folder = VocabButton("food", "food", "food", "F", 0xFFFFFFFF, boardId = "food", isCategory = true)
        val apple = VocabButton("apple", "apple", "apple", "A", 0xFFFFFFFF)
        val boards = mapOf(
            Defaults.HOME_BOARD to listOf(folder, apple),
            "food" to emptyList(),
        )

        val result = BoardEditor.moveButton(
            boards = boards,
            boardId = Defaults.HOME_BOARD,
            fromIndex = 1,
            toIndex = 0,
            action = DropAction.MoveIntoFolder,
        )

        assertEquals(listOf(folder), result.getValue(Defaults.HOME_BOARD))
        assertEquals(listOf(apple), result.getValue("food"))
    }

    @Test
    fun boardEditorAddButtonCreatesFolderBoardAndAppliesSharedVisuals() {
        val existing = VocabButton("help", "help", "help", "?", 0xFFFFFFFF)
        val updated = existing.copy(id = "help_folder", icon = "!", boardId = "help", isCategory = true, addToSentence = false)

        val result = BoardEditor.addButton(
            boards = mapOf(Defaults.HOME_BOARD to listOf(existing)),
            boardId = Defaults.HOME_BOARD,
            button = updated,
        )

        assertTrue(result.containsKey("help"))
        assertEquals("!", result.getValue(Defaults.HOME_BOARD).first().icon)
        assertEquals(false, result.getValue(Defaults.HOME_BOARD).first().addToSentence)
    }

    @Test
    fun sentenceEditorMovesTokenBeforeAndAfterTarget() {
        val sentence = listOf(
            SentenceToken("I", "I", "I", null),
            SentenceToken("want", "want", "W", null),
            SentenceToken("water", "water", "D", null),
        )

        val movedAfter = SentenceEditor.moveToken(sentence, fromIndex = 0, toIndex = 1, action = DropAction.MoveAfter)
        assertEquals(listOf("want", "I", "water"), movedAfter.map { it.label })

        val movedBefore = SentenceEditor.moveToken(sentence, fromIndex = 2, toIndex = 0, action = DropAction.MoveBefore)
        assertEquals(listOf("water", "I", "want"), movedBefore.map { it.label })
    }
}
