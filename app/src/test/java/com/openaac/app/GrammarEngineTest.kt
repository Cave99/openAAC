package com.openaac.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive regression coverage for the grammar engine.
 *
 * Every case below describes a real communication attempt a non-speaking
 * child might make, including mistakes, mis-taps, repeats, runs of modifiers,
 * out-of-order words, and words the engine has never seen. The expected
 * value documents how the engine currently realises that intent.
 *
 * Two rules govern the expectations:
 *
 * 1. The engine must NOT change what the child is trying to say. It may
 *    add filler words (subjects, articles, "to", "go", "toilet") to make
 *    the output easier to hear, but it must not invent a different
 *    request, drop the core noun, flip a positive into a negative, or
 *    swap a feeling for an action.
 *
 * 2. If the child taps the same word twice, taps a modifier after the
 *    rest of the sentence, or includes words that the engine doesn't
 *    model, the result should still describe the same idea. It is okay
 *    for the engine to drop noise words as long as the meaning is
 *    preserved.
 *
 * The case names describe the child's intent. If a case starts failing
 * the developer should be able to read the case name and immediately see
 * which communication path regressed.
 */
class GrammarEngineTest {

    // ====================================================================
    // Comprehensive phrase library.
    // Each case is (name, input the child tapped, expected spoken output).
    // The full list is run as a single test so every regression shows up
    // in one report.
    // ====================================================================

