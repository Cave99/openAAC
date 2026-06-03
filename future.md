# openAAC Future Ideas

This document tracks future features, research topics, and ideas to revisit. Items here are not commitments. They are possibilities that may improve openAAC once the MVP is working and real users can give feedback.

## Guiding Rule

Future features must not compromise the core ideology:

- Free forever.
- Open source.
- Local only.
- No sign in.
- No cloud database.
- No paid features.
- No paid icon packs.
- No external analytics.

## Near-Term Improvements

## Better Quick Access

Add a quick access panel that suggests likely next words or phrases.

Possible sources:

- Rule-based paths.
- Recently used words.
- Frequently used words.
- Common word pairs.
- Time-of-day patterns stored locally.

Example:

If the user often says "I want apple", then after tapping "I" and "want", "apple" should appear quickly without navigating through food categories.

## Frequency-Based Suggestions

Use local usage history to make suggestions more useful.

Potential features:

- Top next word after current word.
- Top category after current path.
- Frequently used phrases.
- Weekly/monthly/yearly usage trends.
- Recently used words.

This should be local, explainable, and easy to turn off.

## Admin Usage Dashboard

Give carers a local view of how the app is being used.

Ideas:

- Most used words this week.
- Most used words this month.
- Most used words this year.
- Unique words used.
- Number of spoken sentences.
- New words used recently.
- Common communication paths.

This is not external analytics. It is a local support tool for carers.

## Phrase Shortcuts

Allow carers to create reusable phrases.

Examples:

- "I want a drink"
- "I need help"
- "I want to go home"
- "I feel sick"

Questions to resolve:

- Should phrase buttons speak immediately?
- Should phrase buttons add multiple words to the sentence bar?
- Should phrases be shown differently from single words?

## Layout Options

The first version uses a 4x5 grid. Future versions may support:

- 3x4.
- 4x5.
- 5x6.
- 6x8.
- Custom board density.

This should be added only when the layout system is stable.

## Multiple Profiles

Support multiple users on one tablet.

Potential use cases:

- Siblings.
- Classrooms.
- Therapy settings.

This is not needed for the first version.

## Local Backup And Restore

Support exporting and importing local configuration files.

Possible formats:

- A local backup file.
- A vocabulary pack file.
- A board configuration file.

The backup should include:

- Vocabulary.
- Layouts.
- Local image references or bundled image files.
- Settings.

Sensitive usage history should be optional during export.

## QR Config Transfer

Investigate copying configuration from one tablet to another using QR codes.

Possible approaches:

- QR code containing a small vocabulary pack.
- QR code containing a local transfer token for direct device-to-device transfer.
- Multiple QR codes for larger exports.

Important constraints:

- No cloud service.
- No account.
- No remote storage.
- Clear confirmation before importing.

## Icon System Expansion

Build a large open icon library for practical AAC use.

Ideas:

- Core openAAC icon pack.
- Category-specific icon packs.
- Community-shared local packs.
- Multiple icon options per word.
- License and attribution metadata.
- Style cleanup tools for inconsistent images.

Potential categories:

- Food and drink.
- Places.
- People.
- Feelings.
- Body and health.
- School.
- Home.
- Activities.
- Actions.
- Descriptions.
- Time.
- Safety and urgent needs.

## Generated Icons

Investigate using generated icons to fill vocabulary gaps.

Requirements:

- Clear licensing.
- No required network use inside the app.
- Generated assets bundled or manually imported.
- Consistent style guide.
- Human review before inclusion.

The app itself should not depend on cloud image generation.

## Offline TTS Improvements

Investigate higher-quality fully local speech options.

Questions:

- Is Android built-in offline TTS good enough?
- Which local voices are available by default on common tablets?
- Can the app detect whether a voice is local or network-backed?
- Are there open-source offline TTS engines that can be bundled?
- What is the storage and performance cost?

The voice system should remain local-first.

## Pronunciation Overrides

Allow carers to control how words are spoken.

Potential options:

- Global pronunciation dictionary.
- Per-word speech text.
- Phonetic override.

This can help with names, places, and unusual words.

## More Languages

English is the first version. Future language support may include:

- Australian English defaults.
- Other English dialects.
- Multilingual vocabulary packs.
- Language-specific TTS settings.

Questions:

- Should language be per app, per board, or per vocabulary pack?
- Should icons be shared across languages?
- How should plural, tense, and grammar differences be handled?

## Better Editing Tools

Improve admin workflows.

Ideas:

- Bulk edit colors.
- Duplicate a board.
- Duplicate a word.
- Move multiple buttons.
- Search vocabulary.
- Preview child mode before saving.
- Undo edit changes.
- Restore deleted word.
- Lock individual buttons.

## Templates And Starter Boards

Add starter templates for common environments.

Examples:

- Home.
- School.
- Food and drink.
- Feelings.
- Body and health.
- Places.
- People.
- Activities.

Templates should remain editable.

## Communication Modeling Mode

Support carers using the app to model language.

Ideas:

- Carer can build a sentence while child watches.
- Optional setting to keep suggested next words visible.
- Ability to highlight a path without changing the child's current board.

This should not make child mode more complex.

## Safety And Recovery

Improve resilience.

Ideas:

- Layout reset.
- Restore default board.
- Recover deleted vocabulary.
- Export backup reminder.
- Validate broken image links.
- Repair missing icon assets.

## Device And Install Strategy

Future release paths:

- Google Play.
- F-Droid.
- Signed APK releases.
- Setup guide for low-cost Android tablets.

Potential documentation:

- Recommended tablet specs.
- Offline setup guide.
- Carer quick-start guide.
- Admin editing guide.

## iOS Port

iOS should be considered after Android feedback.

Possible approaches:

- Native SwiftUI port.
- Kotlin Multiplatform shared logic with native iOS UI.
- Flutter rewrite if cross-platform becomes more important.

Avoid early decisions that make an iOS port unnecessarily hard, but do not slow Android MVP for iOS before the product has been tested.

## Ideas To Avoid

These should remain out of scope unless the core ideology changes, which it should not:

- Cloud accounts.
- Hosted user profiles.
- Remote database sync.
- Paid tiers.
- Subscriptions.
- Paid icon packs.
- Advertising.
- External analytics.
- Required internet connection.
- Cloud AI recommendations.
- Social features.

## Open Questions

- Is Android built-in local TTS good enough for the first release?
- What default vocabulary size is useful without overwhelming the child?
- Should pinned words live in the grid or in a separate strip?
- How should the quick access panel fit beside a 4x5 tablet board?
- Should tapping a category word also speak it, or only navigate?
- Should phrase shortcuts be treated as words or sentence templates?
- What backup format is easiest for non-technical carers?
- What icon style is clearest for the target users?

