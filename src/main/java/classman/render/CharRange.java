package classman.render;

/** The characters of a text from start up to end. */
class CharRange {
    final int start;
    final int end;

    CharRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    /** @return the distance between this range and a later one, negative if they overlap */
    int gapTo(CharRange later) {
        return later.start - end;
    }

    /** @return the smallest range which covers both */
    CharRange coveringWith(CharRange other) {
        return new CharRange(Math.min(start, other.start), Math.max(end, other.end));
    }
}
