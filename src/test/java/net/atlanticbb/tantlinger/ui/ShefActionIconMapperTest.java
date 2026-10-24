package net.atlanticbb.tantlinger.ui;

import net.atlanticbb.tantlinger.ui.text.actions.HTMLAlignAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLBlockAction;
import net.atlanticbb.tantlinger.ui.text.actions.HTMLInlineAction;
import org.junit.jupiter.api.Test;
import specman.graphics.SpecmanIcons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShefActionIconMapperTest {

  @Test
  void inlineActionsGetBoldItalicUnderlineIcons() {
    assertEquals(SpecmanIcons.FORMAT_BOLD, ShefActionIconMapper.pathFor(new HTMLInlineAction(HTMLInlineAction.BOLD)));
    assertEquals(SpecmanIcons.FORMAT_ITALIC, ShefActionIconMapper.pathFor(new HTMLInlineAction(HTMLInlineAction.ITALIC)));
    assertEquals(SpecmanIcons.FORMAT_UNDERLINE, ShefActionIconMapper.pathFor(new HTMLInlineAction(HTMLInlineAction.UNDERLINE)));
    assertNull(ShefActionIconMapper.pathFor(new HTMLInlineAction(HTMLInlineAction.SUP)));
  }

  @Test
  void alignActionsAreToldApartByName() {
    assertEquals(SpecmanIcons.ALIGN_LEFT, ShefActionIconMapper.pathFor(new HTMLAlignAction(HTMLAlignAction.LEFT)));
    assertEquals(SpecmanIcons.ALIGN_CENTER, ShefActionIconMapper.pathFor(new HTMLAlignAction(HTMLAlignAction.CENTER)));
    assertEquals(SpecmanIcons.ALIGN_RIGHT, ShefActionIconMapper.pathFor(new HTMLAlignAction(HTMLAlignAction.RIGHT)));
    assertEquals(SpecmanIcons.ALIGN_JUSTIFY, ShefActionIconMapper.pathFor(new HTMLAlignAction(HTMLAlignAction.JUSTIFY)));
  }

  @Test
  void listBlockActionsGetListIcons() {
    assertEquals(SpecmanIcons.LIST_ORDERED, ShefActionIconMapper.pathFor(new HTMLBlockAction(HTMLBlockAction.OL)));
    assertEquals(SpecmanIcons.LIST_UNORDERED, ShefActionIconMapper.pathFor(new HTMLBlockAction(HTMLBlockAction.UL)));
    assertNull(ShefActionIconMapper.pathFor(new HTMLBlockAction(HTMLBlockAction.P)));
  }

}
