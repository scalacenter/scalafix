module Jekyll
  class VocabularyRefTag < Liquid::Tag

    def initialize(tag_name, term, tokens)
      super
      @term = term.strip
    end

    def render(context)
      baseurl = context['site']['baseurl']
      docsurl = context['site']['docsUrl']
      "[#{@term}](#{baseurl}/#{docsurl}/creating-your-own-rule/vocabulary.html##{@term.downcase})"
    end
  end
end

Liquid::Template.register_tag('vocabulary_ref', Jekyll::VocabularyRefTag)
