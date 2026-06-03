# openAAC Style Guide

## Design Direction

openAAC should look minimal, clear, and practical. It should be easy to glance at, easy to scan, and hard to misread. The interface is for communication first, not decoration.

The app should feel:

- Calm.
- Direct.
- Colorful but not chaotic.
- Tablet-native.
- Friendly without becoming childish in a distracting way.
- Stable and predictable.
- Fast to understand without reading fluency.

The visual reference is a simple AAC board with a sentence bar at the top, icon-and-text buttons below, and clear editing/navigation controls.

## Product Layout

## Main Communication Screen

The first version should use:

- A top sentence bar that fills the upper area of the screen.
- A primary communication grid.
- A fixed pinned-word strip at the bottom of the screen, separate from the main grid.
- Home and back controls.
- Backspace, speak, and question controls inside the sentence bar.
- A side suggestions panel where useful.

Initial grid:

- 4 rows.
- 5 columns.
- Large tap targets.
- Designed for Android tablets, not phones.
- Designed around a 12-inch Android tablet first.
- Locked landscape orientation.

The sentence bar should show selected words as both icons and text. It should have enough height for children and carers to understand the full message at a glance.

The sentence bar should include:

- Speak full sentence.
- Backspace one word.
- Long-press backspace to clear the sentence.
- Question mark action that displays `?` and speaks `hmm`.

## Button Anatomy

Each communication button should include:

- Icon or image.
- Text label.
- Background color.
- Optional visual cue if it opens a nested path.

Text should always be visible. Even when the child does not read, text helps carers understand the selected words and supports language modeling.

Buttons should be large, stable, and evenly spaced. They should not resize based on label length. Long labels should wrap cleanly or use a smaller text size within a defined limit.

Category-style buttons should look like communication buttons first and folders second. The user is still saying the word when tapping it; the folder/path behavior is an extra result of that tap.

Category-style buttons should include a small folder marker.

## Tap Targets

All child-facing controls should use large touch targets suitable for tablet use.

Guidelines:

- Primary communication buttons should be large enough for imprecise taps.
- Small icon-only controls should be avoided in child mode unless they are repeated, familiar, and visually clear.
- Destructive actions such as clear sentence should be visually distinct.
- Long-press destructive actions should have clear pressed feedback.
- Edit controls should not be visible in child mode unless the admin unlocks edit mode.

## Navigation Style

Navigation should be path-based but shallow.

The user should be able to:

- Tap a word.
- See sensible next options.
- Go back one step.
- Return home.
- Reach common communication goals in under 3 clicks where possible.

Examples:

- "I" can lead to "want", "need", "go", "feel".
- "want" can lead to food, drink, help, activity, object.
- "go" can lead to home, school, shops, bathroom, kitchen.

The interface should avoid deep category mazes.

When a normal word is tapped, the main board should move into that word's next board/category where one exists. Suggestions can appear in a side panel while navigation follows the active path.

The home button should reset only the board path. It should not clear the sentence.

The navigation back button should only move back through board history. It should be disabled or visually muted when there is no previous board. It should not delete words.

## Pinned Words

The following words should always be easy to access:

- yes
- no
- more
- help
- stop

These should have stable placement and strong visual distinction. They should not move around during normal navigation.

Pinned words should not consume cells in the main 4x5 board.

Pinned words should speak immediately and add to the sentence bar when tapped.

## Color System

Color should help recognition, not just decorate.

Recommended direction:

- Use clear, saturated colors for communication categories.
- Keep the app background light and neutral.
- Avoid a one-note palette dominated by one color family.
- Use color consistently enough that children can learn patterns.
- Preserve contrast between text, icon, and background.

Possible color associations:

- Actions: green or teal.
- People: orange or warm yellow.
- Places: blue or green.
- Food/drink: yellow or peach.
- Feelings: purple or pink.
- Body/health: blue.
- Urgent words: red, coral, or high-contrast accent.
- Navigation/system actions: neutral grey or blue.