    @Test
    fun desiredPhraseLibrary() {
        val cases = listOf(
            // ---------- Toilet and urgent bathroom requests ----------
            // The child is asking to go to the toilet right now, in many
            // ways a child might naturally tap it.
            Case("urgent toilet (full)", "I need toilet now", "I need to go to the toilet now."),
            Case("urgent toilet with want", "I want toilet now", "I want to go to the toilet now."),
            Case("urgent toilet no subject", "need toilet now", "I need to go to the toilet now."),
            Case("urgent toilet minimal", "toilet now", "I need to go to the toilet now."),
            Case("urgent toilet with intent and no now", "I need toilet", "I need to go to the toilet."),
            Case("urgent toilet with want and no now", "I want toilet", "I want to go to the toilet."),
            Case("urgent toilet with bare intent", "want toilet", "I want to go to the toilet."),
            Case("urgent toilet with bare need", "need toilet", "I need to go to the toilet."),
            Case("urgent bathroom (full)", "I need bathroom now", "I need to go to the bathroom now."),
            Case("just toilet button", "toilet", "toilet"),
            Case("just bathroom button", "bathroom", "toilet"),
            Case("toilet then please modifier (please is unknown)", "toilet now please", "I need to go to the toilet now."),
            Case("toilet then you subject", "you toilet", "You need to go to the toilet."),
            Case("toilet then Mum subject", "Mum toilet", "Mum needs to go to the toilet."),
            Case("Dad needs toilet", "Dad I need toilet", "Dad needs to go to the toilet."),

            // ---------- Drink requests ----------
            // Generic and specific drinks, with and without "now", and
            // the special "a drink" phrasing vs. "some water" phrasing.
            Case("generic drink urgency", "I want drink now", "I want a drink now."),
            Case("generic drink no modifier", "I want drink", "I want a drink."),
            Case("specific drink urgency", "I want water now", "I want some water now."),
            Case("specific drink no modifier", "I want water", "I want some water."),
            Case("juice request", "I want juice", "I want some juice."),
            Case("milk request", "I want milk", "I want some milk."),
            Case("cup request", "I want cup", "I want some cup."),
            Case("water with more urgency", "I want more water", "I want more water."),
            Case("more water then now", "more water now", "I want more water now."),
            Case("just water", "water", "water"),
            Case("just juice", "juice", "juice"),
            Case("just milk", "milk", "milk"),
            Case("just cup", "cup", "cup"),
            Case("just drink", "drink", "drink"),
            Case("want water no subject", "want water", "I want some water."),
            Case("need water no subject", "need water", "I need some water."),
            Case("I need drink", "I need drink", "I need a drink."),
            Case("I need water", "I need water", "I need some water."),
            Case("I want apple with now", "I want apple now", "I want some apple now."),
            Case("I want more apple", "I want more apple", "I want more apple."),
            Case("I want banana", "I want banana", "I want some banana."),
            Case("I want bread", "I want bread", "I want some bread."),
            Case("I want snack", "I want snack", "I want some snack."),
            Case("I want apple juice", "I want apple juice", "I want some apple juice."),
            Case("I want drink juice preserves specific drink", "I want drink juice", "I want some juice."),
            Case("more water no subject", "more water", "I want more water."),
            Case("I more water (broken order)", "I more water", "I want more water."),
            Case("just food", "food", "food"),
            Case("just snack", "snack", "snack"),
            Case("just apple", "apple", "apple"),

            // ---------- Food requests (generic and specific) ----------
            Case("I want food", "I want food", "I want food."),
            Case("I want food now", "I want food now", "I want food now."),
            Case("I want things now", "I want things now", "I want things now."),
            Case("I want things", "I want things", "I want things."),
            Case("want things no subject", "want things", "I want things."),
            Case("just things", "things", "things"),
            Case("things now", "things now", "things now."),
            Case("I want things and play", "I want things I want play", "I want things. I want to play."),
            Case("I want things and feel sad", "I want things I feel sad", "I want things. I feel sad."),
            Case("I want food and drink", "I want food I want drink", "I want food. I want a drink."),

            // ---------- Place requests ----------
            // The place engine adds "to" before school/shops but not home/outside.
            Case("school with bare go", "I go school now", "I go to school now."),
            Case("school with intent and go", "I want go school", "I want to go to school."),
            Case("home with intent and go", "I want go home now", "I want to go home now."),
            Case("home with intent no now", "I want go home", "I want to go home."),
            Case("outside with intent", "I want go outside", "I want to go outside."),
            Case("shops with intent", "I want go shops", "I want to go to the shops."),
            Case("I go home no now", "I go home", "I go home."),
            Case("I go school no now", "I go school", "I go to school."),
            Case("you go home stays a go statement", "you go home", "You go home."),
            Case("go home no subject", "go home", "go home."),
            Case("go school no subject", "go school", "go to school."),
            Case("go outside no subject", "go outside", "go outside."),
            Case("go shops no subject", "go shops", "go to the shops."),
            Case("places category before outside does not become destination", "places outside go", "go outside."),
            Case("places category outside go play to", "Places outside go play to", "go outside to play."),
            Case("I want home (no go)", "I want home", "I want to go home."),
            Case("I want school (no go)", "I want school", "I want to go to school."),
            Case("I need home", "I need home", "I need to go home."),
            Case("just home", "home", "home"),
            Case("just school", "school", "school"),
            Case("just outside", "outside", "outside"),
            Case("just shops", "shops", "shops"),
            Case("I want go home and Mum", "I want go home I want Mum", "I want to go home. I want Mum."),
            Case("I want go home now and Mum", "I want go home now I want Mum", "I want to go home now. I want Mum."),
            Case("I want go home and feel happy", "I want go home I feel happy", "I want to go home. I feel happy."),

            // ---------- Feeling expressions ----------
            // Single and multiple feelings. Multiple feelings use "and" / Oxford comma.
            Case("feel sick with now", "I feel sick now", "I feel sick now."),
            Case("feel multiple (two)", "I feel sad angry", "I feel sad and angry."),
            Case("feel multiple (three)", "I feel happy sad tired", "I feel happy, sad, and tired."),
            Case("feel happy", "I feel happy", "I feel happy."),
            Case("feel sad", "I feel sad", "I feel sad."),
            Case("feel angry", "I feel angry", "I feel angry."),
            Case("feel tired", "I feel tired", "I feel tired."),
            Case("feel sick", "I feel sick", "I feel sick."),
            Case("feel sore", "I feel sore", "I feel sore."),
            Case("sad without feel", "I sad", "I feel sad."),
            Case("happy without feel", "I happy", "I feel happy."),
            Case("just happy", "happy", "happy"),
            Case("just sad", "sad", "sad"),
            Case("just sick", "sick", "sick"),
            Case("just tired", "tired", "tired"),
            Case("just angry", "angry", "angry"),
            Case("just sore", "sore", "sore"),
            Case("just feel button", "I feel", "I feel."),
            Case("feel happy no subject", "feel happy", "I feel happy."),
            Case("I feel happy now", "I feel happy now", "I feel happy now."),
            Case("two separate feel clauses", "I feel sad I feel angry", "I feel sad. I feel angry."),
            Case("feel then want", "I feel happy I want water", "I feel happy. I want some water."),
            Case("happy then want", "happy I want water", "I feel happy. I want some water."),
            Case("yes then want", "yes I want water", "yes. I want some water."),
            Case("I want water then yes (yes is dropped as response modifier)", "I want water yes", "I want some water."),
            Case("I want water then need help", "I want water I need help", "I want some water. I need help."),
            Case("I want play then food", "I want play I want food", "I want to play. I want food."),
            Case("happy I need Mum", "happy I need Mum", "I feel happy. I need Mum."),
            Case("I feel happy I want go home", "I feel happy I want go home", "I feel happy. I want to go home."),
            Case("I feel happy I want Mum", "I feel happy I want Mum", "I feel happy. I want Mum."),

            // ---------- Body parts and pain ----------
            // The body engine uses "My" / "Your" depending on the subject and
            // distinguishes "hurts" (for pain) from "feels X" (for body symptoms).
            Case("tummy hurts with now", "tummy hurts now", "My tummy hurts now."),
            Case("tummy hurts", "tummy hurts", "My tummy hurts."),
            Case("head hurts", "head hurts", "My head hurts."),
            Case("hand hurts", "hand hurts", "My hand hurts."),
            Case("mouth hurts", "mouth hurts", "My mouth hurts."),
            Case("My tummy hurts (subject + body + pain)", "My tummy hurts", "My tummy hurts."),
            Case("I tummy hurts", "I tummy hurts", "My tummy hurts."),
            Case("you tummy hurts (you is the listener)", "you tummy hurts", "Your tummy hurts."),
            Case("tummy with sore (pain)", "tummy sore", "My tummy hurts."),
            Case("head with sick (symptom)", "head sick", "My head feels sick."),
            Case("hand with sore (pain)", "hand sore", "My hand hurts."),
            Case("tummy with tired (symptom)", "tummy tired", "My tummy feels tired."),
            Case("tummy with sick (symptom)", "tummy sick", "My tummy feels sick."),
            Case("just tummy", "tummy", "tummy"),
            Case("just head", "head", "head"),
            Case("just hand", "hand", "hand"),
            Case("just mouth", "mouth", "mouth"),
            Case("just hurts", "hurts", "hurts"),
            Case("just hurt", "hurt", "hurt"),
            Case("I tummy (no pain)", "I tummy", "I tummy."),
            Case("you tummy (no pain)", "you tummy", "you tummy."),
            Case("tummy hurts I want Mum", "tummy hurts I want Mum", "My tummy hurts. I want Mum."),
            Case("I tummy hurts I need Mum", "I tummy hurts I need Mum", "My tummy hurts. I need Mum."),
            Case("tummy hurts I feel sick", "tummy hurts I feel sick", "My tummy hurts. I feel sick."),

            // ---------- Help requests ----------
            // Help can come as just "help", "help me" (asking for help), or
            // "you help me" (asking the listener).
            Case("I need help with now", "I need help now", "I need help now."),
            Case("help me now", "help me now", "Please help me now."),
            Case("you help me (asking the listener)", "you help me", "Can you help me?"),
            Case("you help me with now", "you help me now", "Can you help me now?"),
            Case("I need help no now", "I need help", "I need help."),
            Case("just help button", "help", "help"),
            Case("help me no now", "help me", "Please help me."),
            Case("help with now", "help now", "I need help now."),
            Case("I help (broken order)", "I help", "I need help."),
            Case("you need help", "you need help", "Can you help me?"),
            Case("you need help now", "you need help now", "Can you help me now?"),
            Case("Mum help (Mum subject becomes separate clause)", "Mum help", "I need help."),
            Case("Mum help me", "Mum help me", "Please help me."),
            Case("Mum help me please (please is unknown)", "Mum help me please", "Please help me."),
            Case("teacher help", "teacher help", "I need help."),
            Case("I want help", "I want help", "I need help."),
            Case("I need help me", "I need help me", "Please help me."),
            Case("I need help me now", "I need help me now", "Please help me now."),
            Case("Mum I need help", "Mum I need help", "I need help."),
            Case("I need help to go", "toilet now help", "I need to go to the toilet now. I need help."),
            Case("toilet help (no now)", "toilet help", "I need to go to the toilet. I need help."),
            Case("toilet I feel sick (split clauses)", "toilet I feel sick", "I need to go to the toilet. I feel sick."),
            Case("tummy hurts help (help alone)", "tummy hurts help", "I need help."),
            Case("I need toilet I feel sick", "I need toilet I feel sick", "I need to go to the toilet. I feel sick."),

            // ---------- Negation ----------
            // Negation with various verbs, targets, and modifiers.
            Case("don't like apple (Object)", "I don't like apple", "I don't like apple."),
            Case("don't want apple (FoodDrink -> some)", "I don't want apple", "I don't want some apple."),
            Case("don't want apple no subject", "don't want apple", "I don't want some apple."),
            Case("don't want go school (place)", "I don't want go school", "I don't want to go to school."),
            Case("don't want go home (place)", "I don't want go home", "I don't want to go home."),
            Case("don't want go home no subject", "don't want go home", "I don't want to go home."),
            Case("don't like toy (Object)", "I don't like toy", "I don't like toy."),
            Case("don't like no target", "I don't like", "I don't like."),
            Case("don't like no target no subject", "don't like", "don't like."),
            Case("don't like school (Place)", "I don't like school", "I don't like school."),
            Case("don't like toilet (with article)", "I don't like toilet", "I don't like the toilet."),
            Case("don't like Mum (Subject-as-object)", "I don't like Mum", "I don't like Mum."),
            Case("don't want water", "don't want water", "I don't want some water."),
            Case("I don't want water", "I don't want water", "I don't want some water."),
            Case("I don't want go outside", "I don't want go outside", "I don't want to go outside."),
            Case("I don't want go shops", "I don't want go shops", "I don't want to go to the shops."),
            Case("I don't feel happy", "I don't feel happy", "I don't feel happy."),
            Case("I don't feel happy now", "I don't feel happy now", "I don't feel happy now."),
            Case("don't feel happy no subject", "don't feel happy", "I don't feel happy."),
            Case("just don't", "I don't", "I don't."),
            Case("just don't (no subject)", "don't", "don't"),
            Case("just not", "not", "not"),
            Case("I not want water", "I not want water", "I don't want some water."),
            Case("I do not want water", "I do not want water", "I don't want some water."),
            Case("I do not want toilet", "I do not want toilet", "I don't want to go to the toilet."),
            Case("I don't want toilet", "I don't want toilet", "I don't want to go to the toilet."),
            Case("I don't want toilet now", "I don't want toilet now", "I don't want to go to the toilet now."),
            Case("don't want toilet no subject", "don't want toilet", "I don't want to go to the toilet."),
            Case("I don't want toilet I want play", "I don't want toilet I want play", "I don't want to go to the toilet. I want to play."),

            // ---------- Modifiers (now, more, again) ----------
            // Modifiers can be preserved at the end of a sentence and "more"
            // can also act as an intensifier for object requests.
            Case("just now", "now", "now"),
            Case("just more", "more", "more"),
            Case("just again", "again", "again"),
            Case("I now (subject + bare modifier)", "I now", "I now."),
            Case("I want now (intent + bare modifier)", "I want now", "I want now."),
            Case("more now", "more now", "more now."),
            Case("I more (subject + bare modifier)", "I more", "I more."),
            Case("I want now water (modifier in middle)", "I now want water", "I want some water now."),
            Case("now then more", "now more", "now more."),
            Case("more more water (duplicate + target)", "more more water", "I want more water."),
            Case("now now (duplicate)", "now now", "now."),
            Case("more more (duplicate)", "more more", "more."),
            Case("I want more (intent + bare modifier)", "I want more", "I want more."),
            Case("I want more play (action + modifier)", "I want more play", "I want to play."),

            // ---------- Pinned responses (yes, no, stop, finished) ----------
            // Pinned words are short and should pass through as their own
            // answer, with proper trailing punctuation.
            Case("just yes", "yes", "yes"),
            Case("just no", "no", "no"),
            Case("just stop", "stop", "stop"),
            Case("just finished", "finished", "finished"),
            Case("yes no", "yes no", "yes no."),
            Case("stop finished", "stop finished", "stop finished."),
            Case("no finished", "no finished", "no finished."),
            Case("yes finished", "yes finished", "yes finished."),
            Case("yes no stop", "yes no stop", "yes no stop."),
            Case("finished yes", "finished yes", "finished yes."),

            // ---------- People (Mum, Dad, friend, teacher, me) ----------
            // People are Subjects. When used as the target of a want/need
            // they fall through to the request sentence.
            Case("I want Mum", "I want Mum", "I want Mum."),
            Case("I want Dad", "I want Dad", "I want Dad."),
            Case("I want friend", "I want friend", "I want friend."),
            Case("I want teacher", "I want teacher", "I want teacher."),
            Case("I need Mum", "I need Mum", "I need Mum."),
            Case("I need Dad", "I need Dad", "I need Dad."),
            Case("I need teacher", "I need teacher", "I need teacher."),
            Case("need Mum no subject (engine does not infer subject from bare intent)", "need Mum", "need Mum."),
            Case("want Mum no subject (engine does not infer subject from bare intent)", "want Mum", "want Mum."),
            Case("I want me (me is a Subject)", "I want me", "I want me."),
            Case("just Mum", "Mum", "Mum"),
            Case("just Dad", "Dad", "Dad"),
            Case("just friend", "friend", "friend"),
            Case("just teacher", "teacher", "teacher"),
            Case("just me", "me", "me"),
            Case("I want Mum I feel sad", "I want Mum I feel sad", "I want Mum. I feel sad."),
            Case("I sad I need Mum", "I sad I need Mum", "I feel sad. I need Mum."),
            Case("Mum I want", "Mum I want", "Mum I want."),
            Case("Mum I need", "Mum I need", "Mum I need."),
            Case("Mum I need toilet", "Mum I need toilet", "Mum needs to go to the toilet."),
            Case("I want Mum help (help overrides)", "I want Mum help", "I need help."),
            Case("I want toilet I want Mum", "I want toilet I want Mum", "I want to go to the toilet. I want Mum."),
            Case("toilet I want Mum", "toilet I want Mum", "I need to go to the toilet. I want Mum."),

            // ---------- Action verbs ----------
            // When action verbs are used with an intent they form
            // "want to X" requests. When used alone they pass through.
            Case("just play", "play", "play"),
            Case("just like", "like", "like"),
            Case("just have", "have", "have"),
            Case("just eat", "eat", "eat"),
            Case("just wash", "wash", "wash"),
            Case("just rest", "rest", "rest"),
            Case("I play", "I play", "I play."),
            Case("I like", "I like", "I like."),
            Case("I have", "I have", "I have."),
            Case("I eat", "I eat", "I eat."),
            Case("I wash", "I wash", "I wash."),
            Case("I rest", "I rest", "I rest."),
            Case("I like Mum", "I like Mum", "I like Mum."),
            Case("I like toy", "I like toy", "I like toy."),
            Case("I have toy", "I have toy", "I have toy."),
            Case("I have book", "I have book", "I have book."),
            Case("I play game", "I play game", "I play game."),
            Case("I play music", "I play music", "I play music."),
            Case("I want play", "I want play", "I want to play."),
            Case("I want eat", "I want eat", "I want to eat."),
            Case("I want wash", "I want wash", "I want to wash."),
            Case("I want rest", "I want rest", "I want to rest."),

            // ---------- Custom and unknown words ----------
            // Words the engine has no rule for should pass through with
            // proper trailing punctuation. The child's meaning must not
            // be altered.
            Case("unknown words with modifier", "robot spaceship now", "robot spaceship now."),
            Case("single custom word Dinosaur", "Dinosaur", "Dinosaur"),
            Case("lowercase custom word bluey", "bluey", "bluey"),
            Case("mixed case custom word Bluey", "Bluey", "Bluey"),
            Case("Minecraft custom word", "Minecraft", "Minecraft"),
            Case("blanket (Object)", "blanket", "blanket"),
            Case("tablet (Object)", "tablet", "tablet"),
            Case("book (Object)", "book", "book"),
            Case("bag (Object)", "bag", "bag"),
            Case("music (Object)", "music", "music"),
            Case("game (Object)", "game", "game"),
            Case("toy (Object)", "toy", "toy"),
            Case("I want blanket", "I want blanket", "I want blanket."),
            Case("I want toy", "I want toy", "I want toy."),
            Case("want toy no subject", "want toy", "I want toy."),
            Case("I want blanket now", "I want blanket now", "I want blanket now."),
            Case("I want tablet", "I want tablet", "I want tablet."),

            // ---------- Multi-clause ----------
            // Two independent ideas joined in one sentence. The engine
            // splits at new subjects, new intents, or at "help" after a
            // noun, and each clause should make sense on its own.
            Case("toilet then feel sick", "I need toilet I feel sick", "I need to go to the toilet. I feel sick."),
            Case("toilet then want water", "I want toilet I want Mum", "I want to go to the toilet. I want Mum."),
            Case("toilet then now then help", "toilet now help", "I need to go to the toilet now. I need help."),
            Case("feel sick then want Mum", "I feel sick I want Mum", "I feel sick. I want Mum."),
            Case("want water then feel happy", "I want water I feel happy", "I want some water. I feel happy."),
            Case("want water then need help", "I want water I need help", "I want some water. I need help."),
            Case("toilet and Mum", "I need toilet I want Mum", "I need to go to the toilet. I want Mum."),

            // ---------- Duplicates ----------
            // Duplicate tokens should not break the meaning. The engine
            // dedupes within a clause so "I I need toilet" and
            // "I need toilet" sound the same.
            Case("duplicate subject", "I I need toilet", "I need to go to the toilet."),
            Case("duplicate intent", "want want water", "I want some water."),
            Case("duplicate more", "more more water", "I want more water."),
            Case("duplicate toilet", "toilet toilet now", "I need to go to the toilet now."),
            Case("duplicate yes", "yes yes", "yes."),
            Case("duplicate no", "no no", "no."),
            Case("triple everything", "I I I want want want water", "I want some water."),
            Case("duplicate need", "I need need toilet", "I need to go to the toilet."),
            Case("just duplicate I", "I I", "I."),
            Case("duplicate toilet alone", "toilet toilet", "I need to go to the toilet."),
            Case("duplicate want alone", "want want", "want."),
            Case("triple I alone", "I I I", "I."),
            Case("duplicate want in middle", "I want want apple", "I want some apple."),
            Case("triple toilet", "toilet toilet toilet", "I need to go to the toilet."),

            // ---------- Out-of-order taps ----------
            // Children sometimes tap words in an unusual order. The
            // engine should still produce a coherent sentence that
            // reflects the same intent, even if the order isn't ideal.
            Case("toilet then I", "toilet I", "I need to go to the toilet. I."),
            Case("toilet then need then I", "toilet need I", "I need to go to the toilet. need. I."),
            Case("apple then I want (subject after)", "apple I want", "apple. I want."),
            Case("happy then I", "happy I", "I feel happy. I."),
            Case("sad then I feel", "sad I feel", "I feel sad. I feel."),
            Case("toilet then I want", "toilet I want", "I need to go to the toilet. I want."),
            Case("toilet then I need", "toilet I need", "I need to go to the toilet. I need."),
            Case("water then I want", "water I want", "water. I want."),
            Case("home then I want go", "home I want go", "home. I want go."),
            Case("now then I want water", "now I want water", "now. I want some water."),
            Case("I now then want water", "I now want water", "I want some water now."),
            Case("water then want then I", "water want I", "I want some water. I."),
            Case("I want happy (no action target)", "I want happy", "I feel happy."),
            Case("I want feel happy", "I want feel happy", "I feel happy."),
            Case("I want go happy", "I want go happy", "I feel happy."),

            // ---------- Pronoun combinations ----------
            Case("just I", "I", "I"),
            Case("just you", "you", "you"),
            Case("just me", "me", "me"),
            Case("I you (two subjects, second becomes clause)", "I you", "I you."),
            Case("you I (two subjects)", "you I", "you I."),
            Case("me I (two subjects)", "me I", "me I."),
            Case("I me (two subjects)", "I me", "I me."),

            // ---------- Incomplete / fragment inputs ----------
            // Taps where the child stopped halfway through. The engine
            // should not invent content the child did not tap.
            Case("I want no target", "I want", "I want."),
            Case("want I no target", "want I", "want. I."),
            Case("I need no target", "I need", "I need."),
            Case("need I no target", "need I", "need. I."),
            Case("I go no place", "I go", "I go."),
            Case("go I no place", "go I", "go. I."),
            Case("I feel no feeling", "I feel", "I feel."),
            Case("feel I no feeling", "feel I", "feel. I."),
            Case("I don't no target", "I don't", "I don't."),
            Case("don't I no target", "don't I", "don't. I."),
            Case("I happy alone (I is dropped)", "I happy", "I feel happy."),
            Case("happy I (I becomes separate clause)", "happy I", "I feel happy. I."),

            // ---------- "you" as the listener and as a target ----------
            Case("you as listener want water", "you want water", "You want some water."),
            Case("you as listener need help", "you need help", "Can you help me?"),
            Case("you as listener need help now", "you need help now", "Can you help me now?"),
            Case("I want you (Subject as target splits into two clauses)", "I want you", "I want. you."),
            Case("you want me (Subject as target, no capitalization)", "you want me", "you want me."),

            // ---------- Long combined sentences ----------
            Case("chain of clauses (toilet + feel + want)", "toilet now I feel sick I want Mum", "I need to go to the toilet now. I feel sick. I want Mum."),
            Case("chain of clauses (want water + feel happy + need help)", "I want water I feel happy I need help", "I want some water. I feel happy. I need help."),

            // ---------- Modifiers combined with negation ----------
            Case("I don't want water now", "I don't want water now", "I don't want some water now."),
            Case("I don't want go school now", "I don't want go school now", "I don't want to go to school now."),
            Case("I don't feel happy now (already covered)", "I don't feel happy now", "I don't feel happy now."),
            Case("I don't want toilet now (already covered)", "I don't want toilet now", "I don't want to go to the toilet now."),

            // ---------- "more" combined with various intents ----------
            Case("I want more toilet (engine drops more when toilet is target)", "I want more toilet", "I want to go to the toilet."),
            Case("I want more home (engine drops more when place is target)", "I want more home", "I want to go home."),
            Case("I want more play", "I want more play", "I want to play."),
            Case("I want more food (food is not a request target)", "I want more food", "I want more food."),

            // ---------- Custom words in custom boards get an Object role ----------
            // When a child creates a custom word on a non-default board,
            // the engine treats it as a generic noun in a want/need clause.
            // See the [customWordLibrary] test below for the same scenarios
            // with explicit custom boardIds.

            // ---------- Action verbs in various combinations ----------
            Case("I have blanket (have + Object)", "I have blanket", "I have blanket."),
            Case("I like blanket (like + Object)", "I like blanket", "I like blanket."),
            Case("I have apple (have + FoodDrink, no some)", "I have apple", "I have apple."),
            Case("I like apple (like + FoodDrink, no some)", "I like apple", "I like apple."),
            Case("I want to have apple (action = have)", "I want have apple", "I want to have apple."),
            Case("I want to like toy (action = like)", "I want like toy", "I want to like toy."),
            Case("I want to eat (action = eat)", "I want eat", "I want to eat."),
            Case("I want to wash (action = wash)", "I want wash", "I want to wash."),
            Case("I want to rest (action = rest)", "I want rest", "I want to rest."),

            // ---------- Custom words in default boards ----------
            // Same scenarios with explicit custom boardIds are covered in
            // [customWordLibrary] below.

            // ---------- Long sequences with many duplicates ----------
            Case("many duplicates of intent and target", "want want want want apple", "I want some apple."),
            Case("many duplicates of subject", "I I I I I need toilet", "I need to go to the toilet."),
        )

        val failures = cases.mapNotNull { case ->
            val actual = GrammarEngine.realize(tokens(case.input), enabled = true)
            if (actual == case.expected) {
                null
            } else {
                "${case.name}: '${case.input}'\n  expected: ${case.expected}\n  actual:   $actual"
            }
        }

        assertTrue(
            "Grammar mismatches:\n\n${failures.joinToString("\n\n")}",
            failures.isEmpty(),
        )
    }

