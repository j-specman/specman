package classman.render;

/** Mixes colors, to get pale and veiled variants without transparency. */
final class ColorMix {

    private ColorMix() {
    }

    /** @param share how much of the other color goes into the result, between 0 and 1 */
    static String blend(String color, String other, double share) {
        int from = Integer.parseInt(color.substring(1), 16);
        int to = Integer.parseInt(other.substring(1), 16);
        return String.format("#%02x%02x%02x",
            channel(from >> 16, to >> 16, share), channel(from >> 8, to >> 8, share), channel(from, to, share));
    }

    private static int channel(int from, int to, double share) {
        int start = from & 0xff;
        int end = to & 0xff;
        return (int) Math.round(start + (end - start) * share);
    }
}
