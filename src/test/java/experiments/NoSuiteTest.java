package experiments;

import org.junit.jupiter.api.Disabled;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks tests which must not run automatically as part of the test suite, because they are
 * not suite-capable: They need a display, the file system, user settings or a human looking at
 * the result, or they are experiments rather than regression tests.
 * <p>
 * Works exactly like {@link Disabled} (it is a composed annotation), but states the reason
 * right at the test. To run such a test interactively, deactivate JUnit's disabled-condition in
 * the run configuration: {@code -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition}
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Disabled("Not suite-capable (display, file system, user settings or experiment) - run interactively only")
public @interface NoSuiteTest {
}
