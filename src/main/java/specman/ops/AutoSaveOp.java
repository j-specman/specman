package specman.ops;

import specman.ScrollPause;
import specman.settings.SettingAutoLoad;
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
  private long lastSeenWorkingCopyTimestamp = 0;
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
    if (isNewerThanLastSeen(wc)) {
      return;
    }
    try (UndoRecording ur = pauseUndo(); ScrollPause sp = pauseScrolling()) {
      byte[] content = saveOp.generateBytes();
      int contentHash = Arrays.hashCode(content);
      if (contentHash == lastSavedContentHash) {
        return;
      }
      saveOp.writeToFile(wc, content);
      lastSavedContentHash = contentHash;
      lastSeenWorkingCopyTimestamp = wc.lastModified();
    }
    catch (IOException e) {
      displayException(e);
    }
  }

  /** With auto-load on, a working copy which changed since Specman last saw it (e.g. written by an
   * AI agent) is still waiting to be loaded. Writing the UI state now would overwrite it. */
  private boolean isNewerThanLastSeen(File wc) {
    return SettingAutoLoad.isEnabled() && wc.lastModified() > lastSeenWorkingCopyTimestamp;
  }

  public long getLastSeenWorkingCopyTimestamp() { return lastSeenWorkingCopyTimestamp; }

  public void setLastSeenWorkingCopyTimestamp(long timestamp) { lastSeenWorkingCopyTimestamp = timestamp; }

  /** Called after the UI state was replaced by the working copy's content, so that the next
   * auto-save doesn't write the same content back and thereby touch the file again. */
  public void markCurrentContentAsSaved() {
    try {
      lastSavedContentHash = Arrays.hashCode(saveOp.generateBytes());
    }
    catch (IOException e) {
      lastSavedContentHash = 0; // harmless: the next auto-save writes and reports the problem itself
    }
  }

  public byte[] generateSnapshot() throws IOException {
    return saveOp.generateBytes();
  }

  private static int timerDelay() {
    Integer intervalSeconds = SettingAutoSave.getIntervalSeconds();
    return intervalSeconds != null ? intervalSeconds * 1000 : OFF_CHECK_INTERVAL_MS;
  }

  public static boolean isWorkingCopyNeeded() {
    return SettingAutoSave.getIntervalSeconds() != null || SettingAutoLoad.isEnabled();
  }

  public static File workingCopyFor(File nsdFile) {
    return new File(nsdFile.getAbsolutePath() + WORKING_COPY_EXTENSION);
  }

  public static void deleteWorkingCopyFor(File nsdFile) {
    if (nsdFile != null) {
      workingCopyFor(nsdFile).delete();
    }
  }

  /** Copies the file on disk into its working copy. Throws on failure — the caller must surface
   * this immediately, since nothing else creates the working copy AutoLoad needs to watch. */
  public static long createWorkingCopyFor(File nsdFile) throws IOException {
    if (nsdFile == null) {
      return 0;
    }
    File wc = workingCopyFor(nsdFile);
    Files.copy(nsdFile.toPath(), wc.toPath(), StandardCopyOption.REPLACE_EXISTING);
    return wc.lastModified();
  }

  public static boolean workingCopyExistsFor(File nsdFile) {
    return nsdFile != null && workingCopyFor(nsdFile).exists();
  }

}
