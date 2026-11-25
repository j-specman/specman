package classman.editor;

import classman.model.v001.RelationType_V001;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A button with variants starts with its first kind and remembers the choice while the application runs. */
class VariantButtonTest {
  static {
    System.setProperty("java.awt.headless", "true");
  }

  private final EditorTexts texts = new EditorTexts(Locale.ENGLISH);

  @BeforeEach
  void startFresh() {
    VariantButton.forgetChoices();
  }

  private VariantButton<RelationType_V001> button(String name) {
    return new VariantButton<>(texts, List.of(
      new PaletteVariant<>("association", RelationType_V001.ASSOCIATION, EditorIcons.ASSOCIATION),
      new PaletteVariant<>("aggregation", RelationType_V001.AGGREGATION, EditorIcons.AGGREGATION),
      new PaletteVariant<>("composition", RelationType_V001.COMPOSITION, EditorIcons.COMPOSITION)), name, type -> { });
  }

  /** Nobody has chosen yet: the first kind. */
  @Test
  void startsWithTheFirstKind() {
    assertEquals(RelationType_V001.ASSOCIATION, button("variants").currentValue());
  }

  /** A button which is made later, for the next diagram editor, shows the kind which was chosen last. */
  @Test
  void rememberedForTheNextButton() {
    button("variants").chooseValue(RelationType_V001.COMPOSITION);

    assertEquals(RelationType_V001.COMPOSITION, button("variants").currentValue());
  }

  /** The buttons don't share their choices. */
  @Test
  void choicesBelongToTheButton() {
    button("variants").chooseValue(RelationType_V001.COMPOSITION);

    assertEquals(RelationType_V001.ASSOCIATION, button("other").currentValue());
  }

  /** Showing the kind which is armed is no choice. */
  @Test
  void showingTheArmedKindIsNotRemembered() {
    button("variants").showValue(RelationType_V001.AGGREGATION);

    assertEquals(RelationType_V001.ASSOCIATION, button("variants").currentValue());
  }

  /** A restart of the application forgets the choices. */
  @Test
  void forgottenAfterRestart() {
    button("variants").chooseValue(RelationType_V001.COMPOSITION);

    VariantButton.forgetChoices();

    assertEquals(RelationType_V001.ASSOCIATION, button("variants").currentValue());
  }
}
