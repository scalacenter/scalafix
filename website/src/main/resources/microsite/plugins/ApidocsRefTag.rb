module Jekyll
  class ApidocsRefTag < Liquid::Tag

    def initialize(tag_name, qualified_name, tokens)
      super
      @qualified_name = qualified_name.strip
    end

    def render(context)
      baseurl = context['site']['baseurl']
      docsurl = context['site']['docsUrl']
      path = @qualified_name.gsub('.', '/').concat('.html')
      name = @qualified_name.split('.').last
      "[#{name}](#{baseurl}/#{docsurl}/api/#{path})"
    end
  end
end

Liquid::Template.register_tag('apidocs_ref', Jekyll::ApidocsRefTag)