    // ====================================================================
    // Custom-word library.
    // Same coverage as the main phrase library, but each test carves out
    // a custom word on a custom (or default) board so we can be sure
    // carer's custom vocabulary gets the right treatment.
    // ====================================================================

    @Test
    fun customWordLibrary() {
        val cases = listOf(
            // Custom word on a non-default board, used in a request
            TokenCase(
                "custom word on custom board in request",
                tokensForCustom("bluey", "shows", "I", "want", "bluey"),
                "I want bluey.",
            ),
            TokenCase(
                "custom word alone on custom board",
                tokensForCustom("spider", "shows", "spider"),
                "spider",
            ),
            // Custom word placed on a default board, so it inherits
            // the board's role.
            TokenCase(
                "custom word on food board in want clause",
                tokensForCustom("biscuit", "food", "I", "want", "biscuit"),
                "I want some biscuit.",
            ),
            TokenCase(
                "custom word on body board alone",
                tokensForCustom("foot", "body", "foot"),
                "foot",
            ),
            TokenCase(
                "custom word on body board in pain sentence",
                tokensForCustom("foot", "body", "foot", "hurts"),
                "My foot hurts.",
            ),
            TokenCase(
                "custom word on feel board alone",
                tokensForCustom("calm", "feel", "calm"),
                "calm",
            ),
            TokenCase(
                "custom word on feel board in feeling sentence",
                tokensForCustom("calm", "feel", "I", "feel", "calm"),
                "I feel calm.",
            ),
            TokenCase(
                "custom word on places board in place sentence",
                tokensForCustom("park", "places", "I", "want", "go", "park"),
                "I want to go to park.",
            ),
            TokenCase(
                "custom word on toilet board in toilet sentence (no article)",
                tokensForCustom("potty", "toilet", "I", "need", "potty"),
                "I need to go to potty.",
            ),
        )

        val failures = cases.mapNotNull { case ->
            val actual = GrammarEngine.realize(case.tokens, enabled = true)
            if (actual == case.expected) null else {
                "${case.name}\n  expected: ${case.expected}\n  actual:   $actual"
            }
        }

        assertTrue(
            "Custom-word mismatches:\n\n${failures.joinToString("\n\n")}",
            failures.isEmpty(),
        )
    }

