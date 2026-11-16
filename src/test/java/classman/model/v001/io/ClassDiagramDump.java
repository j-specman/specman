package classman.model.v001.io;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

/** Writes a model with all its public fields into a string, so two models can be compared field by field. */
final class ClassDiagramDump {

  private ClassDiagramDump() {
  }

  static String of(Object value) {
    StringBuilder out = new StringBuilder();
    append(out, value);
    return out.toString();
  }

  private static void append(StringBuilder out, Object value) {
    if (value == null) {
      out.append("null");
    } else if (value instanceof String text) {
      out.append('"').append(text.replace("\n", "\\n")).append('"');
    } else if (value instanceof Collection<?> collection) {
      appendCollection(out, collection);
    } else if (value.getClass().getPackageName().startsWith("classman.model.v001")
        && !value.getClass().isEnum()) {
      appendModel(out, value);
    } else {
      out.append(value);
    }
  }

  private static void appendCollection(StringBuilder out, Collection<?> collection) {
    out.append('[');
    for (Object element : collection) {
      append(out, element);
      out.append(", ");
    }
    out.append(']');
  }

  private static void appendModel(StringBuilder out, Object model) {
    out.append(model.getClass().getSimpleName()).append('{');
    for (Field field : model.getClass().getFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        out.append(field.getName()).append('=');
        append(out, fieldValue(field, model));
        out.append("; ");
      }
    }
    out.append('}');
  }

  private static Object fieldValue(Field field, Object model) {
    try {
      return field.get(model);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}
