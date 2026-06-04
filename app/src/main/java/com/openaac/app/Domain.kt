package com.openaac.app

import java.util.Locale

data class WordEvent(
    val at: Long,
    val word: String,
)

fun inferGrammarRole(label: String, speech: String = label, boardId: String? = null): GrammarRole {
    val text = speech.ifBlank { label }.normalizedGrammarText()
    val board = boardId?.normalizedGrammarText()
    return when {
        text in setOf("i", "you", "me", "mum", "dad", "friend", "teacher") -> GrammarRole.Subject
        text in setOf("want", "need") -> GrammarRole.Intent
        text in setOf("to", "a", "the", "some", "please", "do") -> GrammarRole.None
        text in setOf("don't", "dont", "do not", "not") -> GrammarRole.Negation
        text in setOf("go", "help", "play", "wash", "rest", "like", "have", "eat", "hurts", "hurt") -> GrammarRole.Action
        text in setOf("home", "school", "outside", "shops") || (board == "places" && text != "places") -> GrammarRole.Place
        text in setOf("toilet", "bathroom") || board == "toilet" -> GrammarRole.Toilet
        text in setOf("food", "drink", "water", "juice", "milk", "cup", "apple", "banana", "bread", "snack") ||
            board in setOf("food", "drink") -> GrammarRole.FoodDrink
        text in setOf("happy", "sad", "sick", "tired", "angry", "sore") || board == "feel" -> GrammarRole.Feeling
        text in setOf("head", "hand", "mouth", "tummy") || board == "body" -> GrammarRole.BodyPart
        text in setOf("more", "now", "again") -> GrammarRole.Modifier
        text in setOf("yes", "no", "stop", "finished") -> GrammarRole.Response
        board != null -> GrammarRole.Object
        else -> GrammarRole.Object
    }
}

object GrammarEngine {
    fun realize(tokens: List<SentenceToken>, enabled: Boolean): String {
        return GrammarRealizer.realize(tokens, enabled)
    }

    private fun legacyRealize(tokens: List<SentenceToken>, enabled: Boolean): String {
        val raw = tokens.joinToString(" ") { it.speech }.trim()
        if (!enabled || tokens.isEmpty()) return raw

        val usable = tokens.filter { it.grammarRole != GrammarRole.None && it.speech.isNotBlank() }
        if (usable.isEmpty()) return raw

        val normalized = usable.map { it.speech.trim().normalizedGrammarText() }
        if (normalized.size == 1) return singleWord(usable.first())

        val sentences = splitClauses(usable)
            .flatMap { renderClause(it) }
            .filter { it.isNotBlank() }
        if (sentences.isNotEmpty()) return sentences.joinToString(" ")

        return cleanup(raw)
    }

    private fun splitClauses(tokens: List<SentenceToken>): List<List<SentenceToken>> {
        val clauses = mutableListOf<List<SentenceToken>>()
        val current = mutableListOf<SentenceToken>()

        tokens.forEach { token ->
            val hasContent = current.any { it.grammarRole != GrammarRole.Subject }
            val startsNewSubject = token.grammarRole == GrammarRole.Subject && hasContent
            val startsNewIntent = token.grammarRole == GrammarRole.Intent &&
                current.any { it.grammarRole in setOf(GrammarRole.Feeling, GrammarRole.Toilet, GrammarRole.Place, GrammarRole.Response) }
            if (current.isNotEmpty() && (startsNewSubject || startsNewIntent)) {
                clauses.add(current.toList())
                current.clear()
            }
            current.add(token)
        }
        if (current.isNotEmpty()) clauses.add(current.toList())
        return clauses
    }

    private fun renderClause(tokens: List<SentenceToken>): List<String> {
        negationSentence(tokens)?.let { return listOf(it) }
        painSentence(tokens)?.let { return listOf(it) }
        bodySymptomSentence(tokens)?.let { return listOf(it) }

        val primary = toiletSentence(tokens)
            ?: placeSentence(tokens)
            ?: actionRequestSentence(tokens)
            ?: requestSentence(tokens)
            ?: helpSentence(tokens)
        if (primary != null) {
            return listOfNotNull(primary, feelingSentence(tokens.takeLastWhile { it.grammarRole == GrammarRole.Feeling }))
        }

        feelingSentence(tokens)?.let { return listOf(it) }
        return listOf(cleanup(tokens.joinToString(" ") { it.speech }))
    }

