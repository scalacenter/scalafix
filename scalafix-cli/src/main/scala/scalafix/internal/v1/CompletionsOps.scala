package scalafix.internal.v1

import metaconfig.generic.Setting
import metaconfig.generic.Settings
import metaconfig.internal.Case
import org.apache.commons.text.StringEscapeUtils

object CompletionsOps {
  private def option(kebab: String): String =
    if (kebab.length == 1) "-" + kebab
    else "--" + kebab

  private def names(setting: Setting): List[String] =
    if (setting.isHidden) Nil
    else setting.name :: setting.alternativeNames.filter(_.length == 1)

  private def toZshOption(setting: Setting): scala.collection.Seq[String] = {
    if (setting.isHidden) Nil
    else {
      // See https://github.com/zsh-users/zsh-completions/blob/master/zsh-completions-howto.org#writing-completion-functions-using-_arguments
      // for more details on how to use _arguments in zsh.
      val (repeat, assign, message, action) = setting.name match {
        case "rules" => ("*", "=", ":rule", ":_rule_names")
        case _ => ("", "", "", "")
      }
      val description = setting.description.fold("") { x =>
        val escaped = StringEscapeUtils.escapeXSI(x.replaceAll("\\s+", " "))
        s"$assign[$escaped]"
      }
      names(setting).map { name =>
        s""""$repeat${option(Case.camelToKebab(name))}$description$message$action""""
      }
    }
  }

  private def bashArgs: String = {
    Settings[Args].settings
      .flatMap(names)
      .map(name => Case.camelToKebab(name))
      .distinct
      .mkString(" ")
  }

  private def zshArgs: String = {
    Settings[Args].settings.flatMap(toZshOption).mkString(" \\\n   ")
  }

  private def zshNames: String =
    Rules
      .all()
      .map { rule =>
        val name = rule.name.value
        val description = rule.description
        s""""$name[${StringEscapeUtils.escapeXSI(description)}]""""
      }
      .mkString(" \\\n  ")

  def bashCompletions: String =
    s"""
_scalafix()
{
    local cur prev opts
    COMPREPLY=()
    cur="$${COMP_WORDS[COMP_CWORD]}"
    prev="$${COMP_WORDS[COMP_CWORD-1]}"
    rules="${Rules.all().mkString(" ")}"
    opts="$bashArgs"

    case "$${prev}" in
      --rules|-r )
        COMPREPLY=(   $$(compgen -W "$${rules}" -- $${cur}) )
        return 0
        ;;
    esac
    if [[ $${cur} == -* ]] ; then
        COMPREPLY=(   $$(compgen -W "$${opts}" -- $${cur}) )
        return 0
    fi
}
complete -F _scalafix scalafix
"""

  def zshCompletions: String = {
    s"""#compdef scalafix
typeset -A opt_args
local context state line

_rule_names () {
   _values "rules" $zshNames
}

local -a scalafix_opts
scalafix_opts=(
  $zshArgs
)

case $$words[$$CURRENT] in
      *) _arguments $$scalafix_opts "*::filename:_files";;
esac

return 0
"""
  }

}