These associations can change during design testing. The key requirement is consistency and legibility.

Button colors should be auto-applied from category defaults unless the carer overrides a specific button.

## Typography

Text should be plain, readable, and functional.

Guidelines:

- Use a highly legible sans-serif font.
- Avoid decorative fonts.
- Keep letter spacing normal.
- Use short labels where possible.
- Use title case only where it improves readability.
- Prefer lowercase for common words if that feels more natural in the board.
- Text must fit inside buttons on tablet screens.

Labels should be concise:

- Good: "bathroom", "help", "go", "Mum".
- Avoid: "I would like to go to the bathroom".

Long ideas should be built as word sequences instead of oversized labels.

## Icons And Images

Icons should be:

- Simple.
- Recognizable.
- High contrast.
- Friendly.
- Understandable without reading.
- Consistent where possible.

The icon style should favor clear illustrated symbols rather than detailed decorative art.

The app should allow mixed icon sources because usefulness matters more than perfect consistency. However, default vocabulary should aim for a coherent visual style.

Image rules:

- Prefer simple cartoon-like symbols.
- Avoid dark, low-contrast images for default icons.
- Avoid overly detailed images where the meaning is unclear at small sizes.
- Avoid facial expression icons except for very clear happy and sad icons.
- Prefer centered subjects.
- Prefer transparent or plain backgrounds when possible.
- User photos are acceptable for personalized words.
- Custom image import should include a square crop/positioning step so carers can make the icon readable in the grid.

## Modes

## Child Mode

Child mode is the default communication mode.

It should:

- Hide editing controls.
- Keep layout stable.
- Prevent accidental rearrangement.
- Make speaking and navigation obvious.
- Avoid settings, menus, and complex controls.
- Launch directly after first-time setup is complete.

## Admin Mode

Admin mode is for carers.

It should:

- Require an admin passcode.
- Use `1234` as the default first-install passcode.
- Force the default passcode to be changed during first-time setup.
- Require the passcode every time admin mode is opened.
- Show edit controls.
- Allow drag and drop.
- Allow button editing.
- Allow vocabulary pack management.
- Allow local usage review.
- Provide reset and restore actions.
- Provide restore default layout and restore all defaults actions.
- Include a dedicated layout editor instead of editing the live child board directly.
- Include a modeling mode switch so carers can demonstrate language without polluting the child's usage analytics.

Admin mode can be denser than child mode, but it should remain simple and calm.

## Interaction Behavior

When a word is tapped:

- It is added to the sentence bar.
- It speaks immediately.
- The main board moves into that word's next board/category where one exists.
- Suggested next words may appear in a side panel.

When the sentence speak button is tapped:

- The full sentence is spoken.

When delete is tapped:

- The most recent word is removed.

When backspace is held:

- The full sentence is cleared.

When back is tapped:

- The app returns to the previous board/path.

When home is tapped:

- The app returns to the main board.
- The current sentence remains intact.

When question mark is tapped:

- The sentence bar shows a visible `?` marker.
- The app speaks `hmm`.
- The app should not rely on TTS question inflection.

## Accessibility

openAAC should be built as an accessibility-first app.

Guidelines:

- Large tap targets.
- Clear focus states.
- High contrast.
- No reliance on text alone.
- No reliance on color alone.
- Reduced motion by default.
- Fast visual feedback on tap.
- Stable layout.
- Avoid tiny menus in child mode.
- Avoid time-limited interactions.

## Visual Quality Bar

Before a screen is considered finished:

- Text must fit inside buttons.
- Icons must be recognizable.
- Touch targets must be large.
- Buttons must align cleanly.
- Nothing should overlap.
- Child mode should be usable without reading.
- Admin controls should not leak into child mode.
- Layout should work on common Android tablet sizes.
- Layout should be strongest on a 12-inch landscape tablet.
- Sentence bar controls should be reachable and visually distinct.
