# openAAC Technical Plan

## Technical Priorities

openAAC should optimize for:

- Fast development.
- Easy iteration from real user feedback.
- Android tablet quality.
- Local-only data.
- Future iOS portability where practical.
- Simple contribution and maintenance.
- Avoiding heavy architecture that slows early learning.

The first version should favor pragmatic choices over theoretical perfection.

## Platform Direction

openAAC should be Android first and tablet first.

The app should be designed for:

- Android tablets.
- Locked landscape orientation.
- A 12-inch tablet as the first design target.
- A layout that still works decently on 10-inch tablets.
- APK side-loading during early development.
- Eventual Google Play release.

iOS support is a future goal. The first version should avoid choices that make iOS impossible, but it does not need to ship iOS support immediately.

## Recommended Stack

Recommended first stack:

- Kotlin.
- Jetpack Compose.
- Room SQLite database.
- Android local TextToSpeech APIs.
- Kotlin coroutines and Flow.
- Gradle.

This gives the fastest path to a reliable Android tablet app with strong local storage, native performance, and good UI iteration.

## Why Native Android First

Native Android is recommended because:

- The first target is Android tablets.
- Compose is well suited to grid-based UI.
- Android TTS APIs are available locally.
- Room gives a mature local database.
- APK side-loading is straightforward.
- Fewer cross-platform framework decisions are needed early.
- The app can be tested directly on the target platform.

The tradeoff is that iOS will require either a later port or shared Kotlin business logic later.

## Alternatives Considered

## Kotlin Multiplatform

Kotlin Multiplatform could share data models, vocabulary logic, and recommendation logic between Android and iOS.

Pros:

- Better long-term portability.
- Shared business logic.
- Native UI can still be used per platform.

Cons:

- More complexity at the start.
- Slower early iteration.
- More project setup and maintenance.

Recommendation:

Use native Android first, but keep business logic clean enough that it can be extracted into shared Kotlin later if iOS becomes active.

## Flutter

Flutter could ship Android and iOS from a single codebase.

Pros:

- Good cross-platform UI.
- Fast visual iteration.
- One app codebase for Android and iOS.

Cons:

- Local TTS and Android tablet behavior may require platform integration.
- Native accessibility and platform conventions need careful handling.
- The first release is Android-only, so cross-platform benefit may not pay off immediately.

Recommendation:

Reasonable alternative, but not the preferred first choice unless cross-platform speed becomes more important than native Android fit.

## React Native

React Native could also target Android and iOS.

Pros:

- JavaScript ecosystem.
- Cross-platform potential.

Cons:

- Native module complexity for local TTS, storage, and device-specific behavior.
- More moving parts.
- Less ideal for a polished tablet-first AAC grid than native Compose.

Recommendation:

Not preferred for the first version.

## Architecture

Use a simple layered architecture:

- UI layer: Jetpack Compose screens and components.
- ViewModel layer: state, events, and screen logic.
- Domain layer: vocabulary selection, board navigation, sentence building, recommendation rules.
- Data layer: Room database, local image storage, settings storage.
- Platform layer: Android TextToSpeech, image picker, camera capture, image cropper.

Avoid over-abstracting early. Create boundaries where they protect important decisions:

- Local storage should be clearly separated.
- TTS should be behind a small interface.
- Recommendation logic should be testable outside UI.
- Vocabulary models should not depend on Compose.
- Sentence-building logic should be separate from board-navigation logic.

## Local Data

All user data should stay on the device.

Storage should include:

- Room database for structured app data.
- Local app files directory for custom images.
- DataStore for small settings such as voice, pitch, speed, volume, and admin mode preferences.

No remote database should be used.

The app should not request Android internet permission. This should be validated in code review before every release.

The first-time setup flow should not ask for the child's name or any other personal identity details.

## Suggested Data Model

Initial entities:

- `VocabularyItem`
- `VocabularyPack`
- `Board`
- `BoardCell`
- `IconAsset`
- `NavigationRule`
- `UsageEvent`
- `SentenceSession`
- `AppSettings`

## VocabularyItem

Represents a speakable word or phrase.

Fields may include:

