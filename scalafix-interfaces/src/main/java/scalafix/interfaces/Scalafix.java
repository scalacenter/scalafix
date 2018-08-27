package scalafix.interfaces;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Public API for reflectively invoking Scalafix from a build tool or IDE integration.
 *
 * To obtain an instance of Scalafix, use {@link Scalafix#classloadInstance(ClassLoader)}.
 *
 * @implNote This interface is not intended to be extended, the only implementation of this interface
 * should live in the Scalafix repository.
 */
public interface Scalafix {

    /**
     * Run the Scalafix commmand-line interface <code>main</code> function.
     *
     * @param args The arguments passed to the command-line interface.
     */
    ScalafixError[] runMain(ScalafixMainArgs args);

    /**
     * @return Construct a new instance of {@link ScalafixMainArgs} that can be later passed to {@link #runMain(ScalafixMainArgs) }.
     */
    ScalafixMainArgs newMainArgs();

    /**
     * Get --help message for running the Scalafix command-line interface.
     *
     * @param screenWidth The width of the screen, used for wrapping long sentences
     *                    into multiple lines.
     * @return The help message as a string.
     */
    String mainHelp(int screenWidth);

    /**
     * The release version of the current Scalafix API instance.
     */
    String scalafixVersion();

    /**
     * The recommended Scalameta version to match the current Scalafix API instance.
     */
    String scalametaVersion();

    /**
     * The exact Scala versions that are supported by the recommended {@link #scalametaVersion()}
     */
    String[] supportedScalaVersions();

    /**
     * The most recent Scala 2.11 version in {@link #supportedScalaVersions()}
     */
    String scala211();

    /**
     * The most recent Scala 2.12 version in {@link #supportedScalaVersions()}
     */
    String scala212();



    /**
     * JVM runtime reflection method helper to classload an instance of {@link Scalafix}.
     *
     * @param classLoader Classloader containing the full Scalafix classpath, including the scalafix-cli module.
     * @return An implementation of the {@link Scalafix} interface.
     * @throws ScalafixException in case of errors during classloading, most likely caused
     * by an incorrect classloader argument.
     */
    static Scalafix classloadInstance(ClassLoader classLoader) throws ScalafixException {
        try {
            Class<?> cls = classLoader.loadClass("scalafix.internal.interfaces.ScalafixImpl");
            Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Scalafix) ctor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException |
                IllegalAccessException | InvocationTargetException |
                InstantiationException ex) {
            throw new ScalafixException(
                    "Failed to reflectively load Scalafix with classloader " + classLoader.toString(), ex);
        }
    }
}
