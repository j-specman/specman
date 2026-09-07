package specman.ops;

import specman.EditException;
import specman.ScrollPause;
import specman.settings.SettingAutoLoad;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class AutoLoadOp extends AbstractSpecmanOp {

  private final AutoSaveOp autoSave;
  private final LoadDiagrammSpecmanOp loadOp;
  private final Timer timer;
  private long lastLoadedFileTimestamp = 0;

  public AutoLoadOp(SpecmanOpContext context, AutoSaveOp autoSave) {
    super(context);
    this.autoSave = autoSave;
    loadOp = new LoadDiagrammSpecmanOp(context);
    timer = new Timer(timerDelay(), e -> loadIfNeeded());
    timer.start();
  }

  public void workingCopyInitialized(long timestamp) {
    lastLoadedFileTimestamp = timestamp;
  }

  private void loadIfNeeded() {
    timer.setDelay(timerDelay());
    if (SettingAutoLoad.getIntervalSeconds() == null) {
      return;
    }
    File diagramFile = getDiagrammDatei();
    if (diagramFile == null) {
      return;
    }
    if (!AutoSaveOp.workingCopyExistsFor(diagramFile)) {
      return;
    }
    File workingCopy = AutoSaveOp.workingCopyFor(diagramFile);
    long wcTimestamp = workingCopy.lastModified();
    if (wcTimestamp <= Math.max(lastLoadedFileTimestamp, autoSave.getLastSaveTime())) {
      return;
    }
    lastLoadedFileTimestamp = wcTimestamp;

    loadWorkingCopy(workingCopy, diagramFile);
  }

  /** Loads the working copy if possible and restores the last state if not. */
  private void loadWorkingCopy(File workingCopy, File diagramFile) {
    try (ScrollPause sp = pauseScrolling();
         UndoRecording ur = pauseUndo()) {
      byte[] snapshot = takeSnapshot();
      try {
        loadOp.loadOrThrow(workingCopy);
        setDiagrammDatei(diagramFile);
        markAsUnsavedWorkingCopy();
      }
      catch (Exception x) {
        showMessage(
          "Die Arbeitskopie '" + workingCopy.getName() + "' konnte nicht geladen werden und scheint defekt zu sein:\n\n" +
            x.getMessage() + "\n\n" +
            "Der zuletzt geladene Stand wird beibehalten.");
        restoreFromSnapshot(snapshot, diagramFile);
      }
    }
    catch (Exception e) {
      displayException(e);
    }
  }

  private byte[] takeSnapshot() throws IOException {
    return autoSave.generateSnapshot();
  }

  private void restoreFromSnapshot(byte[] snapshot, File diagramFile) throws EditException, IOException {
    loadOp.loadOrThrow(snapshot);
    setDiagrammDatei(diagramFile);
    markAsUnsavedWorkingCopy();
  }

  private static int timerDelay() {
    Integer intervalSeconds = SettingAutoLoad.getIntervalSeconds();
    return intervalSeconds != null ? intervalSeconds * 1000 : AutoSaveOp.OFF_CHECK_INTERVAL_MS;
  }

}