- id
- label
- speechText
- iconAssetId
- color
- packId
- isPinned
- opensBoardId
- isCategory
- isDefault
- createdAt
- updatedAt

Every first-version button should represent a single word. Category-style buttons can speak and add that word to the sentence, then navigate to the next board.

Category-style buttons should have a small folder marker in the UI.

## VocabularyPack

Groups vocabulary into configurable sets.

Fields may include:

- id
- name
- description
- enabled
- createdAt
- updatedAt

Vocab packs are enabled or disabled globally. Enabled packs populate their words into matching categories and can introduce a new home-board category/folder when needed.

## Board

Represents a visible grid or path.

Fields may include:

- id
- name
- parentBoardId
- type
- sourceVocabularyItemId
- createdAt
- updatedAt

## BoardCell

Represents a button position on a board.

Fields may include:

- id
- boardId
- vocabularyItemId
- row
- column
- colorOverride

Button colors should be resolved from category defaults unless `colorOverride` is set.

## IconAsset

Represents an icon or user image.

Fields may include:

- id
- sourceType
- localPath
- attribution
- license
- cropX
- cropY
- cropScale
- createdAt

Custom images should be copied into app-controlled local storage and cropped/positioned into a square thumbnail for grid display.

Default icons should use simple cartoon-like symbols. Avoid facial expression icons except for clear happy and sad icons.

## NavigationRule

Represents rule-based next-word suggestions or board transitions.

Fields may include:

- id
- fromVocabularyItemId
- toBoardId
- suggestedVocabularyItemId
- priority
- ruleType

## UsageEvent

Represents local-only usage tracking.

Fields may include:

- id
- vocabularyItemId
- sentenceSessionId
- positionInSentence
- sourceMode
- spokenAt

These events can power local summaries and future recommendation behavior.

## SentenceSession

Represents a locally stored spoken sentence.

Fields may include:

- id
- spokenText
- isQuestion
- sourceMode
- startedAt
- spokenAt
- expiresAt

`sourceMode` should distinguish normal child use from carer modeling mode so modeled language does not distort child usage insights.

Exact sentence records should expire after about 30 days. Aggregated usage statistics and sequence summaries can be retained longer.

## Voice And TTS

The first implementation should use Android's local TextToSpeech APIs if they meet the offline requirement on target devices.

The app should expose global settings for:

- Voice.
- Pitch.
- Speech rate.
- Volume if supported.
- Australian English preference where available.
- Normal Android default speech speed.

Important technical requirement:

- The app should clearly warn if a selected voice requires network access.
- The default voice should be local where available.
- The product should not depend on cloud speech services.

If Android built-in local TTS is not reliable enough, investigate bundled offline TTS engines in the future.

## Images

The app should support:

- Built-in icon assets.
- Creative Commons compatible assets.
- Generated assets with clear licensing.
- User-selected gallery images.
- Camera capture for custom words.

Local image storage should:

- Copy selected images into app-controlled local storage.
- Avoid depending on external gallery paths after import.
- Generate thumbnails where useful.
- Preserve attribution and license metadata for built-in or shared assets.

## Admin Passcode

Admin mode should be protected by a simple local passcode.

The first version can use:

- A numeric passcode.
- Default passcode `1234` on first install.
- Forced passcode change during first-time setup.
- Passcode required every time admin mode is opened.
- Stored locally.
- Hashed before storage.
- Reset behavior documented carefully.

The goal is not high-security authentication. The goal is to prevent accidental child edits.

If the passcode is forgotten, the intended recovery path is reinstalling the app to reset the passcode while preserving local vocabulary and images if the platform allows. This needs Android validation because uninstall behavior can remove app-local data unless backup/preservation is deliberately supported.

## Usage Insights

Usage analytics must be local-only.

Track:

- Word taps.
- Sentence speaks.
- Word order in sentences.
- Full spoken sentence sessions.
- Question marker use.
- Board navigation paths.
- Time of use.
- Source mode: child use or modeling mode.

Expose in admin mode:

- Top words this week.
- Top words this month.
- Top words this year.
- Unique words used.
- Common word pairs or paths.
- Common full sentence patterns.
- Modeling mode toggle.

Do not send usage data off device.

