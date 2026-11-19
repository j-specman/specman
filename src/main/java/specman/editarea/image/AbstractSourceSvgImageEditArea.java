package specman.editarea.image;

import specman.ChangeInfo;
import specman.pdf.ShapeImage;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import static specman.Specman.editor;

/** An SVG edit area which shows a diagram rendered from its source. A double click opens an editor for the source to
 * the right of the diagram, a right click its context menu. The diagram is only updated when the editor is left or the
 * user asks for a preview, because most intermediate states while typing are not sensible diagrams.
 * <p>
 * Subclasses say how the source is rendered and checked, see {@link #renderSvg(String)} and {@link #sourceValidator()}.
 * Subclasses can track changes of the source as a whole, see {@link #applySourceChange(String)}, or give the source
 * change marks of its own. */
public abstract class AbstractSourceSvgImageEditArea extends AbstractSvgImageEditArea {
  private String source;
  /** Differs from the source while the editor shows the preview the user asked for. */
  private String displayedSource;
  /** True from the insertion of a new diagram until the user confirmed some content. Leaving the editor
   * before that takes the diagram out again, as if it had never been inserted. */
  private boolean awaitingFirstContent;

  protected AbstractSourceSvgImageEditArea(String source, float individualScalePercent, ChangeInfo changeInfo) {
    super(changeInfo, individualScalePercent);
    this.source = source;
  }

  /** @return the diagram for the given source as SVG */
  protected abstract byte[] renderSvg(String source);

  protected abstract SourceValidator sourceValidator();

  /** @return the message key of the hint which the editor shows below the source as long as it has no problem */
  protected abstract String hintMessageKey();

  /** Asks the user for a file and replaces the source by its content. */
  public abstract void importSource();

  /** Subclasses call this once they are constructed, to show the diagram for the source. */
  protected void displayInitially() {
    displayedSource = source;
    showSvg(renderSvg(source));
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

  /** Shows the diagram for the given source, which doesn't have to be the source of the area: The editor uses this
   * for its preview. */
  protected void show(String sourceToShow) {
    displayedSource = sourceToShow;
    showSvg(renderSvg(sourceToShow));
    imageChanged();
  }

  /** Rendered anew, as PDF export is rare and rendering is cheap, so there is no need to keep the SVG in memory. */
  @Override
  protected ShapeImage createShapeImage() {
    return vectorShapeImage(renderSvg(displayedSource));
  }

  // ---- editing ----

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (SwingUtilities.isRightMouseButton(e)) {
      SourceImageContextMenu.open(this, e);
    }
    else if (e.getClickCount() == 2 && !getChangeInfo().isDeleted()) {
      editSource();
    }
  }

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
   * the user enter the content right away. The relayout which the insertion requested may still be pending, and the
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

  /** Called when the editor was closed with source it accepted. The first content of a new diagram belongs to its
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

  /** Called when the editor was closed, no matter if the source was committed or discarded. A new diagram which got
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
