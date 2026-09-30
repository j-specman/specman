package specman.ops;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/** Watches a single file for modification. A {@link WatchService} can only watch
 * directories, so the file's parent directory is registered and the events are filtered by file name.
 * The callback runs on the watcher thread, so callers must hand over to the EDT themselves. It may be
 * called several times for a single change (e.g. truncate + write) and even without a change of the
 * file (e.g. on {@link java.nio.file.StandardWatchEventKinds#OVERFLOW}).
 * <p>
 * For technical reasons, file creation must be watched too, not just modification: on Windows,
 * an atomic replace of an already-existing file (write to a temp file, then rename over the target —
 * the safe way many tools, including editors that write the working copy externally, avoid leaving
 * a corrupt partial write) is reported as {@code ENTRY_CREATE}, not {@code ENTRY_MODIFY}. See
 * {@code WorkingCopyWatcherTest.atomicReplaceIsReported}. */
class WorkingCopyWatcher implements Closeable {

  private record Target(WatchKey key, Path directory, Path fileName) {}

  private final Runnable onWorkingCopyChanged;
  private WatchService watchService;
  private volatile Target target;

  WorkingCopyWatcher(Runnable onWorkingCopyChanged) {
    this.onWorkingCopyChanged = onWorkingCopyChanged;
  }

  /** Watches the passed file instead of the previously watched one. The file need not exist yet. */
  synchronized void watch(File file) throws IOException {
    createWatchServiceThread();
    Path path = file.toPath().toAbsolutePath();
    Path directory = path.getParent();
    Target previous = target;
    WatchKey key = (previous != null && previous.directory().equals(directory) && previous.key().isValid())
        ? previous.key()
        : directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY);
    target = new Target(key, directory, path.getFileName());
    if (previous != null && previous.key() != key) {
      previous.key().cancel();
    }
  }

  private void createWatchServiceThread() throws IOException {
    if (watchService == null) {
      watchService = FileSystems.getDefault().newWatchService();
      WatchService service = watchService;
      Thread thread = new Thread(() -> processEvents(service), "working-copy-watcher");
      thread.setDaemon(true);
      thread.start();
    }
  }

  private void processEvents(WatchService service) {
    while (true) {
      WatchKey key;
      try {
        key = service.take();
      }
      catch (InterruptedException | ClosedWatchServiceException e) {
        return;
      }
      // Always drain the events, otherwise reset() signals the key again immediately
      List<WatchEvent<?>> events = key.pollEvents();
      try {
        if (concernsTarget(key, events)) {
          onWorkingCopyChanged.run();
        }
      }
      catch (RuntimeException e) {
        e.printStackTrace();
      }
      key.reset();
    }
  }

  private boolean concernsTarget(WatchKey key, List<WatchEvent<?>> events) {
    Target current = target;
    return current != null && key == current.key() && events.stream()
        .anyMatch(event -> event.kind() == OVERFLOW || current.fileName().equals(event.context()));
  }

  @Override
  public synchronized void close() throws IOException {
    if (watchService != null) {
      watchService.close();
    }
  }

}
