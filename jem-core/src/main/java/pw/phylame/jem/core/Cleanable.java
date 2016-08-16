package pw.phylame.jem.core;

/**
 * Interface for cleaning resources and others when destroying chapter.
 *
 * @since 2.0.1
 */
public interface Cleanable {
    /**
     * Cleans the specified <code>Chapter</code>.
     *
     * @param chapter the <code>Chapter</code> to be cleaned
     */
    void clean(Chapter chapter);
}
