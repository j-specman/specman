package specman.model.v002.io;

import org.junit.jupiter.api.Test;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.MetaTagConfigModel_V002;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Colors of a meta tag configuration are optional; a missing color means "default" and is not written. */
class MetaTagConfigColorsTest {
  private static final int RED = 0xFFFF0000;
  private static final int GREEN = 0xFF00FF00;
  private static final int BLUE = 0xFF0000FF;

  @Test
  void configWithoutColorsRoundTrips() throws ModelParseException {
    MetaTagConfigModel_V002 parsed = saveAndLoad(config(null, null, null));

    assertNull(parsed.backgroundColor);
    assertNull(parsed.borderColor);
    assertNull(parsed.foregroundColor);
  }

  @Test
  void configWithAllColorsRoundTrips() throws ModelParseException {
    MetaTagConfigModel_V002 parsed = saveAndLoad(config(RED, GREEN, BLUE));

    assertEquals(RED, parsed.backgroundColor);
    assertEquals(GREEN, parsed.borderColor);
    assertEquals(BLUE, parsed.foregroundColor);
  }

  @Test
  void configWithForegroundOnlyRoundTrips() throws ModelParseException {
    MetaTagConfigModel_V002 parsed = saveAndLoad(config(null, null, BLUE));

    assertNull(parsed.backgroundColor);
    assertNull(parsed.borderColor);
    assertEquals(BLUE, parsed.foregroundColor);
  }

  @Test
  void configWithBackgroundAndBorderIsWrittenAsBefore() {
    String text = serialize(config(RED, GREEN, null));

    assertTrue(text.contains("tag(`T`, background=#ff0000, border=#00ff00, label=`T`)"), text);
    assertFalse(text.contains("foreground"), text);
  }

  private static MetaTagConfigModel_V002 config(Integer background, Integer border, Integer foreground) {
    return new MetaTagConfigModel_V002("T", "T", null, background, border, foreground, true);
  }

  private static String serialize(MetaTagConfigModel_V002 config) {
    DiagramModel_V002 model = diagramm()
      .withMainSequence(sequence().withStep(simple("step")))
      .withMetaTagConfig(config)
      .build();
    return new ModelSerializer_V002().serialize(model);
  }

  private static MetaTagConfigModel_V002 saveAndLoad(MetaTagConfigModel_V002 config) throws ModelParseException {
    return new ModelParser_V002().parse(serialize(config)).metaTagConfigs.get(0);
  }
}
