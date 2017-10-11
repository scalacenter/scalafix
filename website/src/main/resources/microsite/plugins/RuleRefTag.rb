module Jekyll
  class RuleRefTag < Liquid::Tag

    def initialize(tag_name, rule, tokens)
      super
      @rule = rule.strip
    end

    def render(context)
      rules = context['site']['data']['rules']['rules']
      rule = rules.detect { |r| r == @rule }
      if rule.nil? then
        raise ArgumentError, "Could not find a rule '#{@rule}' in file rules.yml"
      else
        baseurl = context['site']['baseurl']
        "[#{@rule}](#{baseurl}/docs/rules/#{@rule})"
      end
    end
  end
end

Liquid::Template.register_tag('rule_ref', Jekyll::RuleRefTag)
