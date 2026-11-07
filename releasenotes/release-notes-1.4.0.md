## Specman 1.4

### New
- **PlantUML diagrams**: Embed PlantUML diagrams (class diagrams, sequence diagrams, etc.) directly in step texts. A new toolbar button creates a diagram, either from an existing `.puml` file or from scratch with a built-in source editor (double-click). Diagrams are rendered as vector graphics, are included in the PDF export, and take part in change tracking. The diagram source is plain text in the model file, so AI agents can read and edit it. Rendering needs no extra installation.
- **Specman URLs**: Copy a link to a step ("Copy Link" in the step number's context menu) and paste it into any other tool. Ctrl+L in Specman navigates to the link in the clipboard. Links also work as hyperlinks in step texts (Ctrl+click). Aliases make links independent of local file paths, which is useful for teams (File > Alias). A new CLI operation `--resolve-link` checks links for agents and pipelines.
- **Meta tags**: Attach configurable tags (icon, label, colors, optional free text) to steps. Tags are configured in the UI, survive copy/paste and drag & drop, are searchable, show up in the PDF export, and are checked by the sanitizer.
- **SVG images**: SVG files can be inserted as images and stay sharp at any zoom factor and in the PDF.
- **Internationalization**: The UI language can be selected, supporting German and English.

### Improved
- Redesigned toolbars.
- New design of the step number labels, which no longer overlap the step text.
- Copy, cut and paste of tables.
- Up/down arrow keys move the caret across step boundaries.

### Delivery
- The manual is now included as `specman-manual.pdf` in the installation root of both the Windows installer and the portable ZIP.
- The grammar (`SpecmanModel_V002.g4`) and the matching reference model (`specman-reference-v1.4-model-v2.nsd`) are delivered in the same place.

### Upgrade notes
- The model format is still V002, and 1.4 only adds new elements when you use new features (meta tags, PlantUML, SVG). Files without any of these new elements still open in 1.3. Opening a 1.3 file in 1.4 shows a warning: once you edit it in 1.4, it should no longer be edited in 1.3, so no meta information is lost.
- The Windows installer replaces an existing Setup EXE installation in place. An installation made with the former MSI is not replaced: uninstall it first via "Apps & Features".
