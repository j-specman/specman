package specman.editarea;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import org.jetbrains.annotations.NotNull;
import specman.Aenderungsart;
import specman.ChangeInfo;
import specman.ChangeSet;
import specman.EditorI;
import specman.SpaltenContainerI;
import specman.SpaltenResizer;
import specman.TextInit;
import specman.model.v001.ChangeInfo_V001;
import static specman.ChangeSet.changeset;
import static specman.ChangeInfo.fromModel;
import specman.editarea.stepnumberlabel.StepnumberLabel;
import specman.model.v001.EditorContentModel_V001;
import specman.model.v001.TableEditAreaModel_V001;
import specman.model.v001.TextEditAreaModel_V001;
import specman.pdf.Shape;
import specman.undo.UndoableTableColumnAdded;
import specman.undo.UndoableTableColumnRemoved;
import specman.undo.UndoableTableRowAdded;
import specman.undo.UndoableTableRowRemoved;
import specman.undo.props.UDBL;
import specman.view.AbstractSchrittView;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentListener;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static specman.Aenderungsart.Geloescht;
import static specman.Aenderungsart.Hinzugefuegt;
import static specman.ColumnSpecByPercent.allocPercent;
import static specman.ColumnSpecByPercent.copyOf;
import static specman.ColumnSpecByPercent.percents2specs;
import static specman.ColumnSpecByPercent.recomputePercents;
import static specman.ColumnSpecByPercent.releasePercent;
import specman.graphics.Styles;

import static specman.graphics.Styles.DIAGRAMM_LINE_COLOR;
import static specman.view.AbstractSchrittView.FORMLAYOUT_GAP;
import static specman.view.AbstractSchrittView.ZEILENLAYOUT_INHALT_SICHTBAR;
import static specman.Specman.editor;

/** The edit area itself is a surrounding panel which contains the actual table panel
 * with a little space in between to all sides. This space is for interactions to remove
 * and add table columns and rows, and to remove the whole table and other things like
 * that. The table panel follows the same approach as step views do concerning basic layout:
 * the panel itself is black and the table cells are placed as children with a little
 * gap between them. This causes the black panel background to shine through the gaps,
 * creating the impression of lines. */
public class TableEditArea extends JPanel implements EditArea<TableEditAreaModel_V001>, SpaltenContainerI {
  private static final int BORDER_THICKNESS = 7;
  private static final String TABLELINE_GAP = FORMLAYOUT_GAP;
  private static final String TABLELAYOUT_ROWSPEC = ZEILENLAYOUT_INHALT_SICHTBAR;
  private static final int WHOLETABLE_COLUMN_INDICATOR = -1;

  private ChangeInfo changeInfo;
  private final List<FocusListener> editAreasFocusListeners = new ArrayList<>();
  private final List<ComponentListener> editAreasComponentListeners = new ArrayList<>();
  private FormLayout tablePanelLayout;
  private int tableWidthPercent;
  private List<Integer> columnsWidthPercent;
  private TableEditAreaSelectionTracker selectionTracker;
  JPanel tablePanel;
  List<List<EditContainer>> cells = new ArrayList<>();

  public TableEditArea(int columns, int rows, ChangeInfo changeInfo) {
    this.changeInfo = changeInfo;
    this.tableWidthPercent = 100;
    initPanels(columns, rows);
    addInitialCells(columns, rows);
  }

  public TableEditArea(TableEditAreaModel_V001 model) {
    this.changeInfo = fromModel(model.changeInfo, model.aenderungsart);
    this.tableWidthPercent = model.tableWidthPercent;
    this.columnsWidthPercent = model.columnsWidthPercent;
    int rows = model.cells.size();
    int columns = model.cells.get(0).size();
    initPanels(columns, rows);
    addCells(model.cells);
  }

  private Stream<EditContainer> cellstream() { return cells.stream().flatMap(l -> l.stream()); }

  private void initPanels(int columns, int rows) {
    tablePanel = new JPanel();
    tablePanel.setBackground(DIAGRAMM_LINE_COLOR);
    createTablePanelLayout(columns, rows);
    createColumnResizers(columns, rows);
    refreshBorderSpaceGeometricsAndColor();
    selectionTracker = new TableEditAreaSelectionTracker(this);
  }