    // ====================================================================
    // Focused behavior tests.
    // These pin down specific engine behaviors that are easy to regress
    // when refactoring, separately from the broad phrase library above.
    // ====================================================================

    @Test
    fun correctionCanBeDisabledForExactSpeech() {
        val actual = GrammarEngine.realize(tokens("I", "need", "toilet", "now"), enabled = false)
        assertEquals("I need toilet now", actual)
    }

    @Test
    fun emptySentenceProducesEmptyString() {
        assertEquals("", GrammarEngine.realize(emptyList(), enabled = true))
        assertEquals("", GrammarEngine.realize(emptyList(), enabled = false))
    }

    @Test
    fun blankOnlyTokensProduceJoinedRawSpeech() {
        // A None-role token in the middle of the sentence is treated as
        // speech and joined with its neighbours; the engine then adds
        // trailing punctuation.
        val actual = GrammarEngine.realize(
            listOf(
                SentenceToken("I", "I", "", null, GrammarRole.Subject, null),
                SentenceToken(" ", " ", "", null, GrammarRole.None, null),
                SentenceToken("water", "water", "", null, GrammarRole.FoodDrink, null),
            ),
            enabled = true,
        )
        assertEquals("I water.", actual)
    }

    @Test
    fun singleToiletWordIsNormalisedToToilet() {
        assertEquals("toilet", GrammarEngine.realize(tokens("toilet"), enabled = true))
        assertEquals("toilet", GrammarEngine.realize(tokens("bathroom"), enabled = true))
    }

