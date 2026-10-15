package experiments.headless;

import specman.EditorI;
import specman.Specman;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelParser_V002;
import specman.undo.manager.SpecmanUndoManager;
import specman.undo.manager.UndoRecording;
import specman.view.AbstractSchrittView;
import specman.view.SchrittSequenzView;

import java.util.Arrays;
import java.util.Map;

public class HeadlessReuseTiming {
  static final int RUNS = 1000;

  static String fixture(int steps, boolean withTable) {
    StringBuilder sb = new StringBuilder();
    sb.append("// {\"specmanVersion\":\"1.3.0\",\"modelType\":\"specman.model.v002.DiagramModel_V002\"}\n");
    sb.append("// Specman: spike\n\nmainSequence {\n");
    for (int i = 1; i <= steps; i++) {
      String id = String.format("%08x", 0xaa000000 + i);
      if (withTable && i == 1) {
        sb.append("    simple(1, " + id + ", `Step with table`, plain=`Step with table`) {\n")
          .append("        table(width=100%) {\n")
          .append("            row { cell(`A1`, plain=`A1`) | cell(`B1`, plain=`B1`) }\n")
          .append("            row { cell(`A2`, plain=`A2`) | cell(`B2`, plain=`B2`) }\n")
          .append("        }\n    }\n\n");
      } else {
        sb.append("    simple(" + i + ", " + id + ", `Step " + i + "`, plain=`Step " + i + "`);\n\n");
      }
    }
    sb.append("}\n");
    return sb.toString();
  }

  /** One complete small test: load, mark first step deleted, undo, redo. Returns diff count of the marking. */
  static int oneTest(Specman stub, String fixture) throws Exception {
    DiagramModel_V002 model = new ModelParser_V002().parse(fixture);
    SchrittSequenzView main = new SchrittSequenzView(null, model.mainSequence);
    main.renummerieren();
    main.viewsNachinitialisieren();
    stub.getUndoManager().discardAllEdits();
    AbstractSchrittView step1 = main.listSteps().get(0);
    Map<String, String> before = HeadlessViewSpike.snapshot(main);
    EditorI editor = Specman.editor();
    try (UndoRecording ur = editor.composeUndo()) {
      step1.alsGeloeschtMarkierenUDBL();
    }
    Map<String, String> marked = HeadlessViewSpike.snapshot(main);
    SpecmanUndoManager undo = stub.getUndoManager();
    undo.undo();
    int undoDiffs = HeadlessViewSpike.diff(before, HeadlessViewSpike.snapshot(main)).size();
    undo.redo();
    int redoDiffs = HeadlessViewSpike.diff(marked, HeadlessViewSpike.snapshot(main)).size();
    if (undoDiffs != 0 || redoDiffs != 0) {
      throw new IllegalStateException("roundtrip diffs: undo=" + undoDiffs + " redo=" + redoDiffs);
    }
    return HeadlessViewSpike.diff(before, marked).size();
  }

  static void measure(String label, boolean reuse, String fixture) throws Exception {
    Specman shared = HeadlessViewSpike.installSpecman();
    // warm-up
    for (int i = 0; i < 100; i++) {
      oneTest(reuse ? shared : HeadlessViewSpike.installSpecman(), fixture);
    }
    double[] ms = new double[RUNS];
    int markedDiffs = -1;
    boolean consistent = true;
    long totalStart = System.nanoTime();
    for (int i = 0; i < RUNS; i++) {
      long t0 = System.nanoTime();
      int diffs = oneTest(reuse ? shared : HeadlessViewSpike.installSpecman(), fixture);
      ms[i] = (System.nanoTime() - t0) / 1e6;
      if (markedDiffs < 0) {
        markedDiffs = diffs;
      }
      consistent &= diffs == markedDiffs;
    }
    double total = (System.nanoTime() - totalStart) / 1e6;
    Arrays.sort(ms);
    System.out.printf("%-34s median %6.2f ms | p95 %6.2f ms | max %6.2f ms | %d tests in %6.0f ms | marked-diffs %d consistent=%b%n",
      label, ms[RUNS / 2], ms[(int) (RUNS * 0.95)], ms[RUNS - 1], RUNS, total, markedDiffs, consistent);
  }

  public static void main(String[] args) throws Exception {
    boolean table = args.length > 0 && args[0].equals("table");
    boolean reuseFirst = args.length > 1 && args[1].equals("reusefirst");
    System.out.println("headless=" + java.awt.GraphicsEnvironment.isHeadless() + ", table=" + table + ", reuseFirst=" + reuseFirst);
    for (int steps : new int[] {2, 30}) {
      String fx = fixture(steps, table);
      if (reuseFirst) {
        measure("reused stub,       " + steps + " steps", true, fx);
        measure("new stub per test, " + steps + " steps", false, fx);
      } else {
        measure("new stub per test, " + steps + " steps", false, fx);
        measure("reused stub,       " + steps + " steps", true, fx);
      }
    }
    System.exit(0);
  }
}
