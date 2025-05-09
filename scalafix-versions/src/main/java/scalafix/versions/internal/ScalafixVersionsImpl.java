package scalafix.versions.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import scalafix.interfaces.ScalafixVersions;
import scalafix.internal.interfaces.ScalafixProperties;

public class ScalafixVersionsImpl implements ScalafixVersions {

    private static final String PROPERTIES_PATH = "scalafix-versions.properties";

    private final Properties properties;
    
    public ScalafixVersionsImpl() throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_PATH);
        Properties properties = new Properties();
        properties.load(stream);
        this.properties = properties;
    }

    @Override
    public String scalafixVersion() {
        return properties.getProperty("scalafix");
    }

    @Override
    public String cliScalaVersion(String sourcesScalaVersion) {
        String scalaVersionKey = ScalafixProperties.getScalaVersionKey(sourcesScalaVersion);
        return properties.getProperty(scalaVersionKey);
    }
}