    private fun singleWord(token: SentenceToken): String {
        val text = token.speech.trim()
        return when (token.grammarRole) {
            GrammarRole.Toilet -> "toilet"
            else -> text
        }
    }

    private fun painSentence(tokens: List<SentenceToken>): String? {
        val bodyPart = tokens.firstOrNull { it.grammarRole == GrammarRole.BodyPart }?.speech?.trim() ?: return null
        val hasPain = tokens.any { it.speech.normalizedGrammarText() in setOf("hurt", "hurts", "sore", "pain") }
        if (!hasPain) return null
        val owner = if (tokens.firstOrNull { it.grammarRole == GrammarRole.Subject }?.speech?.normalizedGrammarText() == "you") "Your" else "My"
        return "$owner $bodyPart hurts."
    }

    private fun bodySymptomSentence(tokens: List<SentenceToken>): String? {
        val bodyPart = tokens.firstOrNull { it.grammarRole == GrammarRole.BodyPart }?.speech?.trim() ?: return null
        val symptom = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Feeling &&
                it.speech.normalizedGrammarText() in setOf("sick", "tired", "sore")
        }?.speech?.trim() ?: return null
        val owner = if (tokens.firstOrNull { it.grammarRole == GrammarRole.Subject }?.speech?.normalizedGrammarText() == "you") "Your" else "My"
        return "$owner $bodyPart feels $symptom."
    }

    private fun feelingSentence(tokens: List<SentenceToken>): String? {
        if (tokens.any { it.isNegation() }) return null
        val feelings = tokens
            .filter { it.grammarRole == GrammarRole.Feeling }
            .map { it.speech.trim() }
            .filter { it.isNotBlank() && it.normalizedGrammarText() != "feel" }
            .distinctBy { it.normalizedGrammarText() }
        if (feelings.isEmpty()) return null
        val subject = subjectText(tokens) ?: "I"
        return "$subject ${verbForSubject(subject, "feel", "feels")} ${feelings.joinForSpeech()}."
    }

    private fun negationSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.isNegation() }) return null
        val subject = subjectText(tokens) ?: "I"
        val negation = negationForSubject(subject)
        val action = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action &&
                it.speech.normalizedGrammarText() !in setOf("hurt", "hurts")
        }?.speech?.normalizedGrammarText()
        val intent = intentText(tokens)
        val toilet = tokens.firstOrNull { it.grammarRole == GrammarRole.Toilet }?.speech?.trim()
        val place = tokens.firstOrNull { it.grammarRole == GrammarRole.Place }?.speech?.trim()
        val feeling = tokens
            .filter { it.grammarRole == GrammarRole.Feeling }
            .map { it.speech.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.normalizedGrammarText() }
            .joinForSpeech()
        val target = tokens.firstOrNull {
            it.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) &&
                it.speech.normalizedGrammarText() !in setOf("food", "drink", "things")
        }?.speech?.trim()

        return when {
            action == "feel" && feeling.isNotBlank() -> "$subject $negation feel $feeling."
            action == "like" && place != null -> "$subject $negation like $place."
            action == "like" && toilet != null -> "$subject $negation like ${toiletArticle(toilet)}$toilet."
            action == "like" && target != null -> "$subject $negation like $target."
            action == "go" && place != null -> "$subject $negation want to go $place."
            action == "go" && toilet != null -> "$subject $negation want to go to ${toiletArticle(toilet)}$toilet."
            intent != null && toilet != null -> "$subject $negation $intent to go to ${toiletArticle(toilet)}$toilet."
            intent != null && place != null -> "$subject $negation $intent to go $place."
            intent != null && target != null -> "$subject $negation $intent $target."
            feeling.isNotBlank() -> "$subject $negation feel $feeling."
            target != null -> "$subject $negation want $target."
            else -> cleanup(tokens.joinToString(" ") { it.speech })
        }
    }

    private fun toiletSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.grammarRole == GrammarRole.Toilet }) return null
        val subject = subjectText(tokens) ?: "I"
        val intent = intentText(tokens) ?: "want"
        val toilet = tokens.firstOrNull { it.grammarRole == GrammarRole.Toilet }?.speech?.trim().orEmpty()
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} to go to ${toiletArticle(toilet)}$toilet."
    }

    private fun placeSentence(tokens: List<SentenceToken>): String? {
        val place = tokens.firstOrNull { it.grammarRole == GrammarRole.Place }?.speech?.trim() ?: return null
        val hasGo = tokens.any { it.speech.normalizedGrammarText() == "go" || it.boardId?.normalizedGrammarText() == "go" }
        if (!hasGo && tokens.none { it.grammarRole == GrammarRole.Intent }) return null
        val subject = subjectText(tokens) ?: "I"
        val intent = intentText(tokens) ?: "want"
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} to go $place."
    }

    private fun requestSentence(tokens: List<SentenceToken>): String? {
        val intent = intentText(tokens) ?: return null
        val subject = subjectText(tokens) ?: "I"
        val target = tokens.firstOrNull {
            it.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) &&
                it.speech.normalizedGrammarText() !in setOf("food", "drink", "things")
        }?.speech?.trim() ?: return null
        val determiner = if (tokens.firstOrNull { it.grammarRole == GrammarRole.FoodDrink } != null) "some " else ""
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} $determiner$target."
    }

    private fun actionRequestSentence(tokens: List<SentenceToken>): String? {
        val intent = intentText(tokens) ?: return null
        val subject = subjectText(tokens) ?: "I"
        val action = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action &&
                it.speech.normalizedGrammarText() !in setOf("feel", "go", "help", "hurt", "hurts")
        }?.speech?.trim() ?: return null
        val target = tokens.firstOrNull {
            it.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) &&
                it.speech.normalizedGrammarText() !in setOf("food", "drink", "things")
        }?.speech?.trim()
        val suffix = target?.let { " $it" }.orEmpty()
        return "$subject ${verbForSubject(subject, intent, "${intent}s")} to $action$suffix."
    }

    private fun helpSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.speech.normalizedGrammarText() == "help" }) return null
        val subject = subjectText(tokens)
        return if (subject?.normalizedGrammarText() == "you") "Can you help me?" else "I need help."
    }

    private fun subjectText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull { it.grammarRole == GrammarRole.Subject }?.speech?.trim()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

    private fun intentText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull { it.grammarRole == GrammarRole.Intent }?.speech?.normalizedGrammarText()

    private fun verbForSubject(subject: String, firstPerson: String, thirdPerson: String): String =
        if (subject.normalizedGrammarText() in setOf("i", "you")) firstPerson else thirdPerson

    private fun negationForSubject(subject: String): String =
        if (subject.normalizedGrammarText() in setOf("i", "you")) "don't" else "doesn't"

    private fun toiletArticle(toilet: String): String =
        if (toilet.normalizedGrammarText() in setOf("toilet", "bathroom")) "the " else ""

    private fun SentenceToken.isNegation(): Boolean =
        grammarRole == GrammarRole.Negation || speech.normalizedGrammarText() in setOf("don't", "dont", "do not", "not")

    private fun cleanup(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        return if (trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith("!")) trimmed else "$trimmed."
    }

    private fun List<String>.joinForSpeech(): String =
        when (size) {
            0 -> ""
            1 -> first()
            2 -> "${first()} and ${last()}"
            else -> dropLast(1).joinToString(", ") + ", and " + last()
        }
}

