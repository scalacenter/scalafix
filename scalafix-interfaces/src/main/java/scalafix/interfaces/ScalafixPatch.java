package scalafix.interfaces;

public interface ScalafixPatch {
    /**
     * Can be RemoveGlobalImport, RemoveImportee, AddGlobalImport, AddGlobalSymbol, ReplaceSymbol
     * @return
     */
    String kind();
}