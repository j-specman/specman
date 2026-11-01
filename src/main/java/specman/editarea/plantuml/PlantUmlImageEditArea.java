package specman.editarea.plantuml;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import specman.ChangeInfo;
import specman.EditorI;
import specman.editarea.AbstractImageEditArea;
import specman.model.v002.ImageEditAreaModel_V002;
import specman.undo.manager.UndoRecording;
import specman.undo.props.UDBL;
import specman.util.NoteEditorPopup;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static specman.Specman.editor;

/** An edit area showing a PlantUML diagram, rendered as SVG so it stays sharp at any zoom level.
 * A double click opens a note-like editor for the source to the right of the diagram. The diagram is only updated
 * on F5 and when the editor is left, because most intermediate states while typing are not sensible diagrams.
 * Proof of concept: the model still persists only the rendered bitmap, not the PlantUML source. */
public class PlantUmlImageEditArea extends AbstractImageEditArea {
  private static final PlantUmlRenderer RENDERER = new PlantUmlRenderer();
  private static final Font SOURCE_FONT = new Font("Courier Prime", Font.PLAIN, 13);
  private static final int SOURCE_COLUMNS = 30;
  private static final int SOURCE_MIN_ROWS = 6;
  private static final int SOURCE_MAX_ROWS = 20;
  /** The editor overlaps the right edge of the area a little, so it does not sit exactly on the step's border line.
   * Like the editor's font, the inset is given for 100% zoom. */
  private static final int SOURCE_EDITOR_INSET = 20;

  private String source;
  /** Differs from the source while the editor shows the preview the user asked for by F5. */
  private String displayedSource;
  private FlatSVGIcon diagram;
  /** True from the insertion of a new diagram until the user confirmed some content. Leaving the editor
   * before that takes the diagram out again, as if it had never been inserted. */
  private boolean awaitingFirstContent;
  public PlantUmlImageEditArea(String source, ChangeInfo changeInfo) {
    super(changeInfo, 1);
    this.source = source;
    this.displayedSource = source;
    this.diagram = RENDERER.renderDiagram(source);
  }

  public String getSource() { return source; }

  /** Not undoable on its own, see {@link #changeSource(String)}. */
  public void setSource(String source) {
    this.source = source;
    display(source);
  }

  public void changeSource(String newSource) {
    EditorI editor = editor();
    try (UndoRecording ur = editor.composeUndo()) {
      UDBL.setPlantUmlSourceUDBL(this, newSource);
    }
  }

  /** For a diagram that has just been inserted with empty content: lets the user enter the content right away. */
  public void startFirstEdit() {
    awaitingFirstContent = true;
    if (isShowing()) {
      editSource();
    }
  }

  /** The first content of a new diagram belongs to its insertion, so a single undo takes the whole diagram out. */
  public void commitSource(String newSource) {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      setSource(newSource);
    }
    else {
      changeSource(newSource);
    }
  }

  /** Called when the editor was closed, no matter if the source was committed or discarded. */
  public void editorClosed() {
    if (awaitingFirstContent) {
      awaitingFirstContent = false;
      editor().undoLastEditAndForget();
    }
    else {
      display(source);
    }
  }

  private void display(String sourceToShow) {
    if (!sourceToShow.equals(displayedSource)) {
      displayedSource = sourceToShow;
      diagram = RENDERER.renderDiagram(sourceToShow);
      imageChanged();
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    super.mouseClicked(e);
    if (e.getClickCount() == 2 && !getChangeInfo().isDeleted()) {
      editSource();
    }
  }

  private void editSource() {
    EditorI editor = editor();
    Font scaledFont = SOURCE_FONT.deriveFont((float) editor.scale(SOURCE_FONT.getSize2D()));
    int scaledInset = (int) Math.round(editor.scale(SOURCE_EDITOR_INSET));
    NoteEditorPopup popup = new PlantUmlSourceEditorPopup(scaledFont, source, this::commitSource, this::editorClosed, this::display)
      .withSize(SOURCE_COLUMNS, SOURCE_MIN_ROWS, SOURCE_MAX_ROWS);
    if (awaitingFirstContent) {
      int emptyLineStart = source.indexOf('\n') + 1;
      popup.select(emptyLineStart, emptyLineStart);
    }
    popup.showRightOf(this, scaledInset);
  }

  @Override
  protected int intrinsicWidth() { return diagram.getIconWidth(); }

  @Override
  protected Icon createScaledIcon(float totalScalePercent) {
    return diagram.derive(
      Math.round(diagram.getIconWidth() * totalScalePercent),
      Math.round(diagram.getIconHeight() * totalScalePercent));
  }

  @Override
  public BufferedImage getFullSizeImage() {
    BufferedImage bitmap = new BufferedImage(diagram.getIconWidth(), diagram.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = bitmap.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      diagram.paintIcon(new JLabel(), g, 0, 0);
    }
    finally {
      g.dispose();
    }
    return bitmap;
  }

  @Override
  public ImageEditAreaModel_V002 toModel(boolean formatierterText) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ImageIO.write(getFullSizeImage(), PERSISTED_IMAGETYPE, bytes);
      return new ImageEditAreaModel_V002(bytes.toByteArray(), PERSISTED_IMAGETYPE, individualScalePercent, getChangeInfo());
    }
    catch (IOException iox) {
      throw new UncheckedIOException(iox);
    }
  }
}
