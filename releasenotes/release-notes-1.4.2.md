## Specman 1.4.2
Released: 2026-11-25

Bugfix and polish release on top of 1.4.1.

### New
- **Command line:** `specman-cli --version` prints the version. An unknown option now ends with an error message instead of starting the GUI.
- **Sanitizer:** `--sanitize` now also checks change tracking: invalid markups, and inconsistent change sets in the coupled source and target steps of a moved step.
- **BACKSPACE in an empty text:** An empty text below a table (or any other area) that is only there as a place to type is removed by BACKSPACE, and the caret moves to the very end of the area above it, for a table to its last cell. The removal can be undone. The first text of a step stays.

### Fixed
- **Text alignment buttons:** Text without a `<div>`, which is every new text, could not be aligned. Several alignment buttons were active at the same time, and a click on the active button switched it off visually.
- **PDF export:** Decorations such as underlining and strike-through changed the line color and width for the shapes drawn afterwards, for example rounded borders.
- **Command line:** Error messages are written as UTF-8.

### Improved
- The buttons for text alignment and text style in the toolbar have the same toggle look as the auto-load button.
- The introductory example in the manual and the readme has been redesigned.
