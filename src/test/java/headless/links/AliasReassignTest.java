package headless.links;

import headless.HeadlessExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import specman.link.LinkAliases;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static specman.link.LinkAliases.absolutePath;

/** The user can tell which file an alias stands for, which another file had so far. */
@ExtendWith(HeadlessExtension.class)
class AliasReassignTest {

  /** Scenario: The alias belongs to a file which is gone. The user assigns it to another file. */
  @Test
  void aliasMovesToTheOtherFile() {
    File oldFile = new File("old/hilfe.cld");
    File newFile = new File("new/hilfe.cld");
    LinkAliases.assign(oldFile, "hilfe");

    LinkAliases.reassign(newFile, "hilfe");

    assertEquals(absolutePath(newFile), absolutePath(LinkAliases.fileOf("hilfe")));
    assertEquals("", LinkAliases.aliasOf(oldFile));
    assertTrue(LinkAliases.isAssigned(oldFile), "the old file stays decided against an alias");
  }

  /** Scenario: Nobody has the alias yet. It is simply assigned. */
  @Test
  void unusedAliasIsJustAssigned() {
    File file = new File("new/hilfe.cld");

    LinkAliases.reassign(file, "hilfe");

    assertEquals("hilfe", LinkAliases.aliasOf(file));
  }

  /** Scenario: The file has the alias already, nothing changes. */
  @Test
  void sameFileKeepsItsAlias() {
    File file = new File("hilfe.cld");
    LinkAliases.assign(file, "hilfe");

    LinkAliases.reassign(file, "hilfe");

    assertEquals("hilfe", LinkAliases.aliasOf(file));
    assertNull(LinkAliases.fileOf("other"));
  }
}