private object GrammarRealizer {
    fun realize(tokens: List<SentenceToken>, enabled: Boolean): String {
        val raw = tokens.joinToString(" ") { it.speech }.trim()
        if (!enabled || tokens.isEmpty()) return raw

        val usable = tokens
            .filter { it.grammarRole != GrammarRole.None && it.speech.isNotBlank() }
        if (usable.isEmpty()) return raw
        if (usable.size == 1) return singleWord(usable.first())

        val rendered = splitClauses(usable)
            .mapNotNull { renderClause(it.dedupeRepeatedWords()) }
            .filter { it.isNotBlank() }

        return if (rendered.isEmpty()) cleanup(raw) else rendered.joinToString(" ")
    }

    private fun splitClauses(tokens: List<SentenceToken>): List<List<SentenceToken>> {
        val clauses = mutableListOf<List<SentenceToken>>()
        val current = mutableListOf<SentenceToken>()
        tokens.forEach { token ->
            val normalized = token.normalized()
            val hasContent = current.any { it.grammarRole != GrammarRole.Subject }
            val startsNewSubject = normalized in setOf("i", "you") && hasContent
            val startsNewIntent = token.grammarRole == GrammarRole.Intent &&
                current.any { it.grammarRole in setOf(GrammarRole.Feeling, GrammarRole.Toilet, GrammarRole.Place, GrammarRole.Response) }
            val startsHelpClause = normalized == "help" &&
                current.any { it.grammarRole in setOf(GrammarRole.Toilet, GrammarRole.Place, GrammarRole.Feeling, GrammarRole.FoodDrink) }
            if (current.isNotEmpty() && (startsNewSubject || startsNewIntent || startsHelpClause)) {
                clauses.add(current.toList())
                current.clear()
            }
            current.add(token)
        }
        if (current.isNotEmpty()) clauses.add(current.toList())
        return clauses
    }

