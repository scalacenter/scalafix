module Jekyll
  class DocRefTag < Liquid::Tag

    def initialize(tag_name, docTitle, tokens)
      super
      @docTitle, @hash = docTitle.split(',').map(&:strip)
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
        hash = if @hash.nil? then "" else "##{@hash.gsub(' ', '-').downcase}" end
        name = @hash || opt['title']
        "[#{name}](#{baseurl}/#{opt['url']}#{hash})"
      end
    end
  end
end

Liquid::Template.register_tag('doc_ref', Jekyll::DocRefTag)
