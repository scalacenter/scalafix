package scalafix.loader.internal;

import scalafix.interfaces.Scalafix;
import scalafix.interfaces.ScalafixArguments;
import scalafix.interfaces.ScalafixVersions;

public class ScalafixImpl implements Scalafix {

    @Override
    public ScalafixArguments newArguments() {
        return new ScalafixArgumentsImpl();
    }

    @Override
    public String mainHelp(int screenWidth) {
        throw new UnsupportedOperationException("Unimplemented method 'mainHelp'");
    }

    @Override
    public String scalaVersion() {
        throw new UnsupportedOperationException("Unimplemented method 'scalaVersion'");
    }

    @Override
    public String scalafixVersion() {
        return ScalafixVersions.get().scalafixVersion();
    }

    @Override
    public String scalametaVersion() {
        throw new UnsupportedOperationException("Unimplemented method 'scalametaVersion'");
    }

    @Override
    public String[] supportedScalaVersions() {
        throw new UnsupportedOperationException("Unimplemented method 'supportedScalaVersions'");
    }

    @SuppressWarnings("deprecation")
    @Override
    public String scala211() {
        throw new UnsupportedOperationException("Unimplemented method 'scala211'");
    }

    @Override
    public String scala212() {
        throw new UnsupportedOperationException("Unimplemented method 'scala212'");
    }

    @Override
    public String scala213() {
        throw new UnsupportedOperationException("Unimplemented method 'scala213'");
    }

    @Override
    public String scala33() {
        throw new UnsupportedOperationException("Unimplemented method 'scala33'");
    }

    @Override
    public String scala35() {
        throw new UnsupportedOperationException("Unimplemented method 'scala35'");
    }

    @Override
    public String scala36() {
        throw new UnsupportedOperationException("Unimplemented method 'scala36'");
    }

    @Override
    public String scala37() {
        throw new UnsupportedOperationException("Unimplemented method 'scala37'");
    }

    @Override
    public String scala3LTS() {
        throw new UnsupportedOperationException("Unimplemented method 'scala3LTS'");
    }

    @Override
    public String scala3Next() {
        throw new UnsupportedOperationException("Unimplemented method 'scala3Next'");
    }
}
