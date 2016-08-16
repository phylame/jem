package pw.phylame.jem.util;

import pw.phylame.jem.core.Chapter;

/**
 * Filter for matching <code>Chapter</code>.
 */
public interface Filter {
    /**
     * Tests the specified chapter is wanted or not.
     *
     * @param chapter the chapter
     * @return <code>true</code> if the chapter is matched
     */
    boolean accept(Chapter chapter);
}
