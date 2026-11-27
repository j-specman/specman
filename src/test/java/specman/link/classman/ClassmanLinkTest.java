package specman.link.classman;

import org.junit.jupiter.api.Test;
import specman.link.LinkLocation;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Classman link names the file which has a diagram, by an alias or by its path, and may tell its ID and name. */
class ClassmanLinkTest {

  @Test
  void aliasLink() {
    String link = ClassmanLink.of(new File("hilfe.cld"), "hilfe", null, null);

    assertEquals("classman://hilfe", link);
    ClassmanLinkReference reference = ClassmanLink.parse(link);
    assertEquals("hilfe", reference.alias());
    assertTrue(reference.hasAlias());
    assertNull(reference.diagramId());
    assertNull(reference.diagramName());
  }

  @Test
  void pathLinkWithSpaceAndComma() {
    File file = new File("Dokumente mit Komma, und Leerzeichen/hilfe.cld").getAbsoluteFile();

    String link = ClassmanLink.of(file, "", null, null);

    assertFalse(link.contains(" "), link);
    assertFalse(link.contains(","), link);
    ClassmanLinkReference reference = ClassmanLink.parse(link);
    assertFalse(reference.hasAlias());
    assertEquals(file, reference.location().resolve(new File(".")));
  }

  /** The path of the file is relative to the directory of the document with the link. */
  @Test
  void relativePathLink() {
    String link = ClassmanLink.of(LinkLocation.parse("../diagrams/hilfe.cld"), "0a1b2c3d", "Hilfe");

    assertEquals("classman://../diagrams/hilfe.cld#0a1b2c3d,Hilfe", link);
    ClassmanLinkReference reference = ClassmanLink.parse(link);
    assertTrue(reference.location().isRelative());
    assertEquals("Hilfe", reference.diagramName());
  }

  /** A diagram file is another file than the document, so a link without file means nothing. */
  @Test
  void localLinkIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> ClassmanLink.parse("classman://#0a1b2c3d,Hilfe"));
  }

  @Test
  void idAndName() {
    String link = ClassmanLink.of(new File("hilfe.cld"), "hilfe", "0a1b2c3d", "Hilfe Versionierung");

    assertEquals("classman://hilfe#0a1b2c3d,Hilfe Versionierung", link);
    ClassmanLinkReference reference = ClassmanLink.parse(link);
    assertEquals("0a1b2c3d", reference.diagramId());
    assertEquals("Hilfe Versionierung", reference.diagramName());
  }

  @Test
  void nameWithoutId() {
    String link = ClassmanLink.of(new File("hilfe.cld"), "hilfe", null, "Hilfe");

    assertEquals("classman://hilfe#,Hilfe", link);
    ClassmanLinkReference reference = ClassmanLink.parse(link);
    assertNull(reference.diagramId());
    assertEquals("Hilfe", reference.diagramName());
  }

  @Test
  void idWithoutName() {
    String link = ClassmanLink.of(new File("hilfe.cld"), "hilfe", "0a1b2c3d", null);

    assertEquals("classman://hilfe#0a1b2c3d", link);
    ClassmanLinkReference reference = ClassmanLink.parse(link);
    assertEquals("0a1b2c3d", reference.diagramId());
    assertNull(reference.diagramName());
  }

  @Test
  void nameWithCommasBackticksAndPercent() {
    String name = "Hilfe, `Version` 100%25 neu";

    String link = ClassmanLink.of(new File("hilfe.cld"), "hilfe", "0a1b2c3d", name);

    assertFalse(link.contains("`"), link);
    assertEquals(name, ClassmanLink.parse(link).diagramName());
  }

  @Test
  void surroundingBlanksAreIgnored() {
    assertEquals("hilfe", ClassmanLink.parse("  classman://hilfe \n").alias());
  }

  @Test
  void slashesAddedByToolsAreIgnored() {
    ClassmanLinkReference reference = ClassmanLink.parse("classman://hilfe/#0a1b2c3d,Hilfe/");

    assertEquals("hilfe", reference.alias());
    assertEquals("0a1b2c3d", reference.diagramId());
    assertEquals("Hilfe", reference.diagramName());
  }

  @Test
  void textWhichIsNoLink() {
    assertThrows(IllegalArgumentException.class, () -> ClassmanLink.parse("specman://hilfe"));
    assertThrows(IllegalArgumentException.class, () -> ClassmanLink.parse("classman://"));
    assertThrows(IllegalArgumentException.class, () -> ClassmanLink.parse("classman://#0a1b2c3d"));
  }
}
