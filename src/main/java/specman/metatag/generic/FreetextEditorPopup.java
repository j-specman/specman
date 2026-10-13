package specman.metatag.generic;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
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
 * Borderless note-like editor (think of an Excel cell note) for the free text of a meta tag.
 * Closing the popup by any means except Escape - clicking elsewhere, losing focus, Ctrl+Enter -
 * commits the text; Escape discards it. Plain Enter inserts a line break. A fresh instance is
 * meant to be created for every edit session. {@code onClosed} runs after every close, whether
 * the text was committed or discarded.
 */
class FreetextEditorPopup extends JPopupMenu {
  private static final int MIN_ROWS = 4;
  private static final int MAX_ROWS = 12;
  private static final int COLUMNS = 28;
  private static final Color NOTE_BACKGROUND = new Color(255, 255, 225);
  private static final Color NOTE_BORDER = Color.DARK_GRAY;

  private final JTextArea textArea = new JTextArea();
  private boolean cancelled;

  FreetextEditorPopup(Font font, String initialText, Consumer<String> onCommit, Runnable onClosed) {
    textArea.setFont(font);
    textArea.setText(initialText);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setBackground(NOTE_BACKGROUND);
    textArea.setMargin(new Insets(3, 4, 3, 4));

    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setBorder(null);
    scrollPane.setPreferredSize(sizeFittingText());

    setBorder(BorderFactory.createLineBorder(NOTE_BORDER));
    setFocusable(true);
    add(scrollPane);

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
    addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
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

  void select(int selectionStart, int selectionEnd) {
    textArea.select(selectionStart, selectionEnd);
  }

  /** Shows the editor below the invoker, or above it if it doesn't fit below the invoker on the
   * screen but above. Otherwise Swing would push it up as far as necessary, over the invoker. */
  void showNear(JComponent invoker) {
    show(invoker, 0, yRelativeToInvoker(invoker));
    SwingUtilities.invokeLater(() -> {
      textArea.requestFocusInWindow();
      scrollSelectionToVisible();
    });
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

  /** Fixed width of COLUMNS characters; height is whatever the wrapped text needs, but never less
   * than MIN_ROWS and never more than MAX_ROWS lines - beyond that the scroll bar takes over.
   * JTextArea's own row/column metrics are protected, so they are recomputed from the font here. */
  private Dimension sizeFittingText() {
    FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
    Insets insets = textArea.getInsets();
    int width = COLUMNS * metrics.charWidth('m') + insets.left + insets.right;
    int rowHeight = metrics.getHeight();
    int verticalInsets = insets.top + insets.bottom;

    // Wrapping depends on the width, so the component needs it before it can tell its height.
    textArea.setSize(width, Short.MAX_VALUE);
    int contentHeight = textArea.getPreferredSize().height;
    int minHeight = MIN_ROWS * rowHeight + verticalInsets;
    int maxHeight = MAX_ROWS * rowHeight + verticalInsets;
    return new Dimension(width, Math.max(minHeight, Math.min(contentHeight, maxHeight)));
  }

  private void cancel() {
    cancelled = true;
    setVisible(false);
  }

  private void bindKey(KeyStroke keyStroke, String actionName, Runnable action) {
    textArea.getInputMap().put(keyStroke, actionName);
    textArea.getActionMap().put(actionName, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        action.run();
      }
    });
  }
}
