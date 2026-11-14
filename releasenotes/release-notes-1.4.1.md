## Specman 1.4.1
Released: 2026-11-14

Bugfix and polish release on top of 1.4.0.

### New
- **Graphviz for PlantUML diagrams**: If a Graphviz installation is available (environment variable `GRAPHVIZ_DOT` or `dot` on the `PATH`), Specman uses it as the PlantUML layout engine instead of the embedded ELK engine, which gives better results for diagrams that rely on layout hints. Without Graphviz, nothing changes. See the manual chapter on PlantUML diagrams for details, in particular if diagrams must render identically on all machines of a team.
- Tables: A click into the bottom margin of a table creates a paragraph behind the table, or moves the caret into the text that follows it.

### Improved
- The mouse cursors for table operations (add/remove row, column and table) have a new design.
- The fold buttons of the steps use the same chevron icons as the "Expand all" and "Collapse all" buttons of the ribbon, and their height is calculated more precisely.

### Fixed
- A diagram with a catch area inside an if branch was not reloaded correctly.
