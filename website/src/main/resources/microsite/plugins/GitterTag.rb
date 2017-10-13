
module Jekyll
  class GitterTag < Liquid::Tag
    def render(context)
      "[gitter](javascript:window.gitter.chat.defaultChat.toggleChat(true))"
    end
  end
end

Liquid::Template.register_tag('gitter', Jekyll::GitterTag)
