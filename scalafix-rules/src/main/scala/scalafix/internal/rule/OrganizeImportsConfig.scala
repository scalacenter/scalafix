package scalafix.internal.rule

import metaconfig._

sealed trait ImportsOrder

object ImportsOrder {
  case object Ascii extends ImportsOrder
  case object AsciiCaseInsensitive extends ImportsOrder
  case object SymbolsFirst extends ImportsOrder
  case object Keep extends ImportsOrder

  implicit val codec: ConfCodecEx[ImportsOrder] = OrganizeImportsConfig
    .getCodecFrom(Ascii, AsciiCaseInsensitive, SymbolsFirst, Keep)
}

sealed trait ImportSelectorsOrder

object ImportSelectorsOrder {
  case object Ascii extends ImportSelectorsOrder
  case object SymbolsFirst extends ImportSelectorsOrder
  case object Keep extends ImportSelectorsOrder

  implicit val codec: ConfCodecEx[ImportSelectorsOrder] = OrganizeImportsConfig
    .getCodecFrom(Ascii, SymbolsFirst, Keep)
}

sealed trait GroupedImports

object GroupedImports {
  case object AggressiveMerge extends GroupedImports
  case object Merge extends GroupedImports
  case object Explode extends GroupedImports
  case object Keep extends GroupedImports

  implicit val codec: ConfCodecEx[GroupedImports] = OrganizeImportsConfig
    .getCodecFrom(AggressiveMerge, Merge, Explode, Keep)
}

sealed trait BlankLines

object BlankLines {
  case object Auto extends BlankLines
  case object Manual extends BlankLines

  implicit val codec: ConfCodecEx[BlankLines] = OrganizeImportsConfig
    .getCodecFrom(Auto, Manual)
}

sealed trait TargetDialect
object TargetDialect {
  case object Auto extends TargetDialect
  case object Scala2 extends TargetDialect
  case object Scala3 extends TargetDialect
  case object StandardLayout extends TargetDialect

  implicit val codec: ConfCodecEx[TargetDialect] = OrganizeImportsConfig
    .getCodecFrom(Auto, Scala2, Scala3, StandardLayout)
}

sealed trait GroupSeparately
object GroupSeparately {
  case object ByNameImplicits extends GroupSeparately
  case object ByTypeGivens extends GroupSeparately

  implicit val codec: ConfCodecEx[GroupSeparately] = OrganizeImportsConfig
    .getCodecFrom(ByNameImplicits, ByTypeGivens)
}

sealed trait GroupRelativeImports
object GroupRelativeImports {
  case object KeepOrdered extends GroupRelativeImports
  case object Grouped extends GroupRelativeImports

  implicit val codec: ConfCodecEx[GroupRelativeImports] = OrganizeImportsConfig
    .getCodecFrom(KeepOrdered, Grouped)
}

final case class OrganizeImportsConfig(
    blankLines: BlankLines = BlankLines.Auto,
    coalesceToWildcardImportThreshold: Option[Int] = None,
    expandRelative: Boolean = false,
    groupRelativeImports: GroupRelativeImports =
      GroupRelativeImports.KeepOrdered,
    groupSeparately: Seq[GroupSeparately] = Seq(GroupSeparately.ByTypeGivens),
    groupedImports: GroupedImports = GroupedImports.Explode,
    groups: Seq[String] = Seq(
      "*",
      "re:(javax?|scala)\\."
    ),
    importSelectorsOrder: ImportSelectorsOrder = ImportSelectorsOrder.Ascii,
    importsOrder: ImportsOrder = ImportsOrder.Ascii,
    removeUnused: Boolean = true,
    targetDialect: TargetDialect = TargetDialect.StandardLayout
) {

  def merge(conf: Conf): Configured[OrganizeImportsConfig] = {
    val (state, confRest) = (conf match {
      case conf: Conf.Obj =>
        val remapped = conf.removeMapIf {
          case ("preset", Conf.Str("DEFAULT")) =>
            OrganizeImportsConfig.default
          case ("preset", Conf.Str("INTELLIJ_2020_3")) =>
            OrganizeImportsConfig(
              coalesceToWildcardImportThreshold = Some(5),
              groupedImports = GroupedImports.Merge
            )
        }
        remapped.map { case (state, elems) => state -> Conf.Obj(elems) }
      case _ => None
    }).getOrElse(this -> conf)
    OrganizeImportsConfig.decoder.read(Some(state), confRest)
  }

}

object OrganizeImportsConfig {
  val default: OrganizeImportsConfig = OrganizeImportsConfig()

  implicit val surface: generic.Surface[OrganizeImportsConfig] =
    generic.deriveSurface
  implicit val encoder: ConfEncoder[OrganizeImportsConfig] =
    generic.deriveEncoder[OrganizeImportsConfig]
  implicit val decoder: ConfDecoderEx[OrganizeImportsConfig] = {
    val baseDecoder = generic.deriveDecoderEx(default).noTypos
    baseDecoder.withSectionRenames(
      annotation.SectionRename { case Conf.Bool(flag) =>
        if (flag) Conf.Lst(Conf.Str("ByNameImplicits")) else Conf.Lst()
      }("groupExplicitlyImportedImplicitsSeparately", "groupSeparately")
    )
  }

  def getCodecFrom[T](values: T*): ConfCodecEx[T] =
    ConfCodecEx.oneOf(values.map(x => sourcecode.Text(x, x.toString)): _*)

}
