package specman.ops;

import specman.ScrollPause;
import specman.settings.SettingAutoLoad;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import java.io.File;

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
    try (ScrollPause sp = pauseScrolling();
         UndoRecording ur = pauseUndo()) {
      loadOp.loadOrThrow(workingCopy);
      setDiagrammDatei(diagramFile);
      lastLoadedFileTimestamp = wcTimestamp;
      markAsUnsavedWorkingCopy();
    }
    catch (Exception e) {
      lastLoadedFileTimestamp = wcTimestamp;
      showMessage(
          "Die Arbeitskopie '" + workingCopy.getName() + "' konnte nicht geladen werden und scheint defekt zu sein.\n" +
          "Das Originalmodell wird wiederhergestellt.");
      try (ScrollPause sp = pauseScrolling();
           UndoRecording ur = pauseUndo()) {
        loadOp.loadOrThrow(diagramFile);
      }
      catch (Exception ex) {
        displayException(ex);
      }
    }
  }

  private static int timerDelay() {
    Integer intervalSeconds = SettingAutoLoad.getIntervalSeconds();
    return intervalSeconds != null ? intervalSeconds * 1000 : AutoSaveOp.OFF_CHECK_INTERVAL_MS;
  }

}
