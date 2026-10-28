package specman.graphics;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IconResourcesTest {

  /** All icon paths declared as public String constants in {@link SpecmanIcons}. */
  private static List<String> allPaths() throws IllegalAccessException {
    List<String> paths = new ArrayList<>();
    for (Field field : SpecmanIcons.class.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())
          && field.getType() == String.class) {
        paths.add((String) field.get(null));
      }
    }
    return paths;
  }

  @Test
  void allDeclaredIconsAreLoadable() throws IllegalAccessException {
    assertFalse(allPaths().isEmpty());
    for (String path : allPaths()) {
      Icon icon = IconReader.readSvgIcon(path, SpecmanIcons.SIZE_SMALL);
      assertEquals(SpecmanIcons.SIZE_SMALL, icon.getIconWidth(), path);
      assertEquals(SpecmanIcons.SIZE_SMALL, icon.getIconHeight(), path);
    }
  }

  @Test
  void missingResourceIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> IconReader.readSvgIcon("images/svg/material/does-not-exist.svg", SpecmanIcons.SIZE_SMALL));
  }

}