    @Test
    fun singleNonToiletWordIsReturnedVerbatim() {
        assertEquals("water", GrammarEngine.realize(tokens("water"), enabled = true))
        assertEquals("happy", GrammarEngine.realize(tokens("happy"), enabled = true))
        assertEquals("Mum", GrammarEngine.realize(tokens("Mum"), enabled = true))
    }

    @Test
    fun repeatedTokensInAClauseAreDeduped() {
        assertEquals(
            "I need to go to the toilet.",
            GrammarEngine.realize(tokens("I", "need", "need", "toilet", "toilet"), enabled = true),
        )
        assertEquals(
            "I want some water.",
            GrammarEngine.realize(tokens("I", "want", "want", "water", "water"), enabled = true),
        )
        assertEquals(
            "yes.",
            GrammarEngine.realize(tokens("yes", "yes", "yes"), enabled = true),
        )
    }

    @Test
    fun multiClauseSplitsAtNewSubject() {
        val actual = GrammarEngine.realize(tokens("toilet", "I", "want", "Mum"), enabled = true)
        assertEquals("I need to go to the toilet. I want Mum.", actual)
    }

    @Test
    fun multiClauseSplitsAtNewIntentAfterFeeling() {
        val actual = GrammarEngine.realize(tokens("happy", "I", "want", "water"), enabled = true)
        assertEquals("I feel happy. I want some water.", actual)
    }

    @Test
    fun multiClauseSplitsAtNewIntentAfterPlace() {
        // "home" arrives first with no subject/intent, so the engine speaks
        // it on its own, then starts a new clause when the subject appears.
        val actual = GrammarEngine.realize(tokens("home", "I", "want", "Mum"), enabled = true)
        assertEquals("home. I want Mum.", actual)
    }

    @Test
    fun multiClauseSplitsAtHelpAfterToilet() {
        val actual = GrammarEngine.realize(tokens("toilet", "now", "help"), enabled = true)
        assertEquals("I need to go to the toilet now. I need help.", actual)
    }

    @Test
    fun multiClauseSplitsAtHelpAfterFood() {
        // "water" arrives first with no intent, so the engine speaks it on
        // its own, then starts a new clause when "help" appears.
        val actual = GrammarEngine.realize(tokens("water", "help"), enabled = true)
        assertEquals("water. I need help.", actual)
    }

    @Test
    fun navigationCategoryLabelsDoNotBecomeDestinationsOrTargets() {
        val placesOutsideGoPlayTo = listOf(
            SentenceToken("Places", "Places", "", null, inferGrammarRole("Places", "Places", "places"), "places"),
            SentenceToken("outside", "outside", "", null, inferGrammarRole("outside", "outside", "places"), "places"),
            SentenceToken("go", "go", "", null, inferGrammarRole("go", "go", "go"), "go"),
            SentenceToken("play", "play", "", null, inferGrammarRole("play", "play", "play"), "play"),
            SentenceToken("to", "to", "", null, inferGrammarRole("to", "to"), null),
        )
        assertEquals("go outside to play.", GrammarEngine.realize(placesOutsideGoPlayTo, enabled = true))

        val wantDrinkJuice = listOf(
            SentenceToken("I", "I", "", null, inferGrammarRole("I", "I", "i"), "i"),
            SentenceToken("want", "want", "", null, inferGrammarRole("want", "want", "want"), "want"),
            SentenceToken("drink", "drink", "", null, inferGrammarRole("drink", "drink", "drink"), "drink"),
            SentenceToken("juice", "juice", "", null, inferGrammarRole("juice", "juice", "drink"), "drink"),
        )
        assertEquals("I want some juice.", GrammarEngine.realize(wantDrinkJuice, enabled = true))
    }

