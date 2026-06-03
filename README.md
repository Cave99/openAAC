# openAAC

openAAC is a planned free, public-source AAC app for Android tablets.

The goal is to make a practical communication app for non-speaking autistic children and their carers without accounts, cloud storage, subscriptions, paid icon packs, or external analytics.

## Current Status

This repository is in product planning.

Planning docs:

- [Product plan](product.md)
- [Style guide](style.md)
- [Technical plan](tech.md)
- [Future ideas](future.md)

## Core Principles

- Free forever for families, carers, schools, and non-commercial use.
- Local-only by design.
- No sign in or sign up.
- No cloud database.
- No internet permission in the app.
- Android tablet first.
- Built for landscape tablet use.
- Custom vocabulary and images from day one.
- Admin editing protected from accidental child access.
- Local usage insights only, never external analytics.

## MVP Direction

The first version is planned as a native Android app using Kotlin, Jetpack Compose, Room, local files, and Android local TextToSpeech.

The MVP should include:

- First-time carer setup flow.
- Child communication mode.
- Admin mode with default passcode `1234`.
- 4x5 communication grid.
- Fixed pinned-word strip.
- Top sentence bar with icons and text.
- Speak sentence, backspace, long-press clear, and question marker controls.
- Configurable vocabulary packs.
- Custom words and images.
- Local usage tracking and admin insights.

## License Status

The license is not finalized yet.

The intended direction is source-available/non-commercial: people should be able to inspect, use, and customize openAAC for free, but selling it, bundling it into a paid product, or using the code in a commercial AAC product should require explicit written permission from the project owner.

That goal is different from standard OSI open source, because OSI-approved open-source licenses allow commercial use. Until the final license is chosen, do not assume commercial reuse is permitted.

