package scalafix.interfaces;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import scalafix.internal.interfaces.ScalafixInterfacesClassloader;

/**
 * Public API for looking up which one of the
 * <code>ch.epfl.scala:::scalafix-cli</code> artifacts of a Scalafix release is
 * the most appropriate, given the Scala version used to compile Scalafix target
 * sources.
 * <p>
 * To obtain an instance of ScalafixVersions, fetch the corresponding
 * <code>ch.epfl.scala:scalafix-versions</code> and use {@link #get}.
 *
 * @implNote This interface is not intended to be extended, the only
 *           implementation of this interface should live in the Scalafix
 *           repository.
 */
public interface ScalafixVersions {

    /**
     * @return the Scalafix release described.
     */
    String scalafixVersion();

    /**
     * Returns the most appropriate full Scala version to resolve
     * <code>ch.epfl.scala:::scalafix-cli:scalafixVersion</code> with.
     * 
     * @param sourcesScalaVersion The Scala version (i.e. "3.3.4") used to compile
     *                            Scalafix target sources.
     * @return a full Scala version to resolve the artifact with.
     */
    String cliScalaVersion(String sourcesScalaVersion);

    /**
     * Obtains an implementation of ScalafixVersions using the provided
     * classpath URLs.
     * 
     * @param classpathUrls URLs to be used in the classloader for loading
     *                      a ScalafixVersions implementation
     * @return the first available implementation advertised as a service provider.
     */
    static ScalafixVersions get(List<URL> classpathUrls) {
        ClassLoader parent = new ScalafixInterfacesClassloader(ScalafixVersions.class.getClassLoader());
        ClassLoader classLoader = new URLClassLoader(classpathUrls.stream().toArray(URL[]::new), parent);
        ServiceLoader<ScalafixVersions> loader = ServiceLoader.load(ScalafixVersions.class, classLoader);
        Iterator<ScalafixVersions> iterator = loader.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new IllegalStateException("No implementation found");
        }
    }
}
