package classman.render;

import classman.model.v001.ElementStyleModel_V001;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** One layouter for each font which the elements of a kind use, because a layouter measures texts in its font. */
final class FontLayouters<T> {
    private final TextMeasurer measurer;
    private final BiFunction<TextMeasurer, TextFont, T> factory;
    private final Map<TextFont, T> layouters = new HashMap<>();

    /** @param factory makes a layouter from a measurer for the font, and the font */
    FontLayouters(TextMeasurer measurer, BiFunction<TextMeasurer, TextFont, T> factory) {
        this.measurer = measurer;
        this.factory = factory;
    }

    /** @return the layouter for the font of the style */
    T of(ElementStyleModel_V001 style) {
        return layouters.computeIfAbsent(TextFont.of(style), font -> factory.apply(measurer.forFont(font), font));
    }
}
