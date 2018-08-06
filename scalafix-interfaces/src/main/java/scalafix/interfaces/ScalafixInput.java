package scalafix.interfaces;

import java.nio.file.Path;
import java.util.Optional;

/**
 * An input represents an input source for code such as a file or virtual file.
 */
public interface ScalafixInput {

    /**
     * @return The full text contents of this input.
     */
    CharSequence text();

    /**
     * @return The string filename of this input.
     */
    String filename();

    /**
     * @return A path to the original source of this input if it came from a file on disk
     * or a virtual file system.
     */
    Optional<Path> path();
}
