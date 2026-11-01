package specman.util;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * Borderless note-like editor (think of an Excel cell note) for short texts, e.g. the free text of a meta tag.
 * Closing the popup by any means except Escape - clicking elsewhere, losing focus, Ctrl+Enter -
 * commits the text; Escape discards it. Plain Enter inserts a line break. A fresh instance is
 * meant to be created for every edit session. {@code onClosed} runs after every close, whether
 * the text was committed or discarded.
 * <p>
 * Subclasses can refuse a text ({@link #canCommit()}): the editor then reappears right away when Swing closes
 * it, and can only be left by Escape. They can also put a component below the text
 * ({@link #setBelowText(JComponent)}) and react to typing ({@link #textChanged()}).
 */
public class NoteEditorPopup extends JPopupMenu {
  private static final int DEFAULT_MIN_ROWS = 4;
  private static final int DEFAULT_MAX_ROWS = 12;
  private static final int DEFAULT_COLUMNS = 28;
  protected static final Color NOTE_BACKGROUND = new Color(255, 255, 225);
  private static final Color NOTE_BORDER = Color.DARK_GRAY;

  private final JTextArea textArea = new JTextArea();
  private final JScrollPane scrollPane = new JScrollPane(textArea);
  private final JPanel contentPanel = new JPanel(new BorderLayout());
  private int columns = DEFAULT_COLUMNS;
  private int minRows = DEFAULT_MIN_ROWS;
  private int maxRows = DEFAULT_MAX_ROWS;
  private int textWidth;
  private boolean cancelled;
  private JComponent shownAtInvoker;
  private int shownAtX;
  private int shownAtY;

  public NoteEditorPopup(Font font, String initialText, Consumer<String> onCommit, Runnable onClosed) {
    textArea.setFont(font);
    textArea.setText(initialText);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setBackground(NOTE_BACKGROUND);
    textArea.setMargin(new Insets(3, 4, 3, 4));

    scrollPane.setBorder(null);
    contentPanel.setOpaque(false);
    contentPanel.add(scrollPane, BorderLayout.CENTER);

    setBorder(BorderFactory.createLineBorder(NOTE_BORDER));
    setFocusable(true);
    add(contentPanel);

    bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel", this::cancel);
    bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "commit", () -> setVisible(false));

    textArea.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        if (isVisible()) {
          setVisible(false);
        }
      }
    });
    textArea.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { textChanged(); }
      @Override public void removeUpdate(DocumentEvent e) { textChanged(); }
      @Override public void changedUpdate(DocumentEvent e) {}
    });
    addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if (!cancelled && !canCommit()) {
          SwingUtilities.invokeLater(NoteEditorPopup.this::reappear);
          return;
        }
        if (!cancelled) {
          onCommit.accept(textArea.getText());
        }
        onClosed.run();
      }

      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
      }
    });
  }

  /** Fixed width of the given number of characters; height is whatever the wrapped text needs, but
   * never less than minRows and never more than maxRows lines - beyond that the scroll bar takes over.
   * For subclasses which don't want the defaults. */
  protected void setTextSize(int columns, int minRows, int maxRows) {
    this.columns = columns;
    this.minRows = minRows;
    this.maxRows = maxRows;
  }

  public void select(int selectionStart, int selectionEnd) {
    textArea.select(selectionStart, selectionEnd);
  }

  /** Shows the editor below the invoker, or above it if it doesn't fit below the invoker on the
   * screen but above. Otherwise Swing would push it up as far as necessary, over the invoker. */
  public void showNear(JComponent invoker) {
    prepareSize();
    showAt(invoker, 0, yRelativeToInvoker(invoker));
  }

  /** Shows the editor at the right edge of the invoker, moved to the left by the given inset, and top aligned
   * with the invoker, so that it stays out of the way if the invoker changes its height while the user types.
   * If there is no room on the screen to the right of the invoker, Swing pushes the popup to the left as far
   * as necessary. */
  public void showRightOf(JComponent invoker, int inset) {
    prepareSize();
    showAt(invoker, invoker.getWidth() - inset, 0);
  }

  /** Whether the text may be committed when the editor is left. Without it, the editor stays open. */
  protected boolean canCommit() {
    return true;
  }

  /** Called whenever the user changes the text. */
  protected void textChanged() {
  }

  /** Called before the editor is shown, also when it reappears because the text can't be committed. The text
   * width is known by then, see {@link #textWidth()}. */
  protected void beforeShow() {
  }

  protected String text() {
    return textArea.getText();
  }

  /** @return the width of the text area in pixels, e.g. to wrap a message below the text to the same width */
  protected int textWidth() {
    return textWidth;
  }

  /** Puts a component below the text, spanning the editor's whole width. It should not be wider than
   * {@link #textWidth()}, as it would widen the editor otherwise. */
  protected void setBelowText(JComponent component) {
    contentPanel.add(component, BorderLayout.SOUTH);
  }

  protected void bindKey(KeyStroke keyStroke, String actionName, Runnable action) {
    textArea.getInputMap().put(keyStroke, actionName);
    textArea.getActionMap().put(actionName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        action.run();
      }
    });
  }

  private void prepareSize() {
    scrollPane.setPreferredSize(sizeFittingText());
    beforeShow();
  }

  private void showAt(JComponent invoker, int x, int y) {
    shownAtInvoker = invoker;
    shownAtX = x;
    shownAtY = y;
    show(invoker, x, y);
    SwingUtilities.invokeLater(() -> {
      textArea.requestFocusInWindow();
      scrollSelectionToVisible();
    });
  }

  /** Swing closed the editor although the text can't be committed, e.g. by a click elsewhere. The editor comes
   * back - the flicker tells the user that leaving is not possible right now. */
  private void reappear() {
    beforeShow();
    showAt(shownAtInvoker, shownAtX, shownAtY);
  }

  /** A selected part of a long text, e.g. a search hit, may be outside of the visible part of the
   * text. Only works once the popup has been laid out, as the position depends on the line wrapping. */
  private void scrollSelectionToVisible() {
    if (textArea.getSelectionStart() == textArea.getSelectionEnd()) {
      return;
    }
    try {
      Rectangle selectionStart = textArea.modelToView2D(textArea.getSelectionStart()).getBounds();
      Rectangle selectionEnd = textArea.modelToView2D(textArea.getSelectionEnd()).getBounds();
      textArea.scrollRectToVisible(selectionStart.union(selectionEnd));
    } catch (BadLocationException e) {
      // The selection was just set by select(), so its positions are valid
      throw new IllegalStateException(e);
    }
  }

  private int yRelativeToInvoker(JComponent invoker) {
    int popupHeight = getPreferredSize().height;
    Rectangle usableScreen = usableScreenArea(invoker);
    int invokerTop = invoker.getLocationOnScreen().y;
    int spaceBelow = usableScreen.y + usableScreen.height - (invokerTop + invoker.getHeight());
    int spaceAbove = invokerTop - usableScreen.y;
    boolean showAbove = popupHeight > spaceBelow && popupHeight <= spaceAbove;
    return showAbove ? -popupHeight : invoker.getHeight();
  }

  /** The screen of the invoker without taskbars and the like. */
  private static Rectangle usableScreenArea(JComponent invoker) {
    GraphicsConfiguration screen = invoker.getGraphicsConfiguration();
    Rectangle bounds = screen.getBounds();
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
    return new Rectangle(
      bounds.x + insets.left,
      bounds.y + insets.top,
      bounds.width - insets.left - insets.right,
      bounds.height - insets.top - insets.bottom);
  }

  /** JTextArea's own row/column metrics are protected, so they are recomputed from the font here. */
  private Dimension sizeFittingText() {
    FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
    Insets insets = textArea.getInsets();
    int width = columns * metrics.charWidth('m') + insets.left + insets.right;
    textWidth = width;
    int rowHeight = metrics.getHeight();
    int verticalInsets = insets.top + insets.bottom;

    // Wrapping depends on the width, so the component needs it before it can tell its height.
    textArea.setSize(width, Short.MAX_VALUE);
    int contentHeight = textArea.getPreferredSize().height;
    int minHeight = minRows * rowHeight + verticalInsets;
    int maxHeight = maxRows * rowHeight + verticalInsets;
    return new Dimension(width, Math.max(minHeight, Math.min(contentHeight, maxHeight)));
  }

  private void cancel() {
    cancelled = true;
    setVisible(false);
  }
}
