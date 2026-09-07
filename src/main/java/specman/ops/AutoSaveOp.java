package specman.ops;

import specman.ScrollPause;
import specman.settings.SettingAutoSave;
import specman.undo.manager.UndoRecording;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class AutoSaveOp extends AbstractSpecmanOp {

  public static final String WORKING_COPY_EXTENSION = ".wrk";
  static final int OFF_CHECK_INTERVAL_MS = 60_000;

  private final SaveDiagrammSpecmanOp saveOp;
  private final Timer timer;
  private long lastSaveTime = 0;
  private int lastSavedContentHash = 0;

  public AutoSaveOp(SpecmanOpContext context) {
    super(context);
    saveOp = new SaveDiagrammSpecmanOp(context);
    timer = new Timer(timerDelay(), e -> saveIfNeeded());
    timer.start();
  }

  private void saveIfNeeded() {
    timer.setDelay(timerDelay());
    Integer intervalSeconds = SettingAutoSave.getIntervalSeconds();
    if (intervalSeconds == null || !hasUnsavedChanges() || getDiagrammDatei() == null) {
      return;
    }
    File wc = workingCopyFor(getDiagrammDatei());
    try (UndoRecording ur = pauseUndo(); ScrollPause sp = pauseScrolling()) {
      byte[] content = saveOp.generateBytes();
      int contentHash = Arrays.hashCode(content);
      if (contentHash == lastSavedContentHash) {
        return;
      }
      saveOp.writeToFile(wc, content);
      lastSavedContentHash = contentHash;
      lastSaveTime = wc.lastModified();
    }
    catch (IOException e) {
      displayException(e);
    }
  }

  public long getLastSaveTime() { return lastSaveTime; }

  public byte[] generateSnapshot() throws IOException {
    return saveOp.generateBytes();
  }

  private static int timerDelay() {
    Integer intervalSeconds = SettingAutoSave.getIntervalSeconds();
    return intervalSeconds != null ? intervalSeconds * 1000 : OFF_CHECK_INTERVAL_MS;
  }

  public static File workingCopyFor(File nsdFile) {
    return new File(nsdFile.getAbsolutePath() + WORKING_COPY_EXTENSION);
  }

  public static void deleteWorkingCopyFor(File nsdFile) {
    if (nsdFile != null) {
      workingCopyFor(nsdFile).delete();
    }
  }

  public static long createWorkingCopyFor(File nsdFile) {
    if (nsdFile == null) {
      return 0;
    }
    try {
      File wc = workingCopyFor(nsdFile);
      Files.copy(nsdFile.toPath(), wc.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return wc.lastModified();
    }
    catch (IOException ignored) {
      // Best-effort: AutoSave will create the working copy on the next save cycle
      return 0;
    }
  }

  public static boolean workingCopyExistsFor(File nsdFile) {
    return nsdFile != null && workingCopyFor(nsdFile).exists();
  }

}
