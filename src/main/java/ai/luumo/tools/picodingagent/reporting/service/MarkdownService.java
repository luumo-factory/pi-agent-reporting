package ai.luumo.tools.picodingagent.reporting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.List;

@Service
public class MarkdownService {
    
    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);
    
    private final Parser parser;
    private final HtmlRenderer renderer;
    private final TemplateEngine templateEngine;
    
    public MarkdownService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        
        // Configure CommonMark with extensions
        List<Extension> extensions = Arrays.asList(
            TablesExtension.create(),
            AutolinkExtension.create()
        );
        
        parser = Parser.builder()
            .extensions(extensions)
            .build();
        
        renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build();
    }
    
    /**
     * Converts markdown content to HTML
     */
    public String markdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
    
    /**
     * Wraps rendered markdown in the report template using Thymeleaf
     */
    public String wrapInTemplate(String htmlContent, String title, String date, String owner) {
        Context context = new Context();
        context.setVariable("htmlContent", htmlContent);
        context.setVariable("title", title);
        context.setVariable("date", date);
        context.setVariable("owner", owner);
        
        return templateEngine.process("report-wrapper", context);
    }
}
