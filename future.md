# openAAC Future Ideas

This document tracks future features, fixes, and ideas to revisit. Items here are not commitments. Anything already built should be removed from this list so the roadmap stays useful.

## Guiding Rule

Future features must not compromise the core ideology:

- Free forever.
- Public source under the PolyForm Noncommercial License 1.0.0.
- Local only.
- No sign in.
- No cloud database.
- No paid features.
- No paid icon packs.
- No external analytics.
- No internet permission unless the project owner makes an explicit major direction change.

## Later Improvements

### Phrase Shortcuts

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

Phrase buttons should not make ordinary single-word navigation harder to understand.

### Layout Options

Future versions may support different board densities.

Possible layouts:

- 3x4.
- 4x5.
- 5x6.
- 6x8.
- Custom board density.

This should be added only when the layout system is stable.

### Switch And Scanning Access

Investigate support for users who cannot reliably tap buttons.

Switch/scanning access means the app can move focus through buttons automatically or step-by-step, and the user selects an item with one or more external switches or accessible controls instead of directly tapping the tablet grid.

Potential users:

- Children with motor differences.
- Users who cannot accurately touch a tablet grid.
- Users who use external accessibility switches.

Questions:

- Should scanning move row-by-row, button-by-button, or by groups?
- Should the app support Android accessibility services first?
- How should scanning work with the sentence bar and pinned strip?
- What visual focus indicator is clear enough for AAC use?

### Local Backup And Restore

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

### QR Config Transfer

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

### Icon System Expansion

Build a larger open icon library for practical AAC use.

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

### Generated Icons

Investigate using generated icons to fill vocabulary gaps.

Requirements:

- Clear licensing.
- No required network use inside the app.
- Generated assets bundled or manually imported.
- Consistent style guide.
- Human review before inclusion.

The app itself should not depend on cloud image generation.

### Offline TTS Improvements

Investigate higher-quality fully local speech options.

Questions:

- Is Android built-in offline TTS good enough?
- Which local voices are available by default on common tablets?
- Are there open-source offline TTS engines that can be bundled?
- What is the storage and performance cost?

The voice system should remain local-first.

### Pronunciation Overrides

Allow carers to control how words are spoken.

Potential options:

- Global pronunciation dictionary.
- Per-word speech text.
- Phonetic override.

This can help with names, places, and unusual words.

### More Languages

English is the first version. Future language support may include:

- Australian English defaults.
- Other English dialects.
- Multilingual vocabulary packs.
- Language-specific TTS settings.

Questions:

- Should language be per app, per board, or per vocabulary pack?
- Should icons be shared across languages?
- How should plural, tense, and grammar differences be handled?

### Better Editing Tools

Improve admin workflows.

Ideas:

- Bulk edit colors.
- Duplicate a board.
- Duplicate a word.
- Move multiple buttons.
- Preview child mode before saving.
- Undo edit changes.
- Restore deleted word.
- Lock individual buttons.

### Communication Modeling Mode

Support carers using the app to model language.

Ideas:

- Carer can build a sentence while child watches.
- Optional setting to keep suggested next words visible.
- Ability to highlight a path without changing the child's current board.

This should not make child mode more complex.

### Safety And Recovery

Improve resilience beyond the existing default restore controls.

Ideas:

- Recover deleted vocabulary.
- Export backup reminder.
- Validate broken image links.
- Repair missing icon assets.

### Device And Install Strategy

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

### iOS Port

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
- Android internet permission.
- Cloud AI recommendations.
- Social features.

## Open Questions

- What default vocabulary size is useful without overwhelming the child?
- What backup format is easiest for non-technical carers?
- What icon style is clearest for the target users?
- How should multiple local profiles be switched without making child mode confusing?
