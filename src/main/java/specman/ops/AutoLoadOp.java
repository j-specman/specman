package specman.ops;

import specman.EditException;
import specman.ScrollPause;
import specman.model.v002.io.ModelParseException;
import specman.settings.SettingAutoLoad;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

import static specman.i18n.Messages.i18n;

public class AutoLoadOp extends AbstractSpecmanOp {

  private final AutoSaveOp autoSave;
  private final LoadDiagrammSpecmanOp loadOp;
  private final WorkingCopyWatcher watcher;

  /** One-shot timer to debounce the change events, e.g. truncate + write of a working copy file before actually loading it */
  private final Timer loadWorkingCopy_Debounced;
  private static final int DEBOUNCE_MS = 500;

  public AutoLoadOp(SpecmanOpContext context, AutoSaveOp autoSave) {
    super(context);
    this.autoSave = autoSave;
    this.loadOp = new LoadDiagrammSpecmanOp(context);
    this.loadWorkingCopy_Debounced = new Timer(DEBOUNCE_MS, e -> loadIfNeeded());
    this.loadWorkingCopy_Debounced.setRepeats(false);
    this.watcher = new WorkingCopyWatcher(() -> SwingUtilities.invokeLater(loadWorkingCopy_Debounced::restart));
  }

  public void workingCopyInitialized(long timestamp) {
    autoSave.setLastSeenWorkingCopyTimestamp(timestamp);
    watchWorkingCopy();
    // Catches changes which happened before the watcher was (re)targeted
    loadWorkingCopy_Debounced.restart();
  }

  /** Schedules a check through the same debounce timer ({@value #DEBOUNCE_MS} ms), which loads the working copy if it is newer
   *  than the last seen state, e.g. after auto load was switched on. The short delay does not matter there. */
  public void checkNow() {
    loadWorkingCopy_Debounced.restart();
  }

  private void watchWorkingCopy() {
    File diagramFile = getDiagrammDatei();
    if (diagramFile == null) {
      return;
    }
    try {
      watcher.watch(AutoSaveOp.workingCopyFor(diagramFile));
    }
    catch (IOException e) {
      if (SettingAutoLoad.isEnabled()) {
        showToast(i18n("msg.autoload.unavailable.brief"),
          i18n("msg.autoload.unavailable.detail", e.getMessage()));
      }
    }
  }

  private void loadIfNeeded() {
    if (!SettingAutoLoad.isEnabled()) {
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
    if (wcTimestamp <= autoSave.getLastSeenWorkingCopyTimestamp()) {
      return;
    }
    autoSave.setLastSeenWorkingCopyTimestamp(wcTimestamp);
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
        autoSave.markCurrentContentAsSaved();
      }
      catch (Exception x) {
        showToast(
          i18n("msg.autoload.corrupt.brief", workingCopy.getName()),
          i18n("msg.autoload.corrupt.detail", x.getMessage()));
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

}