  private void createColumnResizers(int columns, int rows) {
    EditorI editor = editor();
    int resizerHeight = rows * 2;
    // Resizer for the whole table
    tablePanel.add(new SpaltenResizer(this, WHOLETABLE_COLUMN_INDICATOR), CC.xywh(1 + columns * 2, 1, 1, resizerHeight));

    // Resizers for all columns except the last one
    for (int c = 0; c < columns-1; c++) {
      tablePanel.add(new SpaltenResizer(this, c), CC.xywh(3 + c * 2, 1, 1, resizerHeight));
    }
  }

  private int minimumBorderSize() {
    return (int)((float)BORDER_THICKNESS * (float)editor().getZoomFactor() / 100f);
  }

  private void refreshBorderSpaceGeometricsAndColor() {
    int borderThickness = minimumBorderSize();
    float tableWidthFraction = (float)tableWidthPercent / 100;
    String tableColSpec = "pref:grow(" + tableWidthFraction + ")";
    String rightGapColSpec = borderThickness + "px:grow(" + (1 - tableWidthFraction) + ")";
    String outerColumnAndRowSpec = borderThickness + "px," + tableColSpec + "," + rightGapColSpec;
    FormLayout areaLayout = new FormLayout(outerColumnAndRowSpec, outerColumnAndRowSpec);
    setLayout(areaLayout);
    setBackground(changeInfo.panelColor());
    // Removing and re-associating the tablePanel child is required to ensure propper
    // re-layouting after changing the layout in the lines above
    remove(tablePanel);
    add(tablePanel, CC.xy(2, 2));
  }

  @Override
  public void setEditBackgroundUDBL(Color bg) {
    cellstream().forEach(cell -> cell.setBackgroundUDBL(changeInfo.panelColor()));
    setBackgroundUDBL(bg);
  }

  private void setBackgroundUDBL(Color bg) {
    UDBL.setBackgroundUDBL(this, bg);
  }

  @Override
  public synchronized void addFocusListener(FocusListener l) {
    editAreasFocusListeners.add(l);
    cellstream().forEach(cell -> cell.addEditAreasFocusListener(l));
  }

  @Override
  public synchronized void addComponentListener(ComponentListener l) {
    editAreasComponentListeners.add(l);
    cellstream().forEach(cell -> cell.addEditComponentListener(l));
  }

  private void addCells(List<List<EditorContentModel_V001>> model) {
    for (int r = 0; r < model.size(); r++) {
      List<EditorContentModel_V001> rowModel = model.get(r);
      cells.add(new ArrayList<>());
      for (EditorContentModel_V001 cellModel: rowModel) {
        addCell(r, new EditContainer(cellModel, null));
      }
    }
  }

  private void addInitialCells(int columns, int rows) {
    for (int r = 0; r < rows; r++) {
      cells.add(new ArrayList<>());
    }
    for (int c = 0; c < columns; c++) {
      addCell(0, "<b>Spalte " + (c+1) + "</b>", changeInfo);
    }
    for (int r = 1; r < rows; r++) {
      for (int c = 0; c < columns; c++) {
        addCell(r, "Zelle " + (c+1) + "." + r, changeInfo);
      }
    }
  }

  private void addCell(int rowIndex, EditContainer cell) {
    List<EditContainer> row = cells.get(rowIndex);
    int columnIndex = row.size();
    row.add(cell);
    tablePanel.add(cell, CC.xy(2 + columnIndex * 2, 2 + rowIndex * 2));
  }

  private void addCell(int rowIndex, String content, ChangeInfo changeInfo) {
    TextEditArea initialContentArea = new TextEditArea(new TextEditAreaModel_V001(content, content, new ArrayList<>(), changeInfo), Styles.DEFAULTFONT);
    addCell(rowIndex, new EditContainer(initialContentArea, null));
  }

  private void createTablePanelLayout(int columns, int rows) {
    String columnSpecs = TABLELINE_GAP + "," + percents2specs(columns, columnsWidthPercent) + "," + TABLELINE_GAP;
    String rowSpecs = TABLELINE_GAP;
    for (int r = 0; r < rows; r++) {
      rowSpecs += "," + TABLELAYOUT_ROWSPEC + "," + TABLELINE_GAP;
    }
    tablePanelLayout = new FormLayout(columnSpecs, rowSpecs);
    tablePanel.setLayout(tablePanelLayout);
  }