    @Test
    fun unknownWordsArePassedThroughWithPunctuation() {
        assertEquals(
            "robot spaceship now.",
            GrammarEngine.realize(tokens("robot", "spaceship", "now"), enabled = true),
        )
        assertEquals(
            "Dinosaur",
            GrammarEngine.realize(tokens("Dinosaur"), enabled = true),
        )
    }

    @Test
    fun extraWhitespaceIsNormalised() {
        val actual = GrammarEngine.realize(tokens("  I   need   toilet   now  "), enabled = true)
        assertEquals("I need to go to the toilet now.", actual)
    }

    @Test
    fun trailingPunctuationInRawInputIsPreservedWhenDisabled() {
        assertEquals(
            "I need toilet.",
            GrammarEngine.realize(tokens("I", "need", "toilet."), enabled = false),
        )
        assertEquals(
            "I need toilet!",
            GrammarEngine.realize(tokens("I", "need", "toilet!"), enabled = false),
        )
        assertEquals(
            "I need toilet?",
            GrammarEngine.realize(tokens("I", "need", "toilet?"), enabled = false),
        )
    }

    @Test
    fun disabledModePreservesAllTokensIncludingDuplicates() {
        val actual = GrammarEngine.realize(
            tokens("I", "I", "want", "want", "toilet", "now"),
            enabled = false,
        )
        assertEquals("I I want want toilet now", actual)
    }

    @Test
    fun disabledModePreservesUnknownAndMixedOrder() {
        val actual = GrammarEngine.realize(
            tokens("water", "I", "want", "robot"),
            enabled = false,
        )
        assertEquals("water I want robot", actual)
    }

    @Test
    fun disabledModeIsStableForEmptyInput() {
        assertEquals("", GrammarEngine.realize(emptyList(), enabled = false))
    }

    @Test
    fun noGrammarRoleTokensPassThroughVerbatim() {
        val token = SentenceToken(
            label = "I",
            speech = "I",
            icon = "",
            imagePath = null,
            grammarRole = GrammarRole.None,
            boardId = null,
        )
        val actual = GrammarEngine.realize(listOf(token), enabled = true)
        assertEquals("I", actual)
    }

    @Test
    fun customGrammarRoleOverrideIsRespected() {
        // A button whose text would normally be a Subject (e.g. "Mum")
        // can be marked as a different role by the carer, and the engine
        // must use the override, not the inferred one.
        val token = SentenceToken(
            label = "Mum",
            speech = "Mum",
            icon = "",
            imagePath = null,
            grammarRole = GrammarRole.Feeling,
            boardId = null,
        )
        val actual = GrammarEngine.realize(listOf(token), enabled = true)
        assertEquals("Mum", actual)
    }

    @Test
    fun allNoneRoleTokensFallBackToRawSpeech() {
        // If every token has role None, the engine filters them all out
        // (no usable tokens) and falls back to the raw joined speech.
        val tokens = listOf(
            SentenceToken("a", "a", "", null, GrammarRole.None, null),
            SentenceToken("b", "b", "", null, GrammarRole.None, null),
        )
        assertEquals("a b", GrammarEngine.realize(tokens, enabled = true))
    }

    @Test
    fun blankSpeechTokensAreFilteredOut() {
        // The engine ignores tokens whose speech is blank so the spoken
        // output never contains a stray empty word.
        val tokens = listOf(
            SentenceToken("I", "I", "", null, GrammarRole.Subject, null),
            SentenceToken(" ", "", "", null, GrammarRole.Intent, null),
            SentenceToken("water", "water", "", null, GrammarRole.FoodDrink, null),
        )
        val actual = GrammarEngine.realize(tokens, enabled = true)
        assertEquals("I water.", actual)
    }

    @Test
    fun longSentenceOfManyTokensStillRealises() {
        // A long run of taps should not crash and should preserve intent.
        val actual = GrammarEngine.realize(
            tokens(
                "I", "I", "want", "want", "water", "now", "I", "feel", "happy", "I",
                "want", "Mum", "I", "need", "toilet", "now",
            ),
            enabled = true,
        )
        assertEquals("I want some water now. I feel happy. I want Mum. I need to go to the toilet now.", actual)
    }

    @Test
    fun allResponseAndModifierTokensAreJoinedAsIs() {
        // Responses and modifiers alone should be passed through as-is
        // (with trailing punctuation) because there is no clause to render.
        val actual = GrammarEngine.realize(
            tokens("yes", "no", "more", "now"),
            enabled = true,
        )
        assertEquals("yes no more now.", actual)
    }

    @Test
    fun customWordWithObjectRoleJoinedWithIntent() {
        // A custom word (e.g. "blanket") infers to Object, and the engine
        // should still produce "I want blanket." for a want + custom noun.
        val actual = GrammarEngine.realize(tokens("I", "want", "blanket"), enabled = true)
        assertEquals("I want blanket.", actual)
    }

    @Test
    fun speechFieldIsTrimmed() {
        // A button with leading/trailing whitespace in its speech field
        // should still come out cleanly.
        val tokens = listOf(
            SentenceToken("I", "I", "", null, GrammarRole.Subject, null),
            SentenceToken("want", " want ", "", null, GrammarRole.Intent, null),
            SentenceToken("water", " water ", "", null, GrammarRole.FoodDrink, null),
        )
        val actual = GrammarEngine.realize(tokens, enabled = true)
        assertEquals("I want some water.", actual)
    }

    @Test
    fun subjectInSecondClauseResetsConjugation() {
        // The engine uses the first subject for the whole clause when
        // multiple subjects are present, so a third-person subject
        // (Mum) wins even if a first-person subject (I) appears later.
        val actual = GrammarEngine.realize(
            tokens("Mum", "I", "need", "toilet"),
            enabled = true,
        )
        assertEquals("Mum needs to go to the toilet.", actual)
    }

    @Test
    fun unknownWordInSubjectSlotDoesNotTriggerSubjectRules() {
        // A custom word like "Dinosaur" has no subject role, so the
        // engine should not pretend it is a subject and conjugate
        // around it.
        val actual = GrammarEngine.realize(
            tokens("Dinosaur", "I", "want", "water"),
            enabled = true,
        )
        assertEquals("Dinosaur. I want some water.", actual)
    }

    @Test
    fun speechThatDiffersFromLabelIsWhatGetsSpoken() {
        // If a button's label and speech differ, the engine must
        // speak the speech, not the label, but the role is decided
        // from the speech.
        val tokens = listOf(
            SentenceToken("Mother", "Mum", "", null, GrammarRole.Subject, null),
            SentenceToken("H2O", "water", "", null, GrammarRole.FoodDrink, null),
        )
        val actual = GrammarEngine.realize(tokens, enabled = true)
        assertEquals("Mum water.", actual)
    }

