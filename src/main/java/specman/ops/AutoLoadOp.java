package specman.ops;

import specman.EditException;
import specman.ScrollPause;
import specman.model.v002.io.ModelParseException;
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

  /** Loads the working copy if possible and restores the last state if not.
  /** Loads the working copy if possible. On IOException/ModelParseException the UI was
   *  not touched, so only tracking state is restored (no visual rebuild, no flicker).
   *  On other exceptions the last UI state is fully restored from snapshot. */
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
        showToast(
          workingCopy.getName() + " konnte nicht geladen werden.",
          "Die Arbeitskopie scheint defekt zu sein:\n\n" + x.getMessage() +
          "\n\nDer zuletzt geladene Stand wird beibehalten.");
        recoverFromFailedLoad(snapshot, diagramFile, x);
        setDiagrammDatei(diagramFile);
        markAsUnsavedWorkingCopy();
      }
    }
    catch (Exception e) {
      displayException(e);
    }
  }

  /** Restores a consistent UI state after a failed working-copy load if necessary.
   * {@link IOException} and {@link ModelParseException} occur before any UI update —
   * the working copy could not be read or parsed at all, so restoring from the
   * snapshot would rebuild the UI unnecessarily and cause visible flicker.
   * For any other exception the UI may already be partially updated, so a full
   * snapshot restore is required to get back to a clean state. */
  private void recoverFromFailedLoad(byte[] snapshot, File diagramFile, Exception cause)
      throws EditException, IOException {
    if (!(cause instanceof IOException || cause instanceof ModelParseException)) {
      loadOp.loadOrThrow(snapshot);
    }
  }

  private byte[] takeSnapshot() throws IOException {
    return autoSave.generateSnapshot();
  }

  private static int timerDelay() {
    Integer intervalSeconds = SettingAutoLoad.getIntervalSeconds();
    return intervalSeconds != null ? intervalSeconds * 1000 : AutoSaveOp.OFF_CHECK_INTERVAL_MS;
  }

}
