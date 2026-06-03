# openAAC Product Plan

## Purpose

openAAC is a free, public-source AAC application for Android tablets. It is primarily designed for non-speaking autistic children aged 6 to 12, while also supporting carers, parents, teachers, therapists, and other trusted adults who configure vocabulary, simplify communication, and model language for the child.

AAC should not be locked behind expensive apps, paid icon packs, subscriptions, accounts, or cloud services. openAAC exists to give families and carers a practical, local-first communication tool that can be customized to the child without ongoing cost.

The app launcher name should be `OpenAAC`. Inside the app, branding should be minimal because the product is a communication tool, not a marketing surface.

## Core Ideology

- Free forever.
- Public source under the PolyForm Noncommercial License 1.0.0.
- Android tablet first.
- Local only by default and by design.
- No sign in, no sign up, no cloud database, no hosted user data.
- No paid icon packs, paid features, subscriptions, or dark patterns.
- User vocabulary, images, usage history, and configuration stay on the device.
- Customization should be simple enough for carers to use without technical knowledge.
- The child-facing interface should be predictable, glanceable, and hard to accidentally break.
- Every important communication path should be reachable quickly.
- The app should help the user communicate, not force them through unnecessary navigation.
- The app should be free to use and customize, while preventing commercial resale or paid product reuse without explicit written approval from the project owner.

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

## First-Time Setup

The app should launch into a carer setup flow only on first use. After setup is complete, normal launches should open directly into child communication mode.

The setup flow should:

- Explain that openAAC is local-only.
- Explain that no vocabulary, images, sentences, or usage data leave the tablet.
- Avoid asking for the child's name or other personal identity details.
- Explain the temporary default admin passcode of `1234`.
- Force the carer to change the admin passcode before setup is complete.
- Confirm local usage tracking is enabled.
- Confirm voice settings, preferring Australian English where available.
- Explain that the app does not use the internet.

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

The app should not request internet permission. No feature in the first version should touch the internet.

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

Common phrases should be especially fast, ideally reachable in under 3 taps where practical. Examples include:

- "I want food"
- "I need toilet"
- "I need help"
- "go home"

### 4. Stable Core Words

Some words should always be available because they are high-value and urgent.

Pinned words for the first version:

- yes
- no
- more
- help
- stop

These should remain visually stable and should not disappear when the user changes category.

Pinned words should live in a separate fixed strip rather than consuming cells in the main 4x5 board.

The first layout should place the pinned strip at the bottom of the screen. Pinned words should speak immediately and add to the sentence bar when tapped.

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
- Landscape-only tablet layout, designed around a 12-inch tablet first.
- Sentence bar showing selected words with both icon and text.
- Tap a word to add it to the sentence bar.
- Word speaks immediately when tapped.
- Speak button speaks the full sentence.
- Backspace button in the sentence bar to delete the last word.
- Long-press backspace clears the full sentence.
- Question mark button in the sentence bar to mark the message as a question.
- Question marker should display as `?`, speak `hmm` when tapped, and avoid trying to change TTS inflection.
- Home button.
- Navigation back button that only moves back through board history and is disabled when no back path exists.
- Pinned urgent/common words.
- Fixed pinned-word strip outside the 4x5 grid.
- Pinned words should live at the bottom of the screen.
- Visual-first buttons with text labels.
- Fast navigation through common sentence paths.
- Tapping a word speaks it, adds it to the sentence bar, and moves into that word's next board/category where one exists.
- Suggestions may appear in a side panel while the main board navigates through the active word/category path.

## Admin/Edit Mode

- Protected by an admin passcode.
- Default admin passcode is `1234`.
- First-time setup must force the admin passcode to be changed away from `1234`.
- Admin mode requires passcode entry every time.
- Edit vocabulary.
- Add new words.
- Add custom images from gallery.
- Take a photo for a word.
- Edit button color.
- Edit button position.
- Edit category or next-word relationships.
- Drag and drop button arrangement.
- Dedicated layout editor rather than drag-and-drop directly on the live child board.
- Manage vocabulary packs.
- View local usage summaries.
- Reset layout to default if needed.
- Restore all defaults if needed.

Admin mode should be hidden behind a settings/admin screen rather than being visible in child mode.

Modeling mode should be a switch inside admin mode.

## Vocabulary Packs

Vocabulary packs should be configurable from day one.

Vocab packs should be enabled or disabled globally. When a pack is enabled, its words populate the relevant existing categories where possible. If a pack introduces a new major category, it may expose a new folder/category on the home board.

Default vocabulary should include core and practical words, including:

- Pronouns and sentence starters such as "I", "you", "want", "need", "go".
- Common rooms and places such as "bathroom", "kitchen", "home", "school", "shops".
- People such as "Mum", "Dad", "friend", "teacher".
- Needs and actions such as "help", "stop", "more", "eat", "drink", "sleep".
- Feelings and body basics.
- Food and drink basics.

The default vocabulary should avoid highly specific fringe words such as Minecraft, Roblox, Bluey, or individual brands. Those should be easy for carers to add.