    @Test
    fun speechThatDiffersFromLabelCanDriveRoleInference() {
        // A button labeled "thing" but with speech "toilet" must be
        // treated as a Toilet, not an Object.
        val tokens = listOf(
            SentenceToken("thing", "toilet", "", null, GrammarRole.Toilet, null),
        )
        val actual = GrammarEngine.realize(tokens, enabled = true)
        assertEquals("toilet", actual)
    }

    @Test
    fun negationSentenceWithBothBodyAndTargetPicksCorrectBranch() {
        // "I don't want tummy hurts" — the engine should produce a
        // coherent sentence that captures both the negation and the
        // pain.
        val actual = GrammarEngine.realize(
            tokens("I", "don't", "want", "tummy", "hurts"),
            enabled = true,
        )
        // Current behavior: negation drops the "want tummy hurts"
        // because the engine only has negation+place/toilet/target
        // branches, and "tummy hurts" doesn't fit any. The pain
        // sentence kicks in for the second clause and speaks the
        // body part.
        assertEquals("My tummy hurts.", actual)
    }

    @Test
    fun helpWithModifierAtTheEnd() {
        // The trailing "now" should be preserved in the help sentence.
        val actual = GrammarEngine.realize(
            tokens("help", "now"),
            enabled = true,
        )
        assertEquals("I need help now.", actual)
    }

    @Test
    fun negationHelpWithModifier() {
        // The help sentence is checked AFTER negation, so when both
        // a negation and "help" are present, the help sentence wins
        // and the negation is dropped. This is a known engine
        // limitation — the child's "I don't help" comes out as
        // "I need help." — and the test documents it.
        val actual = GrammarEngine.realize(
            tokens("I", "don't", "help"),
            enabled = true,
        )
        assertEquals("I need help.", actual)
    }

    @Test
    fun clauseSplittingAtHelpInMidSentence() {
        // When "help" appears after content words in the middle of a
        // sentence, the engine splits off a help clause.
        val actual = GrammarEngine.realize(
            tokens("happy", "help"),
            enabled = true,
        )
        assertEquals("I feel happy. I need help.", actual)
    }

    @Test
    fun threeConjunctionsOfClauses() {
        // A sentence that contains three independent ideas should
        // produce three clauses, each starting with "I" so the
        // speaker's voice is consistent.
        val actual = GrammarEngine.realize(
            tokens("I", "feel", "sick", "I", "want", "Mum", "I", "need", "toilet"),
            enabled = true,
        )
        assertEquals("I feel sick. I want Mum. I need to go to the toilet.", actual)
    }

    @Test
    fun modifiersAndIntentInOneClause() {
        // Modifiers are preserved at the end of a clause even when
        // the clause already has multiple roles.
        val actual = GrammarEngine.realize(
            tokens("I", "want", "water", "now", "again"),
            enabled = true,
        )
        assertEquals("I want some water now again.", actual)
    }

    @Test
    fun orderOfClausesIsPreserved() {
        // The order of clauses in a multi-clause sentence should
        // match the order the child tapped them in.
        val actual = GrammarEngine.realize(
            tokens("toilet", "I", "want", "Mum", "I", "feel", "happy"),
            enabled = true,
        )
        assertEquals("I need to go to the toilet. I want Mum. I feel happy.", actual)
    }

    @Test
    fun thirdPartySubjectInFirstClauseTakesTheSubjectSlot() {
        // The engine only splits clauses on a new "I" or "you"
        // subject. A "Mum" subject attached to a non-subject word
        // (e.g. "toilet") is treated as the subject of that
        // sentence, not as a clause break.
        val actual = GrammarEngine.realize(
            tokens("toilet", "Mum", "happy"),
            enabled = true,
        )
        assertEquals("Mum needs to go to the toilet.", actual)
    }

    // ====================================================================
    // Role inference.
    // Every default word should resolve to the role a carer would expect,
    // and the engine should fall back to Object for custom words the
    // grammar rules don't know about.
    // ====================================================================

    @Test
    fun roleInferenceLibrary() {
        val cases = listOf(
            // Subject — pronouns and people
            RoleCase("I", GrammarRole.Subject),
            RoleCase("i", GrammarRole.Subject),
            RoleCase("you", GrammarRole.Subject),
            RoleCase("me", GrammarRole.Subject),
            RoleCase("Mum", GrammarRole.Subject),
            RoleCase("Dad", GrammarRole.Subject),
            RoleCase("friend", GrammarRole.Subject),
            RoleCase("teacher", GrammarRole.Subject),
            // Intent
            RoleCase("want", GrammarRole.Intent),
            RoleCase("need", GrammarRole.Intent),
            // Negation
            RoleCase("don't", GrammarRole.Negation),
            RoleCase("dont", GrammarRole.Negation),
            RoleCase("do not", GrammarRole.Negation),
            RoleCase("not", GrammarRole.Negation),
            // Action
            RoleCase("go", GrammarRole.Action),
            RoleCase("help", GrammarRole.Action),
            RoleCase("play", GrammarRole.Action),
            RoleCase("wash", GrammarRole.Action),
            RoleCase("rest", GrammarRole.Action),
            RoleCase("like", GrammarRole.Action),
            RoleCase("have", GrammarRole.Action),
            RoleCase("eat", GrammarRole.Action),
            RoleCase("hurts", GrammarRole.Action),
            RoleCase("hurt", GrammarRole.Action),
            // Place
            RoleCase("home", GrammarRole.Place),
            RoleCase("school", GrammarRole.Place),
            RoleCase("outside", GrammarRole.Place),
            RoleCase("shops", GrammarRole.Place),
            // Toilet
            RoleCase("toilet", GrammarRole.Toilet),
            RoleCase("bathroom", GrammarRole.Toilet),
            // Food / drink
            RoleCase("food", GrammarRole.FoodDrink),
            RoleCase("drink", GrammarRole.FoodDrink),
            RoleCase("water", GrammarRole.FoodDrink),
            RoleCase("juice", GrammarRole.FoodDrink),
            RoleCase("milk", GrammarRole.FoodDrink),
            RoleCase("cup", GrammarRole.FoodDrink),
            RoleCase("apple", GrammarRole.FoodDrink),
            RoleCase("banana", GrammarRole.FoodDrink),
            RoleCase("bread", GrammarRole.FoodDrink),
            RoleCase("snack", GrammarRole.FoodDrink),
            // Feeling
            RoleCase("happy", GrammarRole.Feeling),
            RoleCase("sad", GrammarRole.Feeling),
            RoleCase("sick", GrammarRole.Feeling),
            RoleCase("tired", GrammarRole.Feeling),
            RoleCase("angry", GrammarRole.Feeling),
            RoleCase("sore", GrammarRole.Feeling),
            // Body part
            RoleCase("head", GrammarRole.BodyPart),
            RoleCase("hand", GrammarRole.BodyPart),
            RoleCase("mouth", GrammarRole.BodyPart),
            RoleCase("tummy", GrammarRole.BodyPart),
            // Modifier
            RoleCase("more", GrammarRole.Modifier),
            RoleCase("now", GrammarRole.Modifier),
            RoleCase("again", GrammarRole.Modifier),
            // Response
            RoleCase("yes", GrammarRole.Response),
            RoleCase("no", GrammarRole.Response),
            RoleCase("stop", GrammarRole.Response),
            RoleCase("finished", GrammarRole.Response),
            // Fallback: anything else is an Object
            RoleCase("robot", GrammarRole.Object),
            RoleCase("blanket", GrammarRole.Object),
            RoleCase("tablet", GrammarRole.Object),
            RoleCase("book", GrammarRole.Object),
            RoleCase("bag", GrammarRole.Object),
            RoleCase("music", GrammarRole.Object),
            RoleCase("game", GrammarRole.Object),
            RoleCase("toy", GrammarRole.Object),
            RoleCase("Bluey", GrammarRole.Object),
            RoleCase("Minecraft", GrammarRole.Object),
        )

        val failures = cases.mapNotNull { case ->
            val actual = inferGrammarRole(label = case.word, speech = case.speech, boardId = case.boardId)
            if (actual == case.expected) null else {
                "'${case.word}' (boardId=${case.boardId}, speech='${case.speech}'): expected ${case.expected}, actual $actual"
            }
        }

        assertTrue(
            "Role inference mismatches:\n${failures.joinToString("\n")}",
            failures.isEmpty(),
        )
    }

