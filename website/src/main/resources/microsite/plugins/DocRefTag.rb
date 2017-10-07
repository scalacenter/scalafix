module Jekyll
  class DocRefTag < Liquid::Tag

    def initialize(tag_name, docTitle, tokens)
      super
      @docTitle = docTitle.strip
    end

    def render(context)
      options = context['site']['data']['menu']['options']
      flat_options = options.reduce([]) do |flat_options, opt|
        if opt.key?('nested_options') then
          flat_options.concat(opt['nested_options'])
        end
        flat_options << opt
      end
      baseurl = context['site']['baseurl']
      opt = flat_options.detect { |o| o['title'] == @docTitle }
      if opt.nil? then
        raise ArgumentError, "Could not find a document titled '#{@docTitle}'"
      else
        "[#{opt['title']}](#{baseurl}/#{opt['url']})"
      end
    end
  end
end

Liquid::Template.register_tag('doc_ref', Jekyll::DocRefTag)
