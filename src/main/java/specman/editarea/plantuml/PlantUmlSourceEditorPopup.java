package specman.editarea.plantuml;

import org.apache.commons.lang.StringEscapeUtils;
import specman.graphics.Styles;
import specman.util.NoteEditorPopup;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import static specman.Specman.editor;
import static specman.i18n.Messages.i18n;

/** The editor for PlantUML source. Most intermediate states while typing are not sensible diagrams, so the diagram
 * is only updated on F5, and the editor can only be left with source that PlantUML accepts. A line below the text
 * names the problem, or the F5 key if there is none. */
public class PlantUmlSourceEditorPopup extends NoteEditorPopup {
  private static final PlantUmlSourceValidator VALIDATOR = new PlantUmlSourceValidator();
  private static final Font SOURCE_FONT = Styles.metaTagFont.deriveFont(Font.PLAIN, 13);
  private static final int COLUMNS = 30;
  private static final int MIN_ROWS = 6;
  private static final int MAX_ROWS = 20;
  /** The editor overlaps the right edge of the diagram a little, so it does not sit exactly on the step's border line.
   * Like the font, the inset is given for 100% zoom. */
  private static final int INSET_OVER_DIAGRAM = 20;
  private static final int PROBLEM_CHECK_DELAY_MS = 600;
  private static final Color HINT_COLOR = Color.GRAY;
  private static final Color SEPARATOR_COLOR = Color.GRAY;

  private final JLabel statusLabel = new JLabel();
  private final Timer problemCheckTimer = new Timer(PROBLEM_CHECK_DELAY_MS, e -> updateStatus());
  private final Consumer<String> onPreview;

  /** @param onPreview receives the source when the user presses F5 and the source has no problem */
  public PlantUmlSourceEditorPopup(String source, Consumer<String> onCommit, Runnable onClosed,
                                   Consumer<String> onPreview) {
    this(scaledSourceFont(), source, onCommit, onClosed, onPreview);
  }

  private PlantUmlSourceEditorPopup(Font font, String source, Consumer<String> onCommit, Runnable onClosed,
                                    Consumer<String> onPreview) {
    super(font, source, onCommit, onClosed);
    setTextSize(COLUMNS, MIN_ROWS, MAX_ROWS);
    this.onPreview = onPreview;
    statusLabel.setFont(font.deriveFont(Font.PLAIN));
    statusLabel.setBackground(NOTE_BACKGROUND);
    statusLabel.setOpaque(true);
    statusLabel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(1, 0, 0, 0, SEPARATOR_COLOR),
      BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    setBelowText(statusLabel);
    problemCheckTimer.setRepeats(false);
    bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "preview", this::preview);
  }

  /** The font is given for 100% zoom and follows the editor's zoom. It is proportional, because a monospaced font is
   * too wide for the narrow editor. */
  private static Font scaledSourceFont() {
    return SOURCE_FONT.deriveFont((float) editor().scale(SOURCE_FONT.getSize2D()));
  }

  /** Shows the editor at the right edge of the diagram, overlapping it a little, see {@link #INSET_OVER_DIAGRAM}. */
  public void showRightOf(JComponent diagram) {
    showRightOf(diagram, (int) Math.round(editor().scale(INSET_OVER_DIAGRAM)));
  }

  @Override
  protected boolean canCommit() {
    problemCheckTimer.stop();
    return VALIDATOR.problemOf(text()) == null;
  }

  @Override
  protected void textChanged() {
    problemCheckTimer.restart();
  }

  @Override
  protected void beforeShow() {
    updateStatus();
  }

  private void preview() {
    String problem = VALIDATOR.problemOf(text());
    showStatus(problem);
    if (problem == null) {
      onPreview.accept(text());
    }
  }

  private void updateStatus() {
    showStatus(VALIDATOR.problemOf(text()));
  }

  private void showStatus(String problem) {
    statusLabel.setForeground(problem != null ? Styles.PROBLEM_COLOR : HINT_COLOR);
    statusLabel.setText(wrappedToTextWidth(problem != null ? problem : i18n("msg.plantuml.hint")));
    pack();
  }

  /** A long message wraps within the width of the text instead of making the editor wider. */
  private String wrappedToTextWidth(String message) {
    Insets insets = statusLabel.getInsets();
    int labelTextWidth = textWidth() - insets.left - insets.right;
    return "<html><body style='width:" + labelTextWidth + "px'>" + StringEscapeUtils.escapeHtml(message) + "</body></html>";
  }
}