Words should not have a hidden state in the first version. If a carer does not want a word available, they should delete it or disable the pack that supplied it.

Admins should be able to delete default words and move/edit words from enabled packs. Settings must include restore default layout and restore all defaults from day one.

## Proposed Default Home Board

The first 4x5 home board should prioritize sentence starters and practical communication paths. This is a starting point to test and iterate.

| Row | Button 1 | Button 2 | Button 3 | Button 4 | Button 5 |
| --- | --- | --- | --- | --- | --- |
| 1 | I | you | want | need | go |
| 2 | food | drink | toilet | people | places |
| 3 | home | school | play | feel | body |
| 4 | like | don't | have | things | finished |

Pinned bottom strip:

- yes
- no
- more
- help
- stop

Category-style buttons should still speak and add their word to the sentence bar. They should also show a small folder marker so carers can see that they open a board/path.

## Icon Strategy

openAAC should support a mix of:

- Original openAAC icons.
- Creative Commons compatible icons.
- Generated icons where licensing allows.
- User-provided photos and images.

The target visual style should be consistent, but the product should not block useful communication just because an icon differs in style.

Words should support multiple icon options so carers can choose the image that best matches the child.

Custom images should be cropped to a square, with the carer able to position and crop the image during import.

Default icons should be simple, cartoon-like, and symbolic. Facial expression icons should be avoided except for very clear happy and sad icons.

## Voice And Speech

The most important speech requirement is that it works locally.

First version goals:

- Use local text-to-speech.
- Speak each word when tapped.
- Speak the full sentence on command.
- Global voice settings for voice, pitch, speed, and volume.
- Prefer Australian English voice where available.
- English only.

Custom recorded audio is not part of the first version.

## Local Usage Insights

openAAC may collect local-only usage data to improve the user experience and help carers understand progress.

Examples:

- Most used words this week, month, and year.
- Number of unique words used.
- Common word sequences.
- Full spoken sentence history where needed to support the future local intelligence layer.
- Recently used phrases.
- Frequent paths after a selected word.

This data must stay on the device. It should never be uploaded, sold, synced, or used for external analytics.

Exact spoken sentence history should expire after about 30 days. Aggregate statistics and sequence summaries can be kept longer.

Usage tracking should be enabled by default. Admin mode should include a modeling mode so carers can use the app to demonstrate communication without mixing their own modeled language into the child's usage analytics.

The first recommendation system should be local and explainable. It should begin with rule-based common paths, then learn from repeated word taps and next-word transitions. Onboarding should explain that recommendations are useful immediately as starter paths, usually start personalizing after a few repeated phrases, and tend to become noticeably better after a week or two of regular use.

## MVP Scope

The first meaningful MVP should include:

- Android tablet app.
- Local-only storage.
- First-time setup flow.
- Child communication mode.
- Admin/edit mode with passcode.
- Default admin passcode of `1234`.
- Forced admin passcode change during setup.
- 4x5 board layout.
- Proposed default 20-button home board.
- Separate fixed strip for pinned words.
- Sentence bar with icons and text.
- Tap-to-speak word behavior.
- Speak sentence button.
- Backspace last word and long-press backspace to clear sentence.
- Question mark sentence action.
- Question action displays `?` and speaks `hmm`.
- Pinned words: yes, no, more, help, stop.
- Basic category and sentence-flow navigation.
- Category-style buttons that speak, add to the sentence, and navigate into their next board.
- Configurable vocabulary packs.
- Add/edit custom words.
- Add images from gallery.
- Take photo for word image.
- Carer-controlled square crop/position for custom images.
- Global local TTS settings.
- Local usage counters and sentence/path history.
- Exact sentence retention of about 30 days, with longer-lived aggregate stats.
- Admin usage insights.
- Admin modeling mode.
- Landscape-only tablet layout designed around a 12-inch tablet first.
- No Android internet permission.
- Restore default layout and restore all defaults.

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
- Portrait-first layout.
- Hidden words.
- Phrase buttons.
- Backup/export.
- Full icon coverage before the first usable prototype.

## Success Criteria

The MVP is successful if:

- A child can build and speak useful messages without reading fluently.
- A carer can add a new word and image without technical help.
- A carer can rearrange a board without risking the child-facing mode.
- Speech, backspace, tracking, and editing work reliably.
- The app works offline.
- The app stores all user data locally.
- Frequent communication paths are faster than manual category browsing.
- The interface remains stable and predictable during normal use.

## Licensing Direction

The project goal is public source code that families can inspect, use, and customize for free, while preventing commercial resale or paid product reuse without written permission from the project owner.

That goal conflicts with the standard OSI definition of open source, because OSI-approved open-source licenses allow commercial use. The project should describe itself as public-source or source-available rather than OSI open source.

Chosen direction:

- Use PolyForm Noncommercial License 1.0.0.
- Allow free personal, family, school, therapy, and non-commercial customization.
- Require explicit written approval for selling openAAC, bundling it into a paid product, or using the code in a commercial AAC product.
- Keep all built-in icons and third-party assets under compatible licenses with clear attribution.
