package experiments.headless;

import specman.Specman;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.io.ModelParser_V002;
import specman.view.SchrittSequenzView;

public class HeadlessStartupTiming {
  public static void main(String[] args) throws Exception {
    boolean plain = args.length > 0 && args[0].equals("plain");
    int runs = 15;
    System.out.println("headless=" + java.awt.GraphicsEnvironment.isHeadless() + ", fixture=" + (plain ? "plain" : "with table"));
    for (int i = 0; i < runs; i++) {
      long t0 = System.nanoTime();
      Specman stub = HeadlessViewSpike.installSpecman();
      long t1 = System.nanoTime();
      DiagramModel_V002 model = new ModelParser_V002().parse(plain ? HeadlessViewSpike.FIXTURE_PLAIN : HeadlessViewSpike.FIXTURE);
      long t2 = System.nanoTime();
      SchrittSequenzView main = new SchrittSequenzView(null, model.mainSequence);
      main.renummerieren();
      main.viewsNachinitialisieren();
      stub.getUndoManager().discardAllEdits();
      long t3 = System.nanoTime();
      System.out.printf("run %2d: stub %6.1f ms | parse %6.1f ms | views %6.1f ms | total %6.1f ms%n",
        i + 1, (t1 - t0) / 1e6, (t2 - t1) / 1e6, (t3 - t2) / 1e6, (t3 - t0) / 1e6);
    }
    System.exit(0);
  }
}
