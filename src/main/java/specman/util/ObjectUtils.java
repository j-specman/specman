package specman.util;

import java.util.Objects;
import java.util.function.Supplier;

public class ObjectUtils {

    /** Shorthand for {@link Objects#requireNonNullElse} — same semantics, less visual noise. Named after SQL's NVL function. */
    public static <T> T nvl(T value, T fallback) {
        return Objects.requireNonNullElse(value, fallback);
    }

    /** Shorthand for {@link Objects#requireNonNullElseGet} — same semantics, less visual noise. Named after SQL's NVL function. */
    public static <T> T nvl(T value, Supplier<T> fallback) {
        return Objects.requireNonNullElseGet(value, fallback);
    }

}