  @Override
  public void setGeloeschtMarkiertStilUDBL(ChangeSet triggerSet) {
    setChangeInfo(changeInfo.deleted(triggerSet));
    setBackgroundUDBL(triggerSet.panelColor());
    cellstream().forEach(cell -> cell.setGeloeschtMarkiertStilUDBL(null, changeInfo.changeSet()));
  }

  @Override
  public TableEditAreaModel_V001 toModel(boolean formatierterText) {
    List<List<EditorContentModel_V001>> cellsModel = new ArrayList<>();
    for (List<EditContainer> row: cells) {
      List<EditorContentModel_V001> rowModel = row
        .stream()
        .map(ec -> ec.editorContent2Model(formatierterText))
        .collect(Collectors.toList());
      cellsModel.add(rowModel);
    }
    return new TableEditAreaModel_V001(cellsModel, tableWidthPercent, columnsWidthPercent, changeInfo);
  }

  @Override
  public void skalieren(int prozentNeu, int prozentAktuell) {
    cellstream().forEach(cell -> cell.skalieren(prozentNeu, prozentAktuell));
    refreshBorderSpaceGeometricsAndColor();
  }

  @Override
  /** The logic in here has similarities to {@link AbstractSchrittView#aenderungenUebernehmen()}
   * as it is a similar situation of a container having multiple change aspects.
   */
  public int aenderungenUebernehmen() {
    ChangeSet currentSet = changeset();
    int changesMade = changeInfo.numChangesBy(currentSet);
    if (changeInfo.deletedBy(currentSet) || allCellsMarkedAs(Geloescht)) {
      getParent().removeEditAreaUDBL(this);
      changesMade++;
    }
    else {
      changesMade += removeRowsMarkedAs(Geloescht);
      changesMade += removeColumnsMarkedAs(Geloescht);
      changesMade += acceptOrRejectCellChanges(cell -> cell.aenderungenUebernehmen());
    }
    if (changeInfo.changedBy(currentSet)) {
      aenderungsmarkierungenEntfernen();
    }
    changeInfo = changeInfo.untrack(currentSet);
    return changesMade;
  }

  @Override
  public int aenderungenVerwerfen() {
    ChangeSet currentSet = changeset();
    int changesRejected = changeInfo.numChangesBy(currentSet);
    if (changeInfo.addedBy(currentSet) || allCellsMarkedAs(Hinzugefuegt)) {
      getParent().removeEditAreaUDBL(this);
      changesRejected++;
    }
    else {
      changesRejected += removeRowsMarkedAs(Hinzugefuegt);
      changesRejected += removeColumnsMarkedAs(Hinzugefuegt);
      changesRejected += acceptOrRejectCellChanges(cell -> cell.aenderungenVerwerfen());
    }
    if (changeInfo.changedBy(currentSet)) {
      aenderungsmarkierungenEntfernen();
    }
    changeInfo = changeInfo.untrack(currentSet);
    return changesRejected;
  }

  private int acceptOrRejectCellChanges(java.util.function.ToIntFunction<EditContainer> operation) {
    int changesMade = 0;
    for (EditContainer cell : modifyableCells()) {
      int cellChanges = operation.applyAsInt(cell);
      if (cellChanges > 0) {
        cell.setBackground(changeInfo.panelColor());
        changesMade += cellChanges;
      }
    }
    return changesMade;
  }

  private List<EditContainer> modifyableCells() {
    return cellstream().collect(Collectors.toList());
  }

  private int removeColumnsMarkedAs(Aenderungsart aenderungsart) {
    int changesMade = 0;
    for (int c = 0; c < numColumns(); c++) {
      if (columnIsMarkedAs(c, aenderungsart)) {
        removeColumn(c);
        changesMade++;
        c--;
      }
    }
    return changesMade;
  }

  private int removeRowsMarkedAs(Aenderungsart aenderungsart) {
    int changesMade = 0;
    for (int r = 0; r < numRows(); r++) {
      if (rowIsMarkedAs(r, aenderungsart)) {
        removeRow(r);
        changesMade++;
        r--;
      }
    }
    return changesMade;
  }

