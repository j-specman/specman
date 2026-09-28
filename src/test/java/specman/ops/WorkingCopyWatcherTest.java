package specman.ops;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkingCopyWatcherTest {

  private static final long CHANGE_TIMEOUT_SECONDS = 10;
  private static final long SILENCE_MILLIS = 1000;

  @TempDir
  Path tempDir;

  private final Semaphore changes = new Semaphore(0);
  private final WorkingCopyWatcher watcher = new WorkingCopyWatcher(changes::release);

  // The watcher holds a handle on the watched directory which prevents deleting it on Windows
  @AfterEach
  void closeWatcher() throws IOException {
    watcher.close();
  }

  @Test
  void inPlaceWriteIsReported() throws Exception {
    Path wrk = Files.writeString(tempDir.resolve("model.nsd.wrk"), "v1");
    watcher.watch(wrk.toFile());
    Files.writeString(wrk, "v2");
    assertChangeReported();
  }

  @Test
  void creationOfNotYetExistingFileIsReported() throws Exception {
    Path wrk = tempDir.resolve("model.nsd.wrk");
    watcher.watch(wrk.toFile());
    Files.writeString(wrk, "v1");
    assertChangeReported();
  }

  @Test
  void atomicReplaceIsReported() throws Exception {
    Path wrk = Files.writeString(tempDir.resolve("model.nsd.wrk"), "v1");
    watcher.watch(wrk.toFile());
    Path tmp = Files.writeString(tempDir.resolve("model.nsd.wrk.tmp"), "v2");
    Files.move(tmp, wrk, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    assertChangeReported();
  }

  @Test
  void otherFileInSameDirectoryIsIgnored() throws Exception {
    Path wrk = Files.writeString(tempDir.resolve("model.nsd.wrk"), "v1");
    watcher.watch(wrk.toFile());
    Files.writeString(tempDir.resolve("model.nsd"), "other");
    assertNoChangeReported();
    Files.writeString(wrk, "v2");
    assertChangeReported();
  }

  @Test
  void retargetWithinSameDirectoryFollowsNewFile() throws Exception {
    Path first = Files.writeString(tempDir.resolve("first.nsd.wrk"), "v1");
    Path second = Files.writeString(tempDir.resolve("second.nsd.wrk"), "v1");
    watcher.watch(first.toFile());
    watcher.watch(second.toFile());
    Files.writeString(first, "v2");
    assertNoChangeReported();
    Files.writeString(second, "v2");
    assertChangeReported();
  }

  @Test
  void retargetToOtherDirectoryIgnoresPreviousDirectory() throws Exception {
    Path oldWrk = Files.writeString(Files.createDirectory(tempDir.resolve("old")).resolve("model.nsd.wrk"), "v1");
    Path newWrk = Files.writeString(Files.createDirectory(tempDir.resolve("new")).resolve("model.nsd.wrk"), "v1");
    watcher.watch(oldWrk.toFile());
    watcher.watch(newWrk.toFile());
    Files.writeString(oldWrk, "v2");
    assertNoChangeReported();
    Files.writeString(newWrk, "v2");
    assertChangeReported();
  }

  private void assertChangeReported() throws InterruptedException {
    assertTrue(changes.tryAcquire(CHANGE_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Expected a change to be reported");
  }

  private void assertNoChangeReported() throws InterruptedException {
    assertFalse(changes.tryAcquire(SILENCE_MILLIS, TimeUnit.MILLISECONDS), "Expected no change to be reported");
  }

}
