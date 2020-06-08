package scalafix.internal.interfaces;

/**
 * A classloader that shares only scalafix-interfaces classes from the parent classloader.
 *
 * This classloader is intended to be used as a parent when class-loading scalafix-cli.
 * By using this classloader as a parent, it's possible to cast runtime instances from
 * the scalafix-cli classloader into `scalafix.interfaces.Scalafix` from this classlaoder.
 */
public class ScalafixInterfacesClassloader extends ClassLoader {
    private final ClassLoader parent;

    public ScalafixInterfacesClassloader(ClassLoader parent) {
        super(null);
        this.parent = parent;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("scalafix.interfaces")
                // include types in scalafix.interfaces.* signatures
                || name.startsWith("coursierapi")) {
            return parent.loadClass(name);
        } else {
            throw new ClassNotFoundException(name);
        }
    }
}
