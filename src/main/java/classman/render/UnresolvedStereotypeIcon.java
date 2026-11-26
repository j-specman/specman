package classman.render;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** The icon of a stereotype which refers to a configuration the host doesn't know. It is read once, on first use. */
final class UnresolvedStereotypeIcon {
    private static final String RESOURCE = "images/svg/material/priority_high.svg";
    private static VectorImage icon;

    private UnresolvedStereotypeIcon() {
    }

    static synchronized VectorImage get() {
        if (icon == null) {
            icon = VectorImage.of(read());
        }
        return icon;
    }

    private static String read() {
        try (InputStream stream = UnresolvedStereotypeIcon.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("The icon file " + RESOURCE + " is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("The icon file " + RESOURCE + " can't be read", e);
        }
    }
}
