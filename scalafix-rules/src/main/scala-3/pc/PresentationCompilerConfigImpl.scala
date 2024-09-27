package scalafix.internal.pc

import java.util.Optional
import java.util.concurrent.TimeUnit

import scala.meta.pc.PresentationCompilerConfig
import scala.meta.pc.PresentationCompilerConfig.OverrideDefFormat

case class PresentationCompilerConfigImpl(
    debug: Boolean = false,
    parameterHintsCommand: Optional[String] = Optional.empty(),
    completionCommand: Optional[String] = Optional.empty(),
    symbolPrefixes: java.util.Map[String, String] =
      PresentationCompilerConfig.defaultSymbolPrefixes(),
    overrideDefFormat: OverrideDefFormat = OverrideDefFormat.Ascii,
    isCompletionItemDetailEnabled: Boolean = true,
    isCompletionItemDocumentationEnabled: Boolean = true,
    isHoverDocumentationEnabled: Boolean = true,
    snippetAutoIndent: Boolean = true,
    isSignatureHelpDocumentationEnabled: Boolean = true,
    isCompletionSnippetsEnabled: Boolean = true,
    isCompletionItemResolve: Boolean = true,
    isStripMarginOnTypeFormattingEnabled: Boolean = true,
    timeoutDelay: Long = 20,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS,
    semanticdbCompilerOptions: java.util.List[String] =
      PresentationCompilerConfig.defaultSemanticdbCompilerOptions()
) extends PresentationCompilerConfig {

  override def isDefaultSymbolPrefixes(): Boolean = false
}
