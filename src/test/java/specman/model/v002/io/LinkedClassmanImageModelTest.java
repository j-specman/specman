package specman.model.v002.io;

import org.junit.jupiter.api.Test;
import specman.model.v002.AbstractEditAreaModel_V002;
import specman.model.v002.ClassmanImageEditAreaModel_V002;
import specman.model.v002.DiagramModel_V002;
import specman.model.v002.EditorContentModel_V002;
import specman.model.v002.LinkedClassmanImageEditAreaModel_V002;

import java.util.List;

import static builder.BuilderFactory.diagramm;
import static builder.BuilderFactory.sequence;
import static builder.BuilderFactory.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Classman diagram in the model is either embedded with its source, or linked to a file with it. */
class LinkedClassmanImageModelTest {
  private static final String LINK = "classman://hilfe#0a1b2c3d,Hilfe, Versionierung";
  private static final String EMBEDDED = "classdiagram {\n    class A (aaaaaaaa)\n}\n";

  @Test
  void linkIsWrittenAsLinkAttribute() {
    String text = serialize(new LinkedClassmanImageEditAreaModel_V002(LINK, 1.25f, null));

    assertTrue(text.contains("image(scale=125%, type=classman) { link=`" + LINK + "` }"), text);
  }

  @Test
  void linkedAreaRoundTrips() throws ModelParseException {
    AbstractEditAreaModel_V002 parsed = saveAndLoad(new LinkedClassmanImageEditAreaModel_V002(LINK, 1.25f, null));

    assertTrue(parsed instanceof LinkedClassmanImageEditAreaModel_V002, parsed.getClass().getName());
    LinkedClassmanImageEditAreaModel_V002 linked = (LinkedClassmanImageEditAreaModel_V002) parsed;
    assertEquals(LINK, linked.link);
    assertEquals(1.25f, linked.individualScalePercent, 0.0001f);
  }

  @Test
  void embeddedAreaStaysEmbedded() throws ModelParseException {
    AbstractEditAreaModel_V002 parsed = saveAndLoad(new ClassmanImageEditAreaModel_V002(EMBEDDED, 1f, null));

    assertTrue(parsed instanceof ClassmanImageEditAreaModel_V002, parsed.getClass().getName());
    assertEquals(EMBEDDED, ((ClassmanImageEditAreaModel_V002) parsed).source);
  }

  private static String serialize(AbstractEditAreaModel_V002 area) {
    DiagramModel_V002 model = diagramm()
      .withMainSequence(sequence().withStep(simple("step")))
      .build();
    model.intro = new EditorContentModel_V002("intro", null);
    model.intro.addArea(area);
    return new ModelSerializer_V002().serialize(model);
  }

  private static AbstractEditAreaModel_V002 saveAndLoad(AbstractEditAreaModel_V002 area) throws ModelParseException {
    List<AbstractEditAreaModel_V002> areas = new ModelParser_V002().parse(serialize(area)).intro.areas;
    return areas.get(areas.size() - 1);
  }
}
