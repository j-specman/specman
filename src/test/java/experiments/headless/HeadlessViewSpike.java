package experiments.headless;

import specman.EditorI;
import specman.Specman;
import specman.editarea.EditArea;
import specman.editarea.TextEditArea;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelParser_V002;
import specman.undo.manager.SpecmanUndoManager;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Throwaway spike: can Specman's real view layer be built and operated headless?
 * Run with -Djava.awt.headless=true. NOT a test, NOT part of the suite (has a main only).
 *
 * The result is: yes, with the real, unmodified Specman. Specman is a JRootPane, so it can be
 * instantiated without its SpecmanFrame. No stub subclass, no Unsafe, no reflection: the real
 * toolbars, menus, HTMLEditorPane with all its listeners, drag adapters and the real undo manager
 * are in place. Only the Specman constructor is called.
 */
public class HeadlessViewSpike {

  static Specman installSpecman() throws Exception {
    Specman specman = new Specman(null);
    specman.setChangeModeEnabled(true);
    return specman;
  }


  // ---- structural snapshot (properties only, no geometry) ----

  static Map<String, String> snapshot(SchrittSequenzView main) {
    Map<String, String> snap = new LinkedHashMap<>();
    for (AbstractSchrittView step : main.listSteps()) {
      snap.put("step " + step.getNumber(), "changeInfo=" + step.getChangeInfo());
    }
    walk(main.getContainer(), "root", snap);
    return snap;
  }

  private static void walk(Component c, String path, Map<String, String> snap) {
    StringBuilder sb = new StringBuilder();
    sb.append(c.getClass().getSimpleName());
    sb.append(" bg=").append(colorName(c.getBackground()));
    sb.append(" fg=").append(colorName(c.getForeground()));
    if (c instanceof javax.swing.JComponent jc) {
      sb.append(" border=").append(borderName(jc.getBorder()));
    }
    if (c instanceof JLabel l) {
      sb.append(" text=").append(l.getText());
    }
    if (c instanceof EditArea ea) {
      sb.append(" changeInfo=").append(ea.getChangeInfo());
    }
    if (c instanceof TextEditArea t) {
      sb.append(" html=").append(t.getText().replaceAll("\\s+", " "));
    }
    snap.put(path, sb.toString());
    if (c instanceof Container cont) {
      Component[] children = cont.getComponents();
      for (int i = 0; i < children.length; i++) {
        walk(children[i], path + "/" + i, snap);
      }
    }
  }

  private static String colorName(Color c) {
    return c == null ? "null" : String.format("#%06x", c.getRGB() & 0xffffff);
  }

  private static String borderName(Border b) {
    if (b == null) { return "null"; }
    if (b instanceof MatteBorder m) {
      return "Matte(" + colorName(m.getMatteColor()) + "," + m.getBorderInsets() + ")";
    }
    return b.getClass().getSimpleName();
  }

  static List<String> diff(Map<String, String> before, Map<String, String> after) {
    List<String> diffs = new ArrayList<>();
    for (String key : before.keySet()) {
      if (!after.containsKey(key)) {
        diffs.add("missing after: " + key);
      }
      else if (!before.get(key).equals(after.get(key))) {
        diffs.add(key + "\n    before: " + before.get(key) + "\n    after:  " + after.get(key));
      }
    }
    for (String key : after.keySet()) {
      if (!before.containsKey(key)) {
        diffs.add("new after: " + key);
      }
    }
    return diffs;
  }

  static final String FIXTURE = String.join("\n",
    "// {\"specmanVersion\":\"1.3.0\",\"modelType\":\"specman.model.v002.DiagramModel_V002\"}",
    "// Specman: spike",
    "",
    "intro(`Intro`, plain=`Intro`) {",
    "}",
    "",
    "mainSequence {",
    "    simple(1, aabbccdd, `Step with table`, plain=`Step with table`) {",
    "        table(width=100%) {",
    "            row { cell(`A1`, plain=`A1`) | cell(`B1`, plain=`B1`) }",
    "            row { cell(`A2`, plain=`A2`) | cell(`B2`, plain=`B2`) }",
    "        }",
    "    }",
    "",
    "    simple(2, aabbccee, `Plain step`, plain=`Plain step`);",
    "}",
    "",
    "outro(`Outro`, plain=`Outro`) {",
    "}",
    "");

  static final String FIXTURE_PLAIN = String.join("\n",
    "// {\"specmanVersion\":\"1.3.0\",\"modelType\":\"specman.model.v002.DiagramModel_V002\"}",
    "// Specman: spike",
    "",
    "mainSequence {",
    "    simple(1, aabbccdd, `Step one`, plain=`Step one`);",
    "",
    "    simple(2, aabbccee, `Plain step`, plain=`Plain step`);",
    "}",
    "");

  public static void main(String[] args) throws Exception {
    // java.awt.headless is controlled from the command line (-Djava.awt.headless=true|false)
    System.out.println("headless=" + java.awt.GraphicsEnvironment.isHeadless());

    Specman specman = installSpecman();
    System.out.println("[1] real Specman instantiated headless, no stub: " + (Specman.editor() == specman));

    boolean plain = args.length > 0 && args[0].equals("plain");
    DiagramModel_V002 model = new ModelParser_V002().parse(plain ? FIXTURE_PLAIN : FIXTURE);
    System.out.println("[2] fixture parsed, steps=" + model.queryAllSteps().size());

    SchrittSequenzView main = new SchrittSequenzView(null, model.mainSequence);
    main.renummerieren();
    main.viewsNachinitialisieren();
    specman.getUndoManager().discardAllEdits(); // like LoadDiagrammSpecmanOp.applyEnvelope
    System.out.println("[3] views built, steps=" + main.listSteps().size());

    AbstractSchrittView step1 = main.listSteps().get(0);
    Map<String, String> before = snapshot(main);
    System.out.println("[4] snapshot taken, entries=" + before.size());

    EditorI editor = Specman.editor();
    try (UndoRecording ur = editor.composeUndo()) {
      step1.alsGeloeschtMarkierenUDBL();
    }
    Map<String, String> marked = snapshot(main);
    System.out.println("[5] step marked deleted, snapshot differs from before: " + !diff(before, marked).isEmpty()
      + " (" + diff(before, marked).size() + " diffs)");

    SpecmanUndoManager undo = specman.getUndoManager();
    System.out.println("    canUndo=" + undo.canUndo());
    undo.undo();
    Map<String, String> afterUndo = snapshot(main);
    List<String> undoDiffs = diff(before, afterUndo);
    System.out.println("[6] undo done, residual diffs vs. before: " + undoDiffs.size());
    undoDiffs.forEach(d -> System.out.println("  - " + d));

    undo.redo();
    List<String> redoDiffs = diff(marked, snapshot(main));
    System.out.println("[7] redo done, residual diffs vs. marked: " + redoDiffs.size());
    redoDiffs.forEach(d -> System.out.println("  - " + d));
    System.exit(0);
  }
}
