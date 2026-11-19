# Property Handover

Property Handover is a small Android demo for a key handover. Record damage to a room or a utility meter reading, review a short summary, and share it as plain text with Android's Sharesheet. The app runs offline. Its Spanish UI uses bundled example forms and keeps drafts in memory during the session.

## Run it

Open the project in Android Studio, select JDK 25 for Gradle, and install Android SDK Platform 37.0 and Build Tools 36.0.0 through the SDK Manager. Run the `app` configuration on an emulator or a device with Android 8.0 (API 26) or later.

From the repository root, with the SDK and JDK configured:

```sh
./gradlew :app:assembleDebug :domain:test :data:test :app:lintDebug
```

The installable debug APK is `app/build/outputs/apk/debug/app-debug.apk`. To run the Compose UI tests, connect a single test device and run `./gradlew :app:connectedDebugAndroidTest`. GitHub Actions runs the same debug build, unit tests, and lint on pull requests to `develop` and `main`.

## How A2UI defines the forms

The app bundles two [A2UI 1.0](https://github.com/a2ui-project/a2ui/blob/main/specification/v1_0/docs/a2ui_protocol.md) messages, [damage.json](data/src/main/resources/a2ui/damage.json) and [meter.json](data/src/main/resources/a2ui/meter.json). Each message uses one `createSurface` with inline `components` and `dataModel`. The catalog identifier selects this renderer's subset of the [Basic Catalog](https://github.com/a2ui-project/a2ui/blob/main/catalogs/basic/v1/catalog.json) locally.

The app starts with neither form selected. Choosing an entry type loads a different component tree into the same Compose renderer. Draft values are kept when switching forms. Tap **Ver mensaje A2UI** below either form to inspect the bundled JSON used by the renderer. This viewer shows the original message, including its empty initial values.

Edit a label or reorder the IDs in the `details` column, then rebuild to see the change. The required field paths are the contract with the corresponding domain rules.

| A2UI component | Supported here |
| --- | --- |
| `Column` | Static list of child IDs in display order |
| `Card` | A single child component |
| `Text` | Literal text, with a single-line `## ` prefix rendered as a heading |
| `TextField` | Literal label, optional literal placeholder, `shortText`, `longText`, or `number`, and a binding to a string |
| `ChoicePicker` | `mutuallyExclusive` chips with static options and a binding to a string array |
| `Button` | Primary button with a `Text` child and the local `showSummary` event |

Bindings use `@path` with a single top-level key. The demo loads complete local surfaces. Its parser rejects malformed messages and unsupported components before rendering, and the user can still switch to the other form. Business validation runs when the summary is requested. Damage descriptions have a 240-character limit, with a counter starting at 200 and focus moving to the first invalid damage field on submission.

- `app` contains the Compose renderer, the StateFlow ViewModel, Hilt wiring, and Android sharing.
- `data` loads and parses the JSON resources on the JVM.
- `domain` contains the Kotlin validation rules and summary formatting.

## Try the demo

Choose **Daño en la vivienda** and show **Estancia** and **Descripción del daño**. Switch to **Lectura de contador**, select **Agua**, and enter the fictional reading **128,4**. Tap **Ver resumen** and **Compartir resumen** to open the Android Sharesheet.

## Release build and license

Run `./gradlew :app:assembleRelease` to generate `app/build/outputs/apk/release/app-release-unsigned.apk`. Sign the distribution APK with the release keystore before publishing it. See [the release notes](CHANGELOG.md) and the [Apache 2.0 license](LICENSE). The bundled Instrument Serif font is distributed under the [SIL Open Font License](app/src/main/assets/licenses/instrument-serif.txt).
