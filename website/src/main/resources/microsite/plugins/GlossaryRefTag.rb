module Jekyll
  class GlossaryRefTag < Liquid::Tag

    def initialize(tag_name, term, tokens)
      super
      @term = term.strip
    end

    def render(context)
      baseurl = context['site']['baseurl']
      docsurl = context['site']['docsUrl']
      "[#{@term}](#{baseurl}/#{docsurl}/rule-authors/glossary##{@term.downcase})"
    end
  end
end

Liquid::Template.register_tag('glossary_ref', Jekyll::GlossaryRefTag)