  @Override
  public void aenderungsmarkierungenEntfernen() {
    cellstream().forEach(cell -> cell.aenderungsmarkierungenEntfernen(null));
    setBackgroundUDBL(Styles.BACKGROUND_COLOR_STANDARD);
  }

  @Override
  public void mergeChangeSetUDBL(@NotNull ChangeSet target, @NotNull ChangeSet source, boolean withMarkups) {
    if (changeInfo.changedBy(source)) {
      setChangeInfoUDBL(changeInfo.reassign(target));
      setEditBackgroundUDBL(null);
    }
    cellstream().forEach(cell -> cell.mergeChangeSetUDBL(target, source, withMarkups));
  }

  @Override
  public boolean enthaelt(InteractiveStepFragment fragment) {
    return cellstream().anyMatch(cell -> cell.enthaelt(fragment));
  }

  @Override
  public boolean enthaeltAenderungsmarkierungen() {
    return cellstream().anyMatch(cell -> cell.enthaeltAenderungsmarkierungen());
  }

  @Override
  public void findStepnumberLinkIDs(HashMap<TextEditArea, List<String>> stepnumberLinkMap) {
    cellstream().forEach(cell -> cell.findStepnumberLinkIDs(stepnumberLinkMap));
  }

  @Override
  public Shape getShape() {
    Shape tablePanelShape = new Shape(tablePanel);
    cellstream().forEach(cell -> tablePanelShape.add(cell.getShape()));
    return new Shape(this).add(tablePanelShape);
  }

  @Override
  public int spaltenbreitenAnpassenNachMausDragging(int delta, int spalte) {
    if (delta != 0) {
      return spalte == WHOLETABLE_COLUMN_INDICATOR
        ? updateTableWidthPercentage(delta)
        : updateColumnsWidthPercentage(delta, spalte);
    }
    return delta;
  }

  /** Widens the column of intrest by the number of passed pixels (respectively
   * shrink it by a negative value. The number of pixels is translated to a difference
   * in column width percentages, so that the resulting ratio remains the same if the
   * edit area or the table width is resized later on.
   * <p>
   * The behaviour is the same as it is known from MS Word: the overall width of the
   * table remains the same and the amount of groth for the column of interest is
   * taken from the following column. We can be shure that there is a following column
   * as the resizer at the right edge of the table is used to change the whole table
   * width. */
  private int updateColumnsWidthPercentage(int vergroesserung, int spalte) {
    Integer[] columnWidths = cells.get(0)
      .stream()
      .map(editContainer -> editContainer.getWidth())
      .toArray(Integer[]::new);
    List<Integer> recomputed = recomputePercents(columnWidths, vergroesserung, spalte);
    if (recomputed != null) {
      columnsWidthPercent = recomputed;
      recomputeLayout();
      return vergroesserung;
    }
    return 0;
  }

  private void recomputeLayout() {
    createTablePanelLayout(numColumns(), numRows());
    reassignCellsAndResizers();
    revalidate();
  }

  private void reassignCellsAndResizers() {
    tablePanel.removeAll();
    int columns = numColumns();
    int rows = numRows();
    createColumnResizers(columns, rows);
    for (int r = 0; r < rows; r++) {
      List<EditContainer> row = cells.get(r);
      for (int c = 0; c < columns; c++) {
        EditContainer cell = row.get(c);
        tablePanel.add(cell, CC.xy(2 + c * 2, 2 + r * 2));
      }
    }
  }

  private int updateTableWidthPercentage(int vergroesserung) {
    int currentTablePanelWidth = tablePanel.getWidth();
    int maximumTablePanelWidth = getWidth() - 2 * minimumBorderSize();
    if (currentTablePanelWidth + vergroesserung < 0) {
      // Dragging too far to the left is ignored
      return 0;
    }
    if (currentTablePanelWidth + vergroesserung > maximumTablePanelWidth) {
      // Dragging to the right exceeding the maximum available space is
      // interpreted as: bring the table back to 100% width
      vergroesserung = maximumTablePanelWidth - currentTablePanelWidth;
    }
    float newTablePanelWidth = currentTablePanelWidth + vergroesserung;
    tableWidthPercent = (int)(newTablePanelWidth / maximumTablePanelWidth * 100);
    refreshBorderSpaceGeometricsAndColor();
    revalidate();
    return vergroesserung;
  }

