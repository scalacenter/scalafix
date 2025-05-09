package scalafix.internal.interfaces;

//TODO: move to scalafix-versions when scalafix.interfaces.Scalafix is removed
public class ScalafixProperties {

    public static String getScalaVersionKey(String requestedScalaVersion) {
        String requestedScalaMajorMinorOrMajorVersion =
            requestedScalaVersion.replaceAll("^(\\d+\\.\\d+).*", "$1");

        if (requestedScalaMajorMinorOrMajorVersion.equals("2.12")) {
            return "scala212";
        } else if (requestedScalaMajorMinorOrMajorVersion.equals("2.13") ||
            requestedScalaMajorMinorOrMajorVersion.equals("2")) {
            return "scala213";
        } else if (requestedScalaMajorMinorOrMajorVersion.equals("3.0") ||
            requestedScalaMajorMinorOrMajorVersion.equals("3.1") ||
            requestedScalaMajorMinorOrMajorVersion.equals("3.2") ||
            requestedScalaMajorMinorOrMajorVersion.equals("3.3")) {
            return "scala33";
        } else if (requestedScalaMajorMinorOrMajorVersion.equals("3.5")) {
            return "scala35";
        } else if (requestedScalaMajorMinorOrMajorVersion.equals("3.6")) {
            return "scala36";
        } else if (requestedScalaMajorMinorOrMajorVersion.equals("3.7")) {
            return "scala37";
        } else if (requestedScalaMajorMinorOrMajorVersion.startsWith("3")) {
            return "scala3Next";
        } else {
            throw new IllegalArgumentException("Unsupported scala version " + requestedScalaVersion);
        }
    }
}
