package specman.editarea;

import specman.Specman;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Objects;

import static specman.Aenderungsart.Geloescht;
import static specman.CursorFactory.HotspotPlacement.Bottom;
import static specman.CursorFactory.HotspotPlacement.BottomRight;
import static specman.CursorFactory.HotspotPlacement.Right;
import static specman.CursorFactory.createCursor;
import static specman.view.AbstractSchrittView.LINIENBREITE;
import static specman.Specman.editor;

public class TableEditAreaSelectionTracker implements MouseListener, MouseMotionListener {
  private static final Color SELECTION_COLOR = new Color(200, 200, 200, 150);
  private static final int SELEECTION_GAP_SIZE = 4 * LINIENBREITE;
  public static final Cursor ADD_COLUMN_CURSOR = createCursor("add-column-cursor", Bottom);
  public static final Cursor REMOVE_COLUMN_CURSOR = createCursor("remove-column-cursor", Bottom);
  public static final Cursor ADD_ROW_CURSOR = createCursor("add-row-cursor", Right);
  public static final Cursor REMOVE_ROW_CURSOR = createCursor("remove-row-cursor", Right);
  public static final Cursor REMOVE_TABLE_CURSOR = createCursor("remove-table-cursor", BottomRight);

  public enum Operation {
    AddColumn(ADD_COLUMN_CURSOR),
    AddRow(ADD_ROW_CURSOR),
    RemoveColumn(REMOVE_COLUMN_CURSOR),
    RemoveRow(REMOVE_ROW_CURSOR),
    RemoveTable(REMOVE_TABLE_CURSOR);

    private Cursor cursor;
    Operation(Cursor cursor) { this.cursor = cursor; }
    Cursor toCursor() { return cursor; }
  }

  private final TableEditArea editArea;
  private final JPanel tablePanel;
  private Rectangle selectionHighlight;
  private Integer selectionIndex;
  private Operation selectionOperation;

  public TableEditAreaSelectionTracker(TableEditArea editArea) {
    this.editArea = editArea;
    this.tablePanel = editArea.tablePanel;
    editArea.addMouseListener(this);
    editArea.addMouseMotionListener(this);
  }

  @Override public void mousePressed(MouseEvent e) {}
  @Override public void mouseReleased(MouseEvent e) {}
  @Override public void mouseEntered(MouseEvent e) {}
  @Override public void mouseDragged(MouseEvent e) {}

  @Override public void mouseClicked(MouseEvent e) {
    try(UndoRecording ur = editor().composeUndo()) {
      if (selectionOperation != null) {
        setEditAreaCursor(null);
        switch(selectionOperation) {
          case RemoveTable -> editArea.removeTableOrMarkAsDeletedUDBL();
          case AddRow -> editArea.addRowUDBL(selectionIndex);
          case RemoveRow -> editArea.removeRowOrMarkAsDeletedUDBL(selectionIndex);
          case AddColumn -> editArea.addColumnUDBL(selectionIndex);
          case RemoveColumn -> editArea.removeColumnOrMarkAsDeletedUDBL(selectionIndex);
        }
        resetSelection();
      }
    }
  }

  @Override
  public void mouseExited(MouseEvent e) {
    resetSelection();
    editArea.repaint();
  }

  private void resetSelection() {
    selectionHighlight = null;
    selectionOperation = null;
    setEditAreaCursor(null);
  }

