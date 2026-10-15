package experiments.spellcheck;

import org.languagetool.JLanguageTool;
import org.languagetool.language.GermanyGerman;
import org.languagetool.rules.RuleMatch;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.Utilities;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/** Minimal spell checking experiment: LanguageTool checks the text of a JEditorPane with HTMLEditorKit in
 * the background, errors are shown as red squiggles via a Highlighter (the document itself stays untouched)
 * and suggestions are offered in a popup menu. */
public class SpellcheckExperiment extends JFrame {
  private static final int CHECK_DELAY_MS = 500;
  private static final int MAX_SUGGESTIONS = 5;
  private static final String SAMPLE_TEXT =
      "<html><body><p>Der Benutzer gibt seine Anmeldedaten ein und das System prüft ob die Eingabe korrekt ist. " +
      "Falls das Pasword falsch ist wird eine Fehlermeldumg angezeigt.</p>" +
      "<p>Dieser Satz ist <b>fett</b> und <i>kursiv</i> und <font size=\"7\">groß</font> gemischt mit Fehlren.</p></body></html>";

  private final JEditorPane editor = new JEditorPane();
  private final Highlighter.HighlightPainter squigglePainter = new SquigglePainter(Color.RED);
  private final JLabel status = new JLabel(" ");
  private final Timer recheckTimer = new Timer(CHECK_DELAY_MS, e -> startCheck());
  private JLanguageTool languageTool;
  private List<RuleMatch> currentMatches = new ArrayList<>();

  public SpellcheckExperiment() {
    super("Spellcheck experiment");
    recheckTimer.setRepeats(false);

    editor.setEditorKit(new HTMLEditorKit());
    editor.setText(SAMPLE_TEXT);
    editor.getDocument().addDocumentListener(new SimpleDocumentListener(recheckTimer::restart));
    editor.addMouseListener(new MouseAdapter() {
      @Override public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
      @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
    });

    add(new JScrollPane(editor), BorderLayout.CENTER);
    add(status, BorderLayout.SOUTH);
    setSize(600, 300);
    setDefaultCloseOperation(EXIT_ON_CLOSE);

    status.setText("Loading LanguageTool...");
    new SwingWorker<JLanguageTool, Void>() {
      @Override protected JLanguageTool doInBackground() {
        return new JLanguageTool(new GermanyGerman());
      }
      @Override protected void done() {
        try {
          languageTool = get();
          startCheck();
        }
        catch (InterruptedException | ExecutionException ex) {
          status.setText("LanguageTool failed: " + ex);
        }
      }
    }.execute();
  }

  private void startCheck() {
    if (languageTool == null) {
      return;
    }
    // Document.getText offsets are identical to the offsets the Highlighter works with
    String text;
    try {
      Document doc = editor.getDocument();
      text = doc.getText(0, doc.getLength());
    }
    catch (BadLocationException ex) {
      throw new IllegalStateException(ex);
    }
    new SwingWorker<List<RuleMatch>, Void>() {
      @Override protected List<RuleMatch> doInBackground() throws Exception {
        return languageTool.check(text);
      }
      @Override protected void done() {
        try {
          // Ignore stale results if the text was edited while checking
          if (editor.getDocument().getLength() == text.length()) {
            showMatches(get());
          }
        }
        catch (InterruptedException | ExecutionException ex) {
          status.setText("Check failed: " + ex);
        }
      }
    }.execute();
  }

  private void showMatches(List<RuleMatch> matches) {
    currentMatches = matches;
    Highlighter highlighter = editor.getHighlighter();
    highlighter.removeAllHighlights();
    for (RuleMatch match : matches) {
      try {
        highlighter.addHighlight(match.getFromPos(), match.getToPos(), squigglePainter);
      }
      catch (BadLocationException ex) {
        throw new IllegalStateException(ex);
      }
    }
    // The highlighter only repaints the box between the range's start and end position, which is wrong
    // for lines mixing font sizes (e.g. <font size="7">), so we repaint everything explicitly.
    editor.repaint();
    status.setText(matches.size() + " issue(s)");
  }

  private void maybeShowPopup(MouseEvent e) {
    if (!e.isPopupTrigger()) {
      return;
    }
    int pos = editor.viewToModel2D(e.getPoint());
    for (RuleMatch match : currentMatches) {
      if (pos >= match.getFromPos() && pos <= match.getToPos()) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem message = new JMenuItem(match.getMessage());
        message.setEnabled(false);
        menu.add(message);
        match.getSuggestedReplacements().stream().limit(MAX_SUGGESTIONS).forEach(suggestion -> {
          JMenuItem item = new JMenuItem(suggestion);
          item.addActionListener(ev -> replace(match, suggestion));
          menu.add(item);
        });
        menu.show(editor, e.getX(), e.getY());
        return;
      }
    }
  }

  private void replace(RuleMatch match, String replacement) {
    try {
      Document doc = editor.getDocument();
      int from = match.getFromPos();
      doc.remove(from, match.getToPos() - from);
      doc.insertString(from, replacement, null);
    }
    catch (BadLocationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /** Paints a zigzag line below the highlighted range. */
  private static class SquigglePainter implements Highlighter.HighlightPainter {
    private static final int WAVE_HEIGHT = 2;
    private static final int WAVE_STEP = 2;
    private final Color color;

    SquigglePainter(Color color) {
      this.color = color;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
      try {
        Rectangle start = c.modelToView2D(p0).getBounds();
        // Backward bias keeps the end position inside the last character's own text run. p1 itself may
        // already belong to a following run with a different font size and thus a different y.
        Rectangle end = c.getUI().modelToView2D(c, p1, Position.Bias.Backward).getBounds();
        boolean sameRow = Utilities.getRowStart(c, p0) == Utilities.getRowStart(c, p1 - 1);
        g.setColor(color);
        // Range wrapping over multiple lines: underline from start to the right edge of the first line
        int endX = sameRow ? end.x : c.getWidth();
        drawWave(g, start.x, endX, start.y + start.height - WAVE_HEIGHT - 1);
      }
      catch (BadLocationException ex) {
        // Highlight outside of the document is not painted
      }
    }

    private void drawWave(Graphics g, int x0, int x1, int y) {
      int[] xs = new int[(x1 - x0) / WAVE_STEP + 2];
      int[] ys = new int[xs.length];
      for (int i = 0; i < xs.length; i++) {
        xs[i] = x0 + i * WAVE_STEP;
        ys[i] = y + (i % 2 == 0 ? 0 : WAVE_HEIGHT);
      }
      g.drawPolyline(xs, ys, xs.length);
    }
  }

  private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
    private final Runnable onChange;

    SimpleDocumentListener(Runnable onChange) {
      this.onChange = onChange;
    }

    @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { }
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new SpellcheckExperiment().setVisible(true));
  }
}
