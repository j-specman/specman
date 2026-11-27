package specman.editarea.image;

import specman.ChangeInfo;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import static specman.Specman.editor;

/** An edit area which shows a vector diagram rendered from its source. A double click opens the editor of the diagram,
 * a right click its context menu, which also offers the source editor.
 * <p>
 * By default, the double click opens a source editor, a popup to the right of the diagram in which the user types the
 * image's source code, see {@link #editSource()}. A subclass can have an editor of its own instead, see {@link #doubleClicked()}:
 * PlantUML images use the source editor, Classman diagramms images open a diagram editor in a window of its own.
 * <p>
 * The source editor only updates the diagram when it is closed or the user asks for a preview, because most
 * intermediate states while typing are not reasonable diagrams even if there is no syntax error.
 * <p>
 * Subclasses say how the source is rendered and checked, see {@link #pictureOf(String)} and {@link #sourceValidator()}.
 * Subclasses can track changes of the source as a whole, see {@link #applySourceChange(String)}, or give the source
 * change marks of its own. */
public abstract class AbstractSourceImageEditArea extends AbstractVectorImageEditArea {
  /** The content of the image area: what the model persists, what the user's changes and undo apply to, and what the
   * editors start from. Unlike {@link #displayedSource}, it doesn't follow the preview of the source editor. */
  private String source;
  /** What the picture is drawn from right now. It is the {@link #source}, except while the source editor shows the
   * preview the user asked for. */
  private String displayedSource;
  /** True from the insertion of a new diagram until the user confirmed some content. Leaving the editor
   * before that takes the diagram out again, as if it had never been inserted. */
  private boolean awaitingFirstContent;

  protected AbstractSourceImageEditArea(String source, float individualScalePercent, ChangeInfo changeInfo) {
    super(changeInfo, individualScalePercent);
    this.source = source;
  }

  /** @return the picture shown for the given source, which is also what the PDF export takes over */
  protected abstract Picture pictureOf(String source);

  protected abstract SourceValidator sourceValidator();

  /** @return the message key of the hint which the editor shows below the source as long as it has no problem */
  protected abstract String hintMessageKey();

  /** Asks the user for a file and replaces the source by its content. */
  public abstract void importSource();

  /** Subclasses call this once they are constructed, to show the diagram for the source. */
  protected void displayInitially() {
    displayedSource = source;
    showPicture(pictureOf(source));
  }

  public String getSource() { return source; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setSource(String source) {
    this.source = source;
    display(source);
  }

  // ---- display ----

  private void display(String sourceToShow) {
    if (!sourceToShow.equals(displayedSource)) {
      show(sourceToShow);
    }
  }

  /** Shows the diagram for the given source, which doesn't have to be the source of the area: The source editor uses
   * this for its preview. */
  protected void show(String sourceToShow) {
    displayedSource = sourceToShow;
    showPicture(pictureOf(sourceToShow));
    imageChanged();
  }

  // ---- editing ----

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (SwingUtilities.isRightMouseButton(e)) {
      SourceImageContextMenu.open(this, e);
    }
    else if (e.getClickCount() == 2 && !getChangeInfo().isDeleted()) {
      doubleClicked();
    }
  }

  /** The source editor opens by default. A subclass with an editor of its own opens that one instead. */
  protected void doubleClicked() {
    editSource();
  }

  /** Opens the source editor to the right of the diagram, which every subclass offers in the context menu. */
  public void editSource() {
    SourceEditorPopup popup = newEditorPopup();
    if (awaitingFirstContent) {
      int emptyLineStart = source.indexOf('\n') + 1;
      popup.select(emptyLineStart, emptyLineStart);
    }
    popup.showRightOf(this);
  }

  protected SourceEditorPopup newEditorPopup() {
    return new SourceEditorPopup(source, this::commitSource, this::editorClosed, this::show, sourceValidator(),
      hintMessageKey());
  }

  /** For a diagram that has just been inserted with an empty template as source, whose second line is empty: lets
   * the user enter the content right away, in the source editor. A subclass with an editor of its own opens that one
   * instead. The relayout which the insertion requested may still be pending, and the
   * editor is placed by the diagram's position on screen at the time it is shown. So the layout is completed and the
   * diagram scrolled into view first, otherwise the editor can end up far from the diagram, e.g. for a diagram in
   * the outro. */
  public void startFirstEdit() {
    awaitingFirstContent = true;
    if (isShowing()) {
      SwingUtilities.getRoot(this).validate();
      scrollRectToVisible(new Rectangle(getSize()));
      editSource();
    }
  }

  /** Called when the source editor was closed with source it accepted. The first content of a new diagram belongs to its
   * insertion, so a single undo takes the whole diagram out. */
  public void commitSource(String newSource) {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      setSource(newSource);
    }
    else {
      changeSource(newSource);
    }
  }

  /** Called when the source editor was closed, no matter if the source was committed or discarded. A new diagram which got
   * no content is taken out again, without trace. */
  public void editorClosed() {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      editor().undoLastEditAndForget();
    }
    else {
      display(source);
    }
  }

  /** Replaces the source by an edit of the user, as a single undo step together with what {@link #applySourceChange}
   * records. */
  public void changeSource(String newSource) {
    if (newSource.equals(source)) {
      return;
    }
    try (UndoRecording ur = editor().composeUndo()) {
      applySourceChange(newSource);
    }
  }

  /** Called within the undo recording of {@link #changeSource(String)}. Subclasses record more undoable changes
   * around the change of the source. */
  protected void applySourceChange(String newSource) {
    UDBL.setSourceUDBL(this, newSource);
  }
}
