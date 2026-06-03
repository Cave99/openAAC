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
- Landscape orientation as the primary layout.
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
- Platform layer: Android TextToSpeech, image picker, camera capture.

Avoid over-abstracting early. Create boundaries where they protect important decisions:

- Local storage should be clearly separated.
- TTS should be behind a small interface.
- Recommendation logic should be testable outside UI.
- Vocabulary models should not depend on Compose.

## Local Data

All user data should stay on the device.

Storage should include:

- Room database for structured app data.
- Local app files directory for custom images.
- DataStore for small settings such as voice, pitch, speed, volume, and admin mode preferences.

No remote database should be used.

## Suggested Data Model

Initial entities:

- `VocabularyItem`
- `VocabularyPack`
- `Board`
- `BoardCell`
- `IconAsset`
- `NavigationRule`
- `UsageEvent`
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
- createdAt
- updatedAt

## VocabularyPack

Groups vocabulary into configurable sets.

Fields may include:

- id
- name
- description
- enabled
- createdAt
- updatedAt

## Board

Represents a visible grid or path.

Fields may include:

- id
- name
- parentBoardId
- type
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

## IconAsset

Represents an icon or user image.

Fields may include:

- id
- sourceType
- localPath
- attribution
- license
- createdAt

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
- spokenAt

These events can power local summaries and future recommendation behavior.

## Voice And TTS

The first implementation should use Android's local TextToSpeech APIs if they meet the offline requirement on target devices.

The app should expose global settings for:

- Voice.
- Pitch.
- Speech rate.
- Volume if supported.

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
- Stored locally.
- Hashed before storage.
- Reset behavior documented carefully.

The goal is not high-security authentication. The goal is to prevent accidental child edits.

## Usage Insights

Usage analytics must be local-only.

Track:

- Word taps.
- Sentence speaks.
- Word order in sentences.
- Time of use.

Expose in admin mode:

- Top words this week.
- Top words this month.
- Top words this year.
- Unique words used.
- Common word pairs or paths.

Do not send usage data off device.

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

## Permissions

Initial expected Android permissions:

- Camera permission for taking custom word photos.
- Read media/photo picker access for choosing images, depending on Android version.

Avoid:

- Internet permission.
- Network state permission.
- Account permissions.
- Location permissions.

If a future feature requires a new permission, it should be documented and treated as a major product decision.

## Testing Strategy

Testing should focus on the parts most likely to break communication.

Unit tests:

- Sentence building.
- Delete and clear behavior.
- Navigation rules.
- Recommendation rules.
- Usage summaries.
- Vocabulary pack enable/disable behavior.

UI tests:

- Child mode hides edit controls.
- Admin mode requires passcode.
- A user can tap words and speak a sentence.
- Back and home navigation work.
- Pinned words remain available.

Manual device testing:

- Android tablet landscape layout.
- Local TTS works offline.
- Camera image import works.
- Gallery image import works.
- Text fits in grid buttons.

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

## Development Rules

Technical decisions should preserve the product ideology:

- No sign-in system.
- No cloud backend.
- No remote analytics.
- No subscription architecture.
- No paid feature flags.
- No network-hosted vocabulary.
- No database outside the device.

If a proposed feature conflicts with local-only trust, it should be rejected or redesigned.

