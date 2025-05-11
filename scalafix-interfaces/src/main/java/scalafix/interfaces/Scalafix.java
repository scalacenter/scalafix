package scalafix.interfaces;

import coursierapi.Repository;

import scalafix.internal.interfaces.ScalafixCoursier;
import scalafix.internal.interfaces.ScalafixInterfacesClassloader;
import scalafix.internal.interfaces.ScalafixProperties;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Public API for reflectively invoking Scalafix from a build tool or IDE
 * integration.
 * <p>
 * To obtain an instance of Scalafix, classload
 * <code>ch.epfl.scala:scalafix-loader</code> and use {@link #get()}.
 *
 * @implNote This interface is not intended to be extended, the only
 *           implementation of this interface should live in the Scalafix
 *           repository.
 */
public interface Scalafix {

    /**
     * @return Construct a new instance of {@link ScalafixArguments}.
     */
    ScalafixArguments newArguments();

    /**
     * Get --help message for running the Scalafix command-line interface.
     *
     * @param screenWidth The width of the screen, used for wrapping long sentences
     *                    into multiple lines.
     * @return The help message as a string.
     */
    String mainHelp(int screenWidth);

    /**
     * The exact Scala versions used
     */
    String scalaVersion();

    /**
     * The release version of the current Scalafix API instance.
     */
    String scalafixVersion();

    /**
     * The recommended Scalameta version to match the current Scalafix API instance.
     */
    String scalametaVersion();

    /**
     * The exact Scala versions that are supported
     */
    String[] supportedScalaVersions();

    @Deprecated
    String scala211();

    /**
     * The Scala 2.12 version in {@link #supportedScalaVersions()}
     */
    String scala212();

    /**
     * The Scala 2.13 version in {@link #supportedScalaVersions()}
     */
    String scala213();

    /**
     * The Scala 3.3 version in {@link #supportedScalaVersions()}
     */
    String scala33();

    /**
     * The Scala 3.5 version in {@link #supportedScalaVersions()}
     */
    String scala35();

    /**
     * The Scala 3.6 version in {@link #supportedScalaVersions()}
     */
    String scala36();

    /**
     * The Scala 3.7 version in {@link #supportedScalaVersions()}
     */
    String scala37();

    /**
     * The Scala 3 LTS version in {@link #supportedScalaVersions()}
     */
    String scala3LTS();

    /**
     * The Scala 3 Next version in {@link #supportedScalaVersions()}
     */
    String scala3Next();

    /**
     * @deprecated Use {@link #get()} instead.
     */
    @Deprecated
    static Scalafix fetchAndClassloadInstance(String requestedScalaVersion) throws ScalafixException {
        return fetchAndClassloadInstance(requestedScalaVersion, Repository.defaults());
    }

    /**
     * @deprecated Use {@link #get()} instead.
     */
    @Deprecated
    static Scalafix fetchAndClassloadInstance(String requestedScalaVersion, List<Repository> repositories)
            throws ScalafixException {

        Properties properties = new Properties();
        String propertiesPath = "scalafix-interfaces.properties";
        InputStream stream = Scalafix.class.getClassLoader().getResourceAsStream(propertiesPath);
        try {
            properties.load(stream);
        } catch (IOException | NullPointerException e) {
            throw new ScalafixException("Failed to load '" + propertiesPath + "' to lookup versions", e);
        }

        String scalafixVersion = properties.getProperty("scalafixVersion");
        String scalaVersionKey = ScalafixProperties.getScalaVersionKey(requestedScalaVersion);
        String scalaVersion = properties.getProperty(scalaVersionKey);
        if (scalafixVersion == null || scalaVersion == null)
            throw new ScalafixException("Failed to lookup versions from '" + propertiesPath + "'");

        List<URL> jars = ScalafixCoursier.scalafixCliJars(repositories, scalafixVersion, scalaVersion);
        ClassLoader parent = new ScalafixInterfacesClassloader(Scalafix.class.getClassLoader());
        return classloadInstance(new URLClassLoader(jars.stream().toArray(URL[]::new), parent));
    }

    /**
     * @deprecated Use {@link #get()} instead.
     */
    @Deprecated
    static Scalafix classloadInstance(ClassLoader classLoader) throws ScalafixException {
        try {
            Class<?> cls = classLoader.loadClass("scalafix.internal.interfaces.ScalafixImpl");
            Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            return (Scalafix) ctor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException
                | InstantiationException ex) {
            throw new ScalafixException(
                    "Failed to reflectively load Scalafix with classloader " + classLoader.toString(), ex);
        }
    }

    /**
     * Obtains an implementation of Scalafix using the current classpath.
     * 
     * @return the first available implementation advertised as a service provider.
     */
    static Scalafix get() {
        ServiceLoader<Scalafix> loader = ServiceLoader.load(Scalafix.class);
        Iterator<Scalafix> iterator = loader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new IllegalStateException("No implementation found");
        }
    }
}
