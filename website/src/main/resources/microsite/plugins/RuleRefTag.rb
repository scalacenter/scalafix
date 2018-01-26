module Jekyll
  class RuleRefTag < Liquid::Tag

    def initialize(tag_name, rule, tokens)
      super
      @rule = rule.strip
    end

    def render(context)
      baseurl = context['site']['baseurl']
      "[#{@rule}](#{baseurl}/docs/rules/#{@rule})"
    end
  end
end

Liquid::Template.register_tag('rule_ref', Jekyll::RuleRefTag)