Usage tracking should be enabled by default because it supports the future local intelligence layer. The first-time setup flow should explain this clearly.

## Recommendation Layer

First version should use rule-based suggestions.

Examples:

- "I" suggests "want", "need", "go", "feel".
- "want" suggests request categories.
- "go" suggests places.

Later versions can add frequency-based suggestions using local `UsageEvent` data.

The recommendation system should remain:

- Local.
- Explainable.
- Optional.
- Non-blocking.

The first implementation should support a side suggestion panel while the main board navigates into the active word/category path.

The side suggestion panel should be actionable. Tapping a recommendation should add the word to the sentence, speak it, track the word, track the previous-word transition, and navigate if the recommendation opens a board.

Recommendation data should include:

- Local word counts.
- Local previous-word to next-word transition counts.
- Rule-based fallbacks for cold start.
- Current board fallback suggestions.

Recommendation status labels should be simple:

- Starter suggestions.
- Collecting patterns.
- Starting to personalize.
- Learning from regular use.

## Default Board Seed

The first seed data should create this home board:

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

The pinned words should speak immediately and add to the sentence bar. `stop` can use a stop-sign style icon.

## Permissions

Initial expected Android permissions:

- Camera permission for taking custom word photos.
- Read media/photo picker access for choosing images, depending on Android version.

Avoid:

- Internet permission.
- Network state permission.
- Account permissions.
- Location permissions.

The Android manifest should intentionally omit `android.permission.INTERNET`.

If a future feature requires a new permission, it should be documented and treated as a major product decision.

## Testing Strategy

Testing should focus on the parts most likely to break communication.

Unit tests:

- Sentence building.
- Delete and clear behavior.
- Question marker behavior.
- `?` marker speaks `hmm` and does not depend on TTS inflection.
- Navigation rules.
- Recommendation rules.
- Usage summaries.
- Modeling mode separation.
- Exact sentence expiry after about 30 days.
- Vocabulary pack enable/disable behavior.
- Restore default layout and restore all defaults behavior.

UI tests:

- Child mode hides edit controls.
- Admin mode requires passcode.
- A user can tap words and speak a sentence.
- Back and home navigation work.
- Pinned words remain available.
- Backspace deletes words while navigation back changes only the board path.
- Long-press backspace clears the sentence.
- Home resets the board path without clearing the sentence.
- Question marker is visible in the sentence bar.
- Pinned bottom strip remains available during child mode.
- Category buttons show a folder marker.

Manual device testing:

- Android 12-inch tablet landscape layout.
- Local TTS works offline.
- Camera image import works.
- Gallery image import works.
- Image crop/position flow works.
- Text fits in grid buttons.
- The installed app works without internet permission.

## Release Strategy

Early releases:

- APK side-loading.
- Manual testing on Android tablets.
- Simple release notes.

Later releases:

- Google Play.
- F-Droid investigation.
- Public repository releases.

## Repository And Governance

The project should be public source.

Recommended branches:

- `main` for stable releases.
- `develop` for active integration.

Repository settings should ensure only the owner can directly manage protected branches.

Recommended setup:

- Protect `main`.
- Protect `develop`.
- Require pull requests for protected branches if contributors are added.
- Disable force pushes on protected branches.
- Keep issue tracking simple.
- Avoid heavy contribution process unless the project grows.

GitHub personal repositories cannot restrict protected-branch pushes to a named user in the same way organization repositories can. Practical control is handled by keeping collaborator access limited, protecting branches, and disabling force pushes and branch deletion.

## Licensing

The desired licensing model is not a standard OSI open-source model because it should block commercial resale and paid product reuse without written permission.

Chosen direction:

- Use PolyForm Noncommercial License 1.0.0.
- Keep third-party icon and asset licenses compatible with non-commercial distribution and clearly attributed.

## Development Rules

Technical decisions should preserve the product ideology:

- No sign-in system.
- No cloud backend.
- No remote analytics.
- No subscription architecture.
- No paid feature flags.
- No network-hosted vocabulary.
- No database outside the device.
- No Android internet permission.
- No commercial reuse license without explicit owner approval.

If a proposed feature conflicts with local-only trust, it should be rejected or redesigned.