    private fun renderClause(tokens: List<SentenceToken>): String? {
        if (tokens.all { it.grammarRole in setOf(GrammarRole.Response, GrammarRole.Modifier) }) {
            return cleanup(tokens.joinToString(" ") { it.speech.trim() })
        }
        return negationSentence(tokens)
            ?: helpSentence(tokens)
            ?: painSentence(tokens)
            ?: bodySymptomSentence(tokens)
            ?: toiletSentence(tokens)
            ?: placeSentence(tokens)
            ?: feelingSentence(tokens)
            ?: actionRequestSentence(tokens)
            ?: requestSentence(tokens)
            ?: cleanup(tokens.joinToString(" ") { it.speech.trim() })
    }

    private fun singleWord(token: SentenceToken): String =
        when (token.grammarRole) {
            GrammarRole.Toilet -> "toilet"
            else -> token.speech.trim()
        }

    private fun negationSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.isNegation() }) return null
        val subject = subjectText(tokens) ?: "I"
        val negation = if (subject.normalizedGrammarText() in setOf("i", "you")) "don't" else "doesn't"
        val action = actionText(tokens)
        val intent = intentText(tokens)
        val toilet = tokens.firstOrNull { it.grammarRole == GrammarRole.Toilet }?.speech?.trim()
        val place = tokens.firstOrNull { it.grammarRole == GrammarRole.Place }?.speech?.trim()
        val feelings = feelingWords(tokens).joinForSpeech()
        val target = requestTargetToken(tokens)?.speech?.trim()
        val modifier = endModifierText(tokens)

        return when {
            action == "feel" && feelings.isNotBlank() -> cleanup("$subject $negation feel $feelings$modifier")
            action == "like" && place != null -> cleanup("$subject $negation like $place$modifier")
            action == "like" && toilet != null -> cleanup("$subject $negation like ${toiletArticle(toilet)}$toilet$modifier")
            action == "like" && target != null -> cleanup("$subject $negation like $target$modifier")
            action == "go" && place != null -> cleanup("$subject $negation want to go ${placeDestination(place)}$modifier")
            action == "go" && toilet != null -> cleanup("$subject $negation want to go to ${toiletArticle(toilet)}$toilet$modifier")
            intent != null && toilet != null -> cleanup("$subject $negation $intent to go to ${toiletArticle(toilet)}$toilet$modifier")
            intent != null && place != null -> cleanup("$subject $negation $intent to go ${placeDestination(place)}$modifier")
            intent != null && target != null -> cleanup("$subject $negation $intent ${requestTarget(tokens, target)}$modifier")
            feelings.isNotBlank() -> cleanup("$subject $negation feel $feelings$modifier")
            target != null -> cleanup("$subject $negation want ${requestTarget(tokens, target)}$modifier")
            else -> null
        }
    }

    private fun helpSentence(tokens: List<SentenceToken>): String? {
        if (tokens.none { it.normalized() == "help" }) return null
        val subject = subjectText(tokens)
        val modifier = endModifierText(tokens)
        return when {
            subject?.normalizedGrammarText() == "you" -> cleanupQuestion("Can you help me$modifier")
            tokens.any { it.normalized() == "me" } -> cleanup("Please help me$modifier")
            else -> cleanup("I need help$modifier")
        }
    }

    private fun painSentence(tokens: List<SentenceToken>): String? {
        val bodyPart = tokens.firstOrNull { it.grammarRole == GrammarRole.BodyPart }?.speech?.trim() ?: return null
        if (tokens.none { it.normalized() in setOf("hurt", "hurts", "sore", "pain") }) return null
        val owner = if (subjectText(tokens)?.normalizedGrammarText() == "you") "Your" else "My"
        return cleanup("$owner $bodyPart hurts${endModifierText(tokens)}")
    }

    private fun bodySymptomSentence(tokens: List<SentenceToken>): String? {
        val bodyPart = tokens.firstOrNull { it.grammarRole == GrammarRole.BodyPart }?.speech?.trim() ?: return null
        val symptom = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Feeling && it.normalized() in setOf("sick", "tired", "sore")
        }?.speech?.trim() ?: return null
        val owner = if (subjectText(tokens)?.normalizedGrammarText() == "you") "Your" else "My"
        return cleanup("$owner $bodyPart feels $symptom${endModifierText(tokens)}")
    }

    private fun toiletSentence(tokens: List<SentenceToken>): String? {
        val toilet = tokens.firstOrNull { it.grammarRole == GrammarRole.Toilet }?.speech?.trim() ?: return null
        val subject = subjectText(tokens) ?: "I"
        val intent = intentText(tokens) ?: "need"
        return cleanup("$subject ${verbForSubject(subject, intent, "${intent}s")} to go to ${toiletArticle(toilet)}$toilet${endModifierText(tokens)}")
    }

    private fun placeSentence(tokens: List<SentenceToken>): String? {
        val place = placeToken(tokens)?.speech?.trim() ?: return null
        val hasGo = tokens.any { it.normalized() == "go" || it.boardId?.normalizedGrammarText() == "go" }
        val intent = intentText(tokens)
        if (!hasGo && intent == null) return null
        val subject = subjectText(tokens) ?: "I"
        val destination = placeDestination(place)
        val purpose = purposeText(tokens)
        return when {
            intent != null -> cleanup("$subject ${verbForSubject(subject, intent, "${intent}s")} to go $destination$purpose${endModifierText(tokens)}")
            subjectText(tokens) != null -> cleanup("$subject ${verbForSubject(subject, "go", "goes")} $destination$purpose${endModifierText(tokens)}")
            else -> cleanup("go $destination$purpose${endModifierText(tokens)}")
        }
    }

    private fun feelingSentence(tokens: List<SentenceToken>): String? {
        if (tokens.any { it.isNegation() }) return null
        val feelings = feelingWords(tokens)
        if (feelings.isEmpty()) return null
        val subject = subjectText(tokens) ?: "I"
        return cleanup("$subject ${verbForSubject(subject, "feel", "feels")} ${feelings.joinForSpeech()}${endModifierText(tokens)}")
    }

    private fun actionRequestSentence(tokens: List<SentenceToken>): String? {
        val intent = intentText(tokens) ?: return null
        val action = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action && it.normalized() !in setOf("feel", "go", "help", "hurt", "hurts")
        }?.speech?.trim() ?: return null
        val subject = subjectText(tokens) ?: "I"
        val target = requestTargetPhrase(tokens)?.let { " $it" }.orEmpty()
        return cleanup("$subject ${verbForSubject(subject, intent, "${intent}s")} to $action$target${endModifierText(tokens)}")
    }

    private fun requestSentence(tokens: List<SentenceToken>): String? {
        val target = requestTargetPhrase(tokens) ?: return null
        val intent = intentText(tokens) ?: if (tokens.any { it.normalized() == "more" }) "want" else return null
        val subject = subjectText(tokens) ?: "I"
        return cleanup("$subject ${verbForSubject(subject, intent, "${intent}s")} ${requestTarget(tokens, target)}${endModifierText(tokens)}")
    }

    private fun subjectText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull {
            it.grammarRole == GrammarRole.Subject && it.normalized() in setOf("i", "you", "mum", "dad", "friend", "teacher")
        }?.speech?.trim()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }

    private fun intentText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull { it.grammarRole == GrammarRole.Intent }?.normalized()

    private fun actionText(tokens: List<SentenceToken>): String? =
        tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action && it.normalized() !in setOf("hurt", "hurts")
        }?.normalized()

    private fun feelingWords(tokens: List<SentenceToken>): List<String> =
        tokens
            .filter { it.grammarRole == GrammarRole.Feeling }
            .map { it.speech.trim() }
            .filter { it.isNotBlank() && it.normalizedGrammarText() != "feel" }
            .distinctBy { it.normalizedGrammarText() }

    private fun requestTargetToken(tokens: List<SentenceToken>): SentenceToken? {
        return requestTargetTokens(tokens).firstOrNull()
    }

    private fun requestTargetTokens(tokens: List<SentenceToken>): List<SentenceToken> {
        val subject = subjectText(tokens)?.normalizedGrammarText()
        val candidates = tokens.filter { token ->
            val normalized = token.normalized()
            val usableRole = token.grammarRole in setOf(GrammarRole.Object, GrammarRole.FoodDrink) ||
                (token.grammarRole == GrammarRole.Subject && normalized !in setOf("i", "you", "me") && normalized != subject)
            usableRole && normalized !in setOf("food", "people", "places", "body", "feel")
        }
        val hasSpecificFoodDrink = candidates.any {
            it.grammarRole == GrammarRole.FoodDrink && it.normalized() !in setOf("food", "drink")
        }
        return candidates.filterNot {
            hasSpecificFoodDrink && it.grammarRole == GrammarRole.FoodDrink && it.normalized() in setOf("food", "drink")
        }
    }

    private fun requestTargetPhrase(tokens: List<SentenceToken>): String? {
        val targets = requestTargetTokens(tokens)
        if (targets.isEmpty()) return null
        return targets
            .map { it.speech.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun requestTarget(tokens: List<SentenceToken>, target: String): String {
        val normalized = target.normalizedGrammarText()
        return when {
            tokens.any { it.normalized() == "more" } -> "more $target"
            normalized == "drink" -> "a drink"
            normalized == "things" -> target
            requestTargetTokens(tokens).any { it.grammarRole == GrammarRole.FoodDrink } -> "some $target"
            else -> target
        }
    }

    private fun endModifierText(tokens: List<SentenceToken>): String {
        val modifiers = tokens
            .filter { it.grammarRole == GrammarRole.Modifier }
            .map { it.speech.trim() }
            .filter { it.normalizedGrammarText() != "more" }
            .distinctBy { it.normalizedGrammarText() }
        return if (modifiers.isEmpty()) "" else " ${modifiers.joinToString(" ")}"
    }

    private fun verbForSubject(subject: String, firstPerson: String, thirdPerson: String): String =
        if (subject.normalizedGrammarText() in setOf("i", "you")) firstPerson else thirdPerson

    private fun toiletArticle(toilet: String): String =
        if (toilet.normalizedGrammarText() in setOf("toilet", "bathroom")) "the " else ""

    private fun placeDestination(place: String): String =
        when (place.normalizedGrammarText()) {
            "home", "outside" -> place
            "shops" -> "to the $place"
            else -> "to $place"
        }

    private fun placeToken(tokens: List<SentenceToken>): SentenceToken? =
        tokens.firstOrNull { it.grammarRole == GrammarRole.Place && it.normalized() != "places" }

    private fun purposeText(tokens: List<SentenceToken>): String {
        val hasGo = tokens.any { it.normalized() == "go" || it.boardId?.normalizedGrammarText() == "go" }
        val hasPlace = placeToken(tokens) != null
        if (!hasGo || !hasPlace) return ""
        val purposeAction = tokens.firstOrNull {
            it.grammarRole == GrammarRole.Action && it.normalized() !in setOf("go", "feel", "help", "hurt", "hurts")
        }?.speech?.trim() ?: return ""
        return " to $purposeAction"
    }

    private fun SentenceToken.normalized(): String = speech.normalizedGrammarText()

    private fun SentenceToken.isNegation(): Boolean =
        grammarRole == GrammarRole.Negation || normalized() in setOf("don't", "dont", "do not", "not")

    private fun cleanup(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ")
        return if (trimmed.endsWith(".") || trimmed.endsWith("?") || trimmed.endsWith("!")) trimmed else "$trimmed."
    }

    private fun cleanupQuestion(text: String): String {
        val trimmed = text.trim().replace(Regex("\\s+"), " ").removeSuffix(".")
        return if (trimmed.endsWith("?")) trimmed else "$trimmed?"
    }

    private fun List<String>.joinForSpeech(): String =
        when (size) {
            0 -> ""
            1 -> first()
            2 -> "${first()} and ${last()}"
            else -> dropLast(1).joinToString(", ") + ", and " + last()
        }

    private fun List<SentenceToken>.dedupeRepeatedWords(): List<SentenceToken> {
        val seen = mutableSetOf<String>()
        return filter { token ->
            val normalized = token.normalized()
            if (normalized in seen) false else {
                seen.add(normalized)
                true
            }
        }
    }
}

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
                        grammarRole = source.grammarRole,
                    )
                } else {
                    button
                }
            }
        }
    }
}

fun String.normalizedLabel(): String = lowercase(Locale.ROOT).trim()

fun String.normalizedGrammarText(): String =
    lowercase(Locale.ROOT).trim().replace(Regex("\\s+"), " ")
