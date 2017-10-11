module Jekyll
  class RuleRefTag < Liquid::Tag

    def initialize(tag_name, docTitle, tokens)
      super
      @docTitle, @hash = docTitle.split(',').map(&:strip)
    end

    def render(context)
      rules = context['site']['data']['rules']['rules']
      rule = rules.detect { |r| r == @docTitle }
      if rule.nil? then
        raise ArgumentError, "Could not find a rule '#{@docTitle}' in file rules.yml"
      else
        name = @docTitle
        baseurl = context['site']['baseurl']
        "[#{name}](#{baseurl}/docs/rules/#{name})"
      end
    end
  end
end

Liquid::Template.register_tag('rule_ref', Jekyll::RuleRefTag)
