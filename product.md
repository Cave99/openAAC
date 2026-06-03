# openAAC Product Plan

## Purpose

openAAC is a free, open-source AAC application for Android tablets. It is primarily designed for non-speaking autistic children aged 6 to 12, while also supporting carers, parents, teachers, therapists, and other trusted adults who configure vocabulary, simplify communication, and model language for the child.

AAC should not be locked behind expensive apps, paid icon packs, subscriptions, accounts, or cloud services. openAAC exists to give families and carers a practical, local-first communication tool that can be customized to the child without ongoing cost.

## Core Ideology

- Free forever.
- Open source.
- Android tablet first.
- Local only by default and by design.
- No sign in, no sign up, no cloud database, no hosted user data.
- No paid icon packs, paid features, subscriptions, or dark patterns.
- User vocabulary, images, usage history, and configuration stay on the device.
- Customization should be simple enough for carers to use without technical knowledge.
- The child-facing interface should be predictable, glanceable, and hard to accidentally break.
- Every important communication path should be reachable quickly.
- The app should help the user communicate, not force them through unnecessary navigation.

## Primary Users

### Child User

The first version targets non-speaking autistic children aged 6 to 12 using an Android tablet. The child should be able to tap icons and words to build messages without needing to read fluently.

The app should support:

- Fast access to common needs.
- Visual recognition through icons and colors.
- Consistent placement of important buttons.
- Simple category flow.
- Immediate spoken feedback when a word is tapped.
- A sentence bar that can speak the full message.
- Back and home navigation when the user enters a path that is not useful.

### Carer/Admin User

Carers, parents, teachers, or therapists manage the app. They should be able to add, edit, arrange, and review vocabulary without needing a developer or external service.

The admin experience should support:

- Editing button text, icon, color, category, and position.
- Adding custom images from the tablet gallery.
- Taking a photo for a custom word.
- Managing vocabulary packs.
- Viewing local usage summaries.
- Resetting or repairing layouts if something goes wrong.
- Locking edit mode behind an admin passcode.

## Product Principles

### 1. Local Trust

openAAC should work offline forever. It should not depend on accounts, remote servers, cloud sync, network-hosted images, telemetry, or third-party analytics.

Local data may include:

- Vocabulary.
- Custom icons and photos.
- Board layouts.
- Local usage counts.
- Recent phrases.
- Admin settings.

The app should avoid requesting network permission unless a future optional feature has a very strong reason. The default product direction is no network access.

### 2. Communication First

Every screen should prioritize communication over configuration. The child-facing mode should feel stable, minimal, and fast.

The app should make it easy to say messages like:

- "I want food apple"
- "I want help"
- "go home"
- "stop"
- "more"
- "yes"
- "no"

The goal is not to perfectly model grammar in the first version. The goal is to support usable expression with low friction.

### 3. Flexible Pathways

Vocabulary should use a hybrid of categories and sentence flow.

Examples:

- Tapping "go" can reveal places such as "home", "school", "shops", "bathroom", and "kitchen".
- Tapping "I" can suggest likely next words such as "want", "need", "go", or "feel".
- Tapping "want" can suggest common request categories such as food, drink, help, activity, or object.

Everything should remain accessible through normal navigation, but common paths should become faster over time.

### 4. Stable Core Words

Some words should always be available because they are high-value and urgent.

Pinned words for the first version:

- yes
- no
- more
- help
- stop

These should remain visually stable and should not disappear when the user changes category.

### 5. Customizable From Day One

Custom vocabulary is not a future enhancement. It is core to the product.

From the first usable version, carers should be able to:

- Add a word.
- Add or choose an icon.
- Choose a button color.
- Place the button.
- Move the button.
- Assign it to a category or flow.
- Choose whether it appears in a vocabulary pack.

## Key Features

## Child-Facing Mode

- 4 row by 5 column communication grid for the first version.
- Sentence bar showing selected words with both icon and text.
- Tap a word to add it to the sentence bar.
- Word speaks immediately when tapped.
- Speak button speaks the full sentence.
- Delete last word button.
- Clear sentence button.
- Home button.
- Back button.
- Pinned urgent/common words.
- Visual-first buttons with text labels.
- Fast navigation through common sentence paths.

## Admin/Edit Mode

- Protected by an admin passcode.
- Edit vocabulary.
- Add new words.
- Add custom images from gallery.
- Take a photo for a word.
- Edit button color.
- Edit button position.
- Edit category or next-word relationships.
- Drag and drop button arrangement.
- Manage vocabulary packs.
- View local usage summaries.
- Reset layout to default if needed.

## Vocabulary Packs

Vocabulary packs should be configurable from day one.

Default vocabulary should include core and practical words, including:

- Pronouns and sentence starters such as "I", "you", "want", "need", "go".
- Common rooms and places such as "bathroom", "kitchen", "home", "school", "shops".
- People such as "Mum", "Dad", "friend", "teacher".
- Needs and actions such as "help", "stop", "more", "eat", "drink", "sleep".
- Feelings and body basics.
- Food and drink basics.

The default vocabulary should avoid highly specific fringe words such as Minecraft, Roblox, Bluey, or individual brands. Those should be easy for carers to add.

## Icon Strategy

openAAC should support a mix of:

- Original openAAC icons.
- Creative Commons compatible icons.
- Generated icons where licensing allows.
- User-provided photos and images.

The target visual style should be consistent, but the product should not block useful communication just because an icon differs in style.

Words should support multiple icon options so carers can choose the image that best matches the child.

## Voice And Speech

The most important speech requirement is that it works locally.

First version goals:

- Use local text-to-speech.
- Speak each word when tapped.
- Speak the full sentence on command.
- Global voice settings for voice, pitch, speed, and volume.
- English only.

Custom recorded audio is not part of the first version.

## Local Usage Insights

openAAC may collect local-only usage data to improve the user experience and help carers understand progress.

Examples:

- Most used words this week, month, and year.
- Number of unique words used.
- Common word sequences.
- Recently used phrases.
- Frequent paths after a selected word.

This data must stay on the device. It should never be uploaded, sold, synced, or used for external analytics.

## MVP Scope

The first meaningful MVP should include:

- Android tablet app.
- Local-only storage.
- Child communication mode.
- Admin/edit mode with passcode.
- 4x5 board layout.
- Sentence bar with icons and text.
- Tap-to-speak word behavior.
- Speak sentence button.
- Delete last word and clear sentence.
- Pinned words: yes, no, more, help, stop.
- Basic category and sentence-flow navigation.
- Configurable vocabulary packs.
- Add/edit custom words.
- Add images from gallery.
- Take photo for word image.
- Global local TTS settings.
- Local usage counters.

## Non-Goals

openAAC should not include these in the first version:

- User accounts.
- Sign in or sign up.
- Cloud sync.
- Remote database.
- Subscription features.
- Paid vocabulary or icon packs.
- Network-hosted image libraries.
- Online analytics.
- Phone-first UI.
- Multiple child profiles.
- Custom recorded audio.
- Non-English language support.
- AI cloud recommendations.

## Success Criteria

The MVP is successful if:

- A child can build and speak useful messages without reading fluently.
- A carer can add a new word and image without technical help.
- A carer can rearrange a board without risking the child-facing mode.
- The app works offline.
- The app stores all user data locally.
- Frequent communication paths are faster than manual category browsing.
- The interface remains stable and predictable during normal use.

