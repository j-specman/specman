package experiments.jeditorpane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Experiment: Can a plain JEditorPane (no SHEF, no Specman context) parse HTML
 * and allow reliable document-level text manipulation in java.awt.headless=true mode?
 *
 * This is the prerequisite check for ModelStepnumberRewriter. If the replacement
 * and round-trip work correctly here, we can use this approach to rewrite Steplink
 * text in TextEditAreaModel_V002 without Swing views.
 *
 * Run via main() and observe output manually.
 */
public class HeadlessEditorPaneExperiment {

  // Same color as ChangeSet.STEPNUMBER_LINK_COLOR
  static final Color STEPLINK_COLOR = new Color(220, 220, 220);

  public static void main(String[] args) throws Exception {
    // MUST be set before any AWT class is touched
    System.setProperty("java.awt.headless", "true");

    System.out.println("=== Phase 1: Build document with styled steplink ===");
    JEditorPane editor = new JEditorPane();
    editor.setEditorKit(new HTMLEditorKit());
    editor.setText("<html><body><p>Weiter bei Schritt 3 danach.</p></body></html>");

    // Flush the EDT so setText() is fully processed
    SwingUtilities.invokeAndWait(() -> {});

    HTMLDocument doc = (HTMLDocument) editor.getDocument();
    String plainTextBefore = doc.getText(0, doc.getLength());
    System.out.println("plainText: [" + plainTextBefore.trim() + "]");

    int stepPos = plainTextBefore.indexOf("3");
    System.out.println("Position of '3': " + stepPos);

    // Apply steplink background color (simulates what Specman stores in markups)
    SimpleAttributeSet stepLinkStyle = new SimpleAttributeSet();
    StyleConstants.setBackground(stepLinkStyle, STEPLINK_COLOR);
    doc.setCharacterAttributes(stepPos, 1, stepLinkStyle, false);

    String htmlBefore = serialize(doc);
    System.out.println("\nHTML after styling:\n" + htmlBefore);

    System.out.println("\n=== Phase 2: Replace '3' with '7' at document position ===");
    // Get attributes BEFORE remove to preserve the steplink background
    AttributeSet stepLinkAttrs = doc.getCharacterElement(stepPos).getAttributes();
    doc.remove(stepPos, 1);
    doc.insertString(stepPos, "7", stepLinkAttrs);

    // Flush EDT again after document modification
    SwingUtilities.invokeAndWait(() -> {});

    String plainTextAfter = doc.getText(0, doc.getLength());
    String htmlAfter = serialize(doc);
    System.out.println("plainText after: [" + plainTextAfter.trim() + "]");
    System.out.println("\nHTML after replacement:\n" + htmlAfter);

    boolean replacedInPlain = plainTextAfter.contains("7") && !plainTextAfter.contains("3");
    System.out.println("Replacement in plainText: " + (replacedInPlain ? "OK" : "FAILED"));

    // Note: background-color for steplinks is NOT serialized into the HTML by HTMLEditorKit —
    // it is stored separately in markups and re-applied at load time by MarkupBackgroundStyleInitializer.
    // So we only check that the number text was replaced correctly in the HTML.
    boolean replacedInHtml = htmlAfter.contains("7") && !htmlAfter.contains(">3<") && !htmlAfter.contains("Schritt 3");
    System.out.println("Replacement in HTML:      " + (replacedInHtml ? "OK" : "FAILED"));
    System.out.println("(Background-color in HTML: " + (htmlAfter.contains("background-color") ? "YES" : "NO — expected: stored in markups only"));

    System.out.println("\n=== Phase 3: Round-trip — load serialized HTML back ===");
    JEditorPane editor2 = new JEditorPane();
    editor2.setEditorKit(new HTMLEditorKit());
    editor2.setText(htmlAfter);
    SwingUtilities.invokeAndWait(() -> {});

    HTMLDocument doc2 = (HTMLDocument) editor2.getDocument();
    String roundTripPlain = doc2.getText(0, doc2.getLength());
    System.out.println("Round-trip plainText: [" + roundTripPlain.trim() + "]");
    boolean roundTripOk = roundTripPlain.trim().equals(plainTextAfter.trim());
    System.out.println("Round-trip plainText: " + (roundTripOk ? "OK" : "FAILED"));

    // Verify steplink background color is NOT in the HTML round-trip
    // (expected: background-color is stored in markups, not in text/HTML)
    int newStepPos = roundTripPlain.indexOf("7");
    if (newStepPos >= 0) {
      Element elem = doc2.getCharacterElement(newStepPos);
      Color bg = StyleConstants.getBackground(elem.getAttributes());
      boolean colorAbsent = bg == null || bg.equals(Color.BLACK) || bg.equals(new Color(0,0,0));
      System.out.println("Background-color absent from round-trip (expected): " + (colorAbsent ? "OK — confirmed not in HTML" : "UNEXPECTED: " + bg));
    }

    System.out.println("\n=== Phase 4: Round-trip fidelity with real Specman HTML ===");
    System.out.println("(Only TextEditAreas WITH changed steplinks will be touched — others stay untouched.)");
    List<String> specmanTexts = extractTextFields(new File("UC791-Specman-1.3.nsd"));
    System.out.println("Text fields found: " + specmanTexts.size());
    int identical = 0, different = 0;
    for (String originalHtml : specmanTexts) {
      JEditorPane rtEditor = new JEditorPane();
      rtEditor.setEditorKit(new HTMLEditorKit());
      rtEditor.setText(originalHtml);
      SwingUtilities.invokeAndWait(() -> {});
      String reserialized = serialize((HTMLDocument) rtEditor.getDocument());
      if (reserialized.equals(originalHtml)) {
        identical++;
      } else {
        different++;
        if (different <= 4) {
          System.out.println("Difference #" + different + ":");
          // Find the first differing character
          int diffAt = 0;
          while (diffAt < Math.min(originalHtml.length(), reserialized.length())
              && originalHtml.charAt(diffAt) == reserialized.charAt(diffAt)) {
            diffAt++;
          }
          int ctx = Math.max(0, diffAt - 30);
          System.out.println("  First diff at char " + diffAt + ":");
          System.out.println("  original   : [" + originalHtml.substring(ctx, Math.min(originalHtml.length(), ctx + 80)) + "]");
          System.out.println("  reserialized: [" + reserialized.substring(ctx, Math.min(reserialized.length(), ctx + 80)) + "]");
        }
      }
    }
    System.out.println("Identical: " + identical + " / " + specmanTexts.size());
    System.out.println("Different: " + different + " / " + specmanTexts.size());
    System.out.println("Round-trip fidelity: " + (different == 0 ? "PERFECT" : "IMPERFECT — only reserialize fields where steplinks actually changed"));

    System.out.println("\n=== SUMMARY ===");
    System.out.println(replacedInPlain && replacedInHtml && roundTripOk
        ? "ALL OK — JEditorPane headless manipulation works.\n" +
          "Insight: steplink background-color is NOT in the HTML text field;\n" +
          "only the number text needs to be rewritten. Markups carry the color."
        : "FAILURES detected — consider falling back to jsoup");
  }

  /** Extracts all text fields from a Specman .nsd/.wrk JSON file via Jackson. */
  private static List<String> extractTextFields(File file) throws Exception {
    List<String> result = new ArrayList<>();
    JsonNode root = new ObjectMapper().readTree(file);
    collectTextFields(root, result);
    return result;
  }

  private static void collectTextFields(JsonNode node, List<String> result) {
    if (node.isObject()) {
      JsonNode text = node.get("text");
      JsonNode plainText = node.get("plainText");
      if (text != null && plainText != null && text.isTextual()) {
        result.add(text.asText());
      }
      node.fields().forEachRemaining(e -> collectTextFields(e.getValue(), result));
    } else if (node.isArray()) {
      node.forEach(child -> collectTextFields(child, result));
    }
  }

  private static String serialize(HTMLDocument doc) throws Exception {
    StringWriter sw = new StringWriter();
    new HTMLEditorKit().write(sw, doc, 0, doc.getLength());
    return sw.toString();
  }
}