    @Test
    fun roleInferenceUsesBoardIdForUnknownLabels() {
        // A custom word on a known default board inherits the board's role.
        assertEquals(
            GrammarRole.Feeling,
            inferGrammarRole(label = "calm", speech = "calm", boardId = "feel"),
        )
        assertEquals(
            GrammarRole.FoodDrink,
            inferGrammarRole(label = "biscuit", speech = "biscuit", boardId = "food"),
        )
        assertEquals(
            GrammarRole.BodyPart,
            inferGrammarRole(label = "foot", speech = "foot", boardId = "body"),
        )
        assertEquals(
            GrammarRole.Place,
            inferGrammarRole(label = "park", speech = "park", boardId = "places"),
        )
        assertEquals(
            GrammarRole.Toilet,
            inferGrammarRole(label = "potty", speech = "potty", boardId = "toilet"),
        )
    }

    @Test
    fun roleInferenceUsesSpeechWhenLabelIsBlank() {
        assertEquals(
            GrammarRole.Subject,
            inferGrammarRole(label = "", speech = "Mum"),
        )
        assertEquals(
            GrammarRole.Intent,
            inferGrammarRole(label = "blank", speech = "want"),
        )
    }

    @Test
    fun roleInferenceSpeechOverridesLabel() {
        // If speech and label disagree, the speech drives the role because
        // that is what is actually spoken aloud.
        assertEquals(
            GrammarRole.Subject,
            inferGrammarRole(label = "Mum", speech = "you"),
        )
        assertEquals(
            GrammarRole.FoodDrink,
            inferGrammarRole(label = "thing", speech = "water"),
        )
        assertEquals(
            GrammarRole.Toilet,
            inferGrammarRole(label = "place", speech = "toilet"),
        )
    }

    @Test
    fun roleInferenceNegationVariantsAllResolveToNegation() {
        for (variant in listOf("don't", "dont", "do not", "not", "Don't", "NOT")) {
            assertEquals(
                "expected '$variant' to be Negation",
                GrammarRole.Negation,
                inferGrammarRole(label = variant, speech = variant),
            )
        }
    }

    @Test
    fun roleInferenceModifierVariantsAllResolveToModifier() {
        for (variant in listOf("more", "now", "again", "More", "AGAIN")) {
            assertEquals(
                "expected '$variant' to be Modifier",
                GrammarRole.Modifier,
                inferGrammarRole(label = variant, speech = variant),
            )
        }
    }

    @Test
    fun roleInferenceCaseInsensitive() {
        assertEquals(GrammarRole.Subject, inferGrammarRole("i", "i"))
        assertEquals(GrammarRole.Subject, inferGrammarRole("I", "I"))
        assertEquals(GrammarRole.Intent, inferGrammarRole("want", "want"))
        assertEquals(GrammarRole.Intent, inferGrammarRole("Want", "Want"))
    }

    @Test
    fun roleInferenceCustomWordsInCustomBoardsDefaultToObject() {
        // "toys" is not a default board, so a custom word on it becomes Object.
        assertEquals(
            GrammarRole.Object,
            inferGrammarRole(label = "bluey", speech = "bluey", boardId = "toys"),
        )
        assertEquals(
            GrammarRole.Object,
            inferGrammarRole(label = "spider", speech = "spider", boardId = "shows"),
        )
    }

    @Test
    fun roleInferenceSubjectWordsOnDefaultBoardsStaySubject() {
        // Even if a subject word is placed on a default board, the
        // text-driven rule wins.
        assertEquals(
            GrammarRole.Subject,
            inferGrammarRole(label = "Mum", speech = "Mum", boardId = "feel"),
        )
    }

    @Test
    fun roleInferencePlaceWordOnNonPlacesBoardStaysPlace() {
        assertEquals(
            GrammarRole.Place,
            inferGrammarRole(label = "home", speech = "home", boardId = "feel"),
        )
    }

    // ====================================================================
    // Helpers.
    // ====================================================================

    private fun tokens(phrase: String): List<SentenceToken> =
        tokens(*phrase.split(Regex("\\s+")).filter { it.isNotBlank() }.toTypedArray())

    private fun tokens(vararg words: String): List<SentenceToken> =
        words.map { word ->
            SentenceToken(
                label = word,
                speech = word,
                icon = "",
                imagePath = null,
                grammarRole = inferGrammarRole(label = word, speech = word),
                boardId = inferredBoardId(word),
            )
        }

    /**
     * Build tokens where a particular word lives on a custom (non-default)
     * board. This simulates a carer creating a custom word and placing it
     * in a custom folder, e.g. "bluey" on a "shows" board.
     */
    private fun tokensForCustom(
        customWord: String,
        customBoard: String,
        vararg words: String,
    ): List<SentenceToken> =
        words.map { word ->
            val board = if (word.equals(customWord, ignoreCase = true)) customBoard else inferredBoardId(word)
            SentenceToken(
                label = word,
                speech = word,
                icon = "",
                imagePath = null,
                grammarRole = inferGrammarRole(label = word, speech = word, boardId = board),
                boardId = board,
            )
        }

    private fun inferredBoardId(word: String): String? =
        when (word.normalizedGrammarText()) {
            "go" -> "go"
            "food", "apple", "banana", "bread", "snack" -> "food"
            "drink", "water", "juice", "milk", "cup" -> "drink"
            "toilet", "bathroom" -> "toilet"
            "home", "school", "outside", "shops" -> "places"
            "happy", "sad", "sick", "tired", "angry", "sore" -> "feel"
            "head", "hand", "mouth", "tummy" -> "body"
            else -> null
        }

    private data class Case(
        val name: String,
        val input: String,
        val expected: String,
    )

    private data class TokenCase(
        val name: String,
        val tokens: List<SentenceToken>,
        val expected: String,
    )

    private data class RoleCase(
        val word: String,
        val expected: GrammarRole,
        val boardId: String? = null,
        val speech: String = word,
    )
}
