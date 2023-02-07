package scalafix.interfaces;

public interface ScalafixTextEdit {
    ScalafixPosition position();

    String newText();
}
