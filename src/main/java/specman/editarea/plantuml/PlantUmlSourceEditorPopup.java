package specman.editarea.plantuml;

import org.apache.commons.lang.StringEscapeUtils;
import specman.graphics.Styles;
import specman.util.NoteEditorPopup;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import static specman.i18n.Messages.i18n;

/** The editor for PlantUML source. Most intermediate states while typing are not sensible diagrams, so the diagram
 * is only updated on F5, and the editor can only be left with source that PlantUML accepts. A line below the text
 * names the problem, or the F5 key if there is none. */
public class PlantUmlSourceEditorPopup extends NoteEditorPopup {
  private static final PlantUmlSourceValidator VALIDATOR = new PlantUmlSourceValidator();
  private static final int PROBLEM_CHECK_DELAY_MS = 600;
  private static final Color HINT_COLOR = Color.GRAY;
  private static final Color SEPARATOR_COLOR = Color.GRAY;

  private final JLabel statusLabel = new JLabel();
  private final Timer problemCheckTimer = new Timer(PROBLEM_CHECK_DELAY_MS, e -> updateStatus());
  private final Consumer<String> onPreview;

  /** @param onPreview receives the source when the user presses F5 and the source has no problem */
  public PlantUmlSourceEditorPopup(Font font, String source, Consumer<String> onCommit, Runnable onClosed,
                                   Consumer<String> onPreview) {
    super(font, source, onCommit, onClosed);
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
