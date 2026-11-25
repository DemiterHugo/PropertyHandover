# Changelog

## 0.1.0

- Record property damage or a utility meter reading during a key handover.
- Render both forms from bundled A2UI 1.0 messages with a single Material 3 renderer.
- Validate and save one damage entry and one reading in the same report. Review, replace, or remove the entries and share them together as plain text using Android's Sharesheet.
- Run the demo offline on Android 8.0 or later.
- Choose the entry type explicitly and keep draft values when switching forms.
- Use a house-and-key adaptive launcher icon, a slate-blue brand header, and distinct clay and teal accents for the two forms.
- Enter damage details with examples, inline errors, and a character-limit progress bar. Select a utility from a radio list and enter a decimal reading.
- See the A2UI component behind each field in **Cómo se genera** and inspect the original JSON.
- Fix a release-only startup crash caused by scoped R8 optimization.