  @Override
  public EditContainer getParent() { return (EditContainer) super.getParent(); }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    selectionTracker.paintSelection(g);
  }

  int numColumns() { return cells.get(0).size(); }
  int numRows() { return cells.size(); }

  public void removeTableOrMarkAsDeletedUDBL() {
    // Marking as removed is only required if there is no change tracked yet for the table.
    // If the table is marked as created, it can simply be removed
    // If the table is already marked as removed, the method here should not have been called
    if (deletionsMustBeMarked()) {
      setChangeInfoUDBL(changeInfo.deleted());
      setGeloeschtMarkiertStilUDBL(changeInfo.changeSet());
    }
    else {
      getParent().removeEditAreaUDBL(this); // Already includes undo recording
      editor().diagrammAktualisieren(null);
    }
  }

  @Override
  public void requestFocus() {
    cells.get(0).get(0).requestFocus();
  }

  public void addRowUDBL(int rowIndex) {
    addEmptyRow(rowIndex);
    editor().addEdit(new UndoableTableRowAdded(this, rowIndex, cells.get(rowIndex)));
  }

  public void addEmptyRow(int rowIndex) {
    EditorI editor = editor();
    List<EditContainer> row = new ArrayList<>();
    for (int c = 0; c < numColumns(); c++) {
      row.add(new EditContainer(TextInit.initialChangeInfo()));
    }
    cells.add(rowIndex, row);
    recomputeLayout();
  }

  private boolean deletionsMustBeMarked() {
    return editor().aenderungenVerfolgen() && changeInfo.isUntracked();
  }

  public void removeRowOrMarkAsDeletedUDBL(int rowIndex) {
    EditorI editor = editor();
    if (rowIsMarkedAs(rowIndex, Hinzugefuegt) || !editor.aenderungenVerfolgen()) {
      if (numRows() > 1) {
        List<EditContainer> row = cells.get(rowIndex);
        removeRow(rowIndex);
        editor.addEdit(new UndoableTableRowRemoved(this, rowIndex, row));
      }
      else {
        removeTableOrMarkAsDeletedUDBL();
      }
    }
    else {
      cells.get(rowIndex).forEach(cell -> cell.setGeloeschtMarkiertStilUDBL(null, changeInfo.changeSet()));
    }
  }

  public void removeRow(int rowIndex) {
    cells.remove(rowIndex);
    recomputeLayout();
  }

  public void addRow(int rowIndex, List<EditContainer> row) {
    cells.add(rowIndex, row);
    recomputeLayout();
  }

  /** Adding a column does not change the table width. If the column widths have not
   * yet explicitly been set, all columns will have the same width after adding a new
   * one. Otherwise, the new column will steal half of the width of the preceeding column
   * (respectively half of the <i>last</i> column when the new one is appended). */
  public void addColumnUDBL(int colIndex) {
    EditorI editor = editor();
    List<Integer> originalColumnsWidthPercent = addEmptyColumn(colIndex);
    editor.addEdit(new UndoableTableColumnAdded(this, colIndex, toColumn(colIndex), originalColumnsWidthPercent));
  }

  public List<Integer> addEmptyColumn(int colIndex) {
    EditorI editor = editor();

    List<Integer> originalColumnsWidthPercent = allocPercent(colIndex, columnsWidthPercent);
    for (List<EditContainer> row : cells) {
      row.add(colIndex, new EditContainer(TextInit.initialChangeInfo()));
    }
    recomputeLayout();
    return originalColumnsWidthPercent;
  }

  private List<EditContainer> toColumn(int columnIndex) {
    return this.cells.stream().map(row -> row.get(columnIndex)).collect(Collectors.toList());
  }

  public void removeColumn(int colIndex, List<Integer> columnsWidthPercent) {
    this.columnsWidthPercent = copyOf(columnsWidthPercent);
    this.cells.forEach(row -> row.remove(colIndex));
    recomputeLayout();
  }

  /** Removing a column does not change the table width. If the column widths have not
   * yet explicitly been set, all columns will have the same width after removing one.
   * Otherwise, the removed column's width is added to the preceeding column's width
   * (respectively to the new <i>last</i> column's width when removing the last one). */
  public void removeColumnOrMarkAsDeletedUDBL(int colIndex) {
    EditorI editor = editor();
    if (columnIsMarkedAs(colIndex, Hinzugefuegt) || !editor.aenderungenVerfolgen()) {
      if (numColumns() > 1) {
        List<Integer> originalColumnsWidthPercent = copyOf(columnsWidthPercent);
        List<EditContainer> column = removeColumn(colIndex);
        editor.addEdit(new UndoableTableColumnRemoved(this, colIndex, column, originalColumnsWidthPercent));
      } else {
        removeTableOrMarkAsDeletedUDBL();
      }
    }
    else {
      cells.forEach(row -> row.get(colIndex).setGeloeschtMarkiertStilUDBL(null, changeInfo.changeSet()));
    }
  }

  public List<EditContainer> removeColumn(int colIndex) {
    List<EditContainer> column = new ArrayList<>();
    columnsWidthPercent = releasePercent(colIndex, columnsWidthPercent);
    for (List<EditContainer> row : cells) {
      column.add(row.remove(colIndex));
    }
    recomputeLayout();
    return column;
  }

  public void addColumn(int colIndex, List<EditContainer> column, List<Integer> originalColumnsWidthPercent) {
    for (int r = 0; r < numRows(); r++) {
      List<EditContainer> row = cells.get(r);
      row.add(colIndex, column.get(r));
    }
    columnsWidthPercent = originalColumnsWidthPercent;
    recomputeLayout();
  }

  @Override public ChangeInfo getChangeInfo() {
    return changeInfo;
  }

  @Override public void setChangeInfo(ChangeInfo changeInfo) {
    this.changeInfo = changeInfo;
  }

  private void setChangeInfoUDBL(ChangeInfo changeInfo) {
    UDBL.setChangeInfo(this, changeInfo);
  }

  public boolean isMarkedAsDeleted() { return changeInfo.isDeleted(); }
  private boolean allCellsMarkedAs(Aenderungsart aenderungsart) {
    return cells.stream().allMatch(row -> rowIsMarkedAs(row, aenderungsart));
  }

  public boolean rowIsMarkedAs(int rowIndex, Aenderungsart aenderungsart) { return rowIsMarkedAs(cells.get(rowIndex), aenderungsart); }
  private boolean rowIsMarkedAs(List<EditContainer> row, Aenderungsart aenderungsart) {
    return row.stream().allMatch(cell -> cell.isMarkedAs(aenderungsart));
  }

  public boolean columnIsMarkedAs(int columnIndex, Aenderungsart aenderungsart) {
    return toColumn(columnIndex).stream().allMatch(cell -> cell.isMarkedAs(aenderungsart));
  }

  @Override
  public List<JTextComponent> getTextAreas() {
    List<JTextComponent> result = new ArrayList<>();
    for (List<EditContainer> row : cells) {
      row.stream().forEach(ec -> result.addAll(ec.getTextAreas()));
    }
    return result;
  }

  @Override
  public void viewsNachinitialisieren() {
    if (changeInfo.isDeleted()) {
      for (List<EditContainer> row : cells) {
        row.stream().forEach(ec -> ec.setGeloeschtMarkiertStilUDBL(null, changeInfo.changeSet()));
      }
    }
    for (List<EditContainer> row : cells) {
      row.stream().forEach(ec -> ec.viewsNachinitialisieren());
    }
  }

  @Override public void setQuellStil(ChangeSet changeSet) { /* Not required for tables - source steps only contain an empty text area */ }
  @Override public void addSchrittnummer(StepnumberLabel schrittNummer) { add(schrittNummer); }
  @Override public Component asComponent() { return this; }
  @Override public String getPlainText() { return ""; }
  @Override public TextEditArea asTextArea() { return null; }
  @Override public ImageEditArea asImageArea() { return null; }
  @Override public String getText() { return "table"; }
  @Override public void setEditDecorationIndentions(Indentions indentions) { /* Nothing to do */ }
  @Override public boolean isTableEditArea() { return true; }
}
