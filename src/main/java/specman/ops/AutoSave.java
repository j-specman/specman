package specman.ops;

import specman.ScrollPause;
import specman.settings.SettingAutoSave;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class AutoSave extends AbstractSpecmanOp {

  public static final String WORKING_COPY_EXTENSION = ".wrk";
  private static final int INTERVAL_MS = 5 * 1000;
  //private static final int INTERVAL_MS = 5 * 60 * 1000;

  private final SaveDiagrammSpecmanOp saveOp;

  public AutoSave(SpecmanOpContext context) {
    super(context);
    saveOp = new SaveDiagrammSpecmanOp(context);
    Timer timer = new Timer(INTERVAL_MS, e -> saveIfNeeded());
    timer.start();
  }

  private void saveIfNeeded() {
    if (!SettingAutoSave.isSet()) {
      return;
    }
    if (!hasUnsavedChanges()) {
      return;
    }
    File diagramFile = getDiagrammDatei();
    if (diagramFile == null) {
      return;
    }
    try (UndoRecording ur = pauseUndo();
         ScrollPause sp = pauseScrolling()) {
      saveOp.saveToFile(workingCopyFor(diagramFile));
    }
    catch (IOException e) {
      displayException(e);
    }
  }

  public static File workingCopyFor(File nsdFile) {
    return new File(nsdFile.getAbsolutePath() + WORKING_COPY_EXTENSION);
  }

  public static void deleteWorkingCopyFor(File nsdFile) {
    if (nsdFile != null) {
      workingCopyFor(nsdFile).delete();
    }
  }

  public static boolean workingCopyExistsFor(File nsdFile) {
    return nsdFile != null && workingCopyFor(nsdFile).exists();
  }

}