  /** Found this little trick at https://coderanch.com/t/710608/java/set-cursor-JButton
   * Setting the cursor of the editArea itself doesn't do the job. Setting it on the
   * root pane instead works well. Root pane must be null-checked for the case the
   * users requested deletion of the table which will cause the edit area to be detached
   * from its {@link EditContainer}. */
  private void setEditAreaCursor(Cursor cursor) {
    if (editArea.getRootPane() != null) {
      editArea.getRootPane().setCursor(cursor);
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (editArea.isMarkedAsDeleted()) {
      // Tables being marked as deleted do not support changes in row and column layout
      return;
    }
    Point mousePos = e.getPoint();
    Rectangle selectionUpdate = null;
    selectionUpdate = wholeTableSelected(mousePos);
    if (selectionUpdate == null) {
      selectionUpdate = yToTableRowGabSelection(mousePos);
    }
    if (selectionUpdate == null) {
      selectionUpdate = xToTableColumnGapSelection(mousePos);
    }
    if (selectionUpdate == null) {
      selectionUpdate = yToTableRowSelection(mousePos);
    }
    if (selectionUpdate == null) {
      selectionUpdate = xToTableColumnSelection(mousePos);
    }
    if (!Objects.equals(selectionHighlight, selectionUpdate)) {
      selectionHighlight = selectionUpdate;
      setEditAreaCursor(selectionHighlight != null ? selectionOperation.toCursor() : null);
      editArea.repaint();
    }
  }

  private Rectangle wholeTableSelected(Point mousePos) {
    if (mousePos.y < tablePanel.getY() && mousePos.x < tablePanel.getX()) {
      selectionOperation = Operation.RemoveTable;
      return tablePanel.getBounds();
    }
    return null;
  }

  private Rectangle xToTableColumnGapSelection(Point mousePos) {
    if (mousePos.y < tablePanel.getY()) {
      final int x = mousePos.x - tablePanel.getX();
      List<EditContainer> leadingRow = editArea.cells.get(0);
      EditContainer columnLeader = null;
      for (int c = 0; c < leadingRow.size(); c++) {
        columnLeader = leadingRow.get(c);
        Rectangle selection = xToTableColumnGapSelection(x, columnLeader.getX() - LINIENBREITE, columnLeader);
        if (selection != null) {
          selectionIndex = c;
          return selection;
        }
      }

      Rectangle selection = xToTableColumnGapSelection(x, columnLeader.getX() + columnLeader.getWidth(), columnLeader);
      if (selection != null) {
        selectionIndex = leadingRow.size();
      }
      return selection;
    }
    return null;
  }

  private Rectangle xToTableColumnGapSelection(int x, int xGapLeft, EditContainer cell) {
    if (x >= xGapLeft && x <= xGapLeft + SELEECTION_GAP_SIZE) {
      selectionOperation = Operation.AddColumn;
      return new Rectangle(xGapLeft + tablePanel.getX(), tablePanel.getY(), SELEECTION_GAP_SIZE, tablePanel.getHeight());
    }
    return null;
  }

  private Rectangle yToTableRowGabSelection(Point mousePos) {
    if (mousePos.x < tablePanel.getX()) {
      final int y = mousePos.y - tablePanel.getY();
      EditContainer rowLeader = null;
      for (int r = 0; r < editArea.cells.size(); r++) {
        rowLeader = editArea.cells.get(r).get(0);
        Rectangle selection = yToTableRowGapSelection(y, rowLeader.getY() - LINIENBREITE, rowLeader);
        if (selection != null) {
          selectionIndex = r;
          return selection;
        }
      }

      Rectangle selection = yToTableRowGapSelection(y, rowLeader.getY() + rowLeader.getHeight(), rowLeader);
      if (selection != null) {
        selectionIndex = editArea.cells.size();
      }
      return selection;
    }
    return null;
  }

  private Rectangle yToTableRowGapSelection(int y, int yGapTop, EditContainer rowLeader) {
    if (y >= yGapTop && y <= yGapTop + SELEECTION_GAP_SIZE) {
      selectionOperation = Operation.AddRow;
      return new Rectangle(tablePanel.getX(), yGapTop + tablePanel.getY(), tablePanel.getWidth(), SELEECTION_GAP_SIZE);
    }
    return null;
  }

  private Rectangle xToTableColumnSelection(Point mousePos) {
    if (mousePos.y < tablePanel.getY()) {
      final int x = mousePos.x - tablePanel.getX();
      List<EditContainer> leadingRow = editArea.cells.get(0);
      for (int c = 0; c < leadingRow.size(); c++) {
        EditContainer columnLeader = leadingRow.get(c);
        if (isAtXPosition(x, columnLeader) && !editArea.columnIsMarkedAs(c, Geloescht)) {
          selectionIndex = c;
          selectionOperation = Operation.RemoveColumn;
          return new Rectangle(
            tablePanel.getX() + columnLeader.getX(),
            tablePanel.getY(),
            columnLeader.getWidth(),
            tablePanel.getHeight());
        }
      }
    }
    selectionIndex = null;
    return null;
  }

  private boolean isAtXPosition(int x, EditContainer cell) {
    return cell.getX() <= x && cell.getX() + cell.getWidth() >= x;
  }

  private Rectangle yToTableRowSelection(Point mousePos) {
    if (mousePos.x < tablePanel.getX()) {
      final int y = mousePos.y - tablePanel.getY();
      for (int r = 0; r < editArea.cells.size(); r++) {
        EditContainer rowLeader = editArea.cells.get(r).get(0);
        if (isAtYPosition(y, rowLeader) && !editArea.rowIsMarkedAs(r, Geloescht)) {
          selectionIndex = r;
          selectionOperation = Operation.RemoveRow;
          return new Rectangle(
            tablePanel.getX(),
            tablePanel.getY() + rowLeader.getY(),
            tablePanel.getWidth(),
            rowLeader.getHeight());
        }
      }
    }
    return null;
  }

  private boolean isAtYPosition(int y, EditContainer rowLeader) {
    return rowLeader.getY() <= y && rowLeader.getY() + rowLeader.getHeight() >= y;
  }

  public void paintSelection(Graphics g) {
    if (selectionHighlight != null) {
      g.setColor(SELECTION_COLOR);
      g.fillRect(selectionHighlight.x, selectionHighlight.y, selectionHighlight.width, selectionHighlight.height);
    }
  }
}
