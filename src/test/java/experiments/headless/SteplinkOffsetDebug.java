package experiments.headless;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import specman.editarea.markups.MarkupType;
import specman.model.ModelEnvelope;
import specman.model.v002.*;

import javax.swing.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Debug: show what doc.getText(from, to-from+1) actually returns for steplink markups
 * when model.text is loaded into a fresh JEditorPane.
 */
public class SteplinkOffsetDebug {

  public static void main(String[] args) throws Exception {
    System.setProperty("java.awt.headless", "true");

    ObjectMapper mapper = new ObjectMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    DiagramModel_V002 model = (DiagramModel_V002) mapper.readValue(
        new File(args.length > 0 ? args[0] : "specman-reference-v1.3-model-v2.nsd"), ModelEnvelope.class).model;

    List<AbstractStepModel_V002> allSteps = new ArrayList<>();
    model.mainSequence.addStepsRecursively(allSteps);

    int shown = 0;
    for (AbstractStepModel_V002 step : allSteps) {
      if (shown >= 5) break;
      shown += debugContent(step.content);
    }
  }

  private static int debugContent(EditorContentModel_V002 content) throws Exception {
    if (content == null) return 0;
    int shown = 0;
    for (AbstractEditAreaModel_V002 area : content.areas) {
      if (!(area instanceof TextEditAreaModel_V002 textArea)) continue;
      if (textArea.markups == null) continue;
      for (Markup_V002 m : textArea.markups) {
        if (m.type != MarkupType.Steplink && m.type != MarkupType.ChangedSteplink) continue;

        // What plainText.substring gives (potentially wrong)
        String fromPlain = textArea.plainText != null && m.to < textArea.plainText.length()
            ? textArea.plainText.substring(m.from, m.to + 1) : "(out of range)";

        // What doc.getText gives (the correct approach)
        JEditorPane ed = new JEditorPane();
        ed.setEditorKit(new HTMLEditorKit());
        ed.setText(textArea.text);
        SwingUtilities.invokeAndWait(() -> {});
        HTMLDocument doc = (HTMLDocument) ed.getDocument();
        String fromDoc = m.to < doc.getLength()
            ? doc.getText(m.from, m.to - m.from + 1) : "(out of range, docLen=" + doc.getLength() + ")";
        int ctxStart = Math.max(0, m.from - 5);
        String docContext = doc.getText(ctxStart, Math.min(20, doc.getLength() - ctxStart));

        System.out.println("from=" + m.from + " to=" + m.to);
        System.out.println("  plainText.substring : [" + fromPlain + "]");
        System.out.println("  doc.getText         : [" + fromDoc + "]");
        System.out.println("  doc context         : [" + docContext + "]");
        System.out.println();
        shown++;
        if (shown >= 5) return shown;
      }
    }
    return shown;
  }
}
