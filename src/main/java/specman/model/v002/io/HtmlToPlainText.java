package specman.model.v002.io;

import specman.editarea.document.WrappedDocument;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLEditorKit;

/** Converting HTML to plain text is based on headless JEditorPanes to ensure that it works exactly
 * identical in both UI and headless sanitizer. */
public class HtmlToPlainText {
    static final HTMLEditorKit HTML_EDITOR_KIT = new HTMLEditorKit();

    static String convert(String html) {
        JEditorPane ed = fromHtml(html);
        return new WrappedDocument((StyledDocument) ed.getDocument()).getVisibleText();
    }

    public static JEditorPane fromHtml(String html) {
        JEditorPane ed = new JEditorPane();
        ed.setEditorKit(HTML_EDITOR_KIT);
        ed.setText(html);
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(() -> {});
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return ed;
    }
}
