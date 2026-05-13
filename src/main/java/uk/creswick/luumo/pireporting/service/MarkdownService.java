package uk.creswick.luumo.pireporting.service;

import lombok.extern.slf4j.Slf4j;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class MarkdownService {
    
    private final Parser parser;
    private final HtmlRenderer renderer;
    
    public MarkdownService() {
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
     * Wraps rendered markdown in the report template
     */
    public String wrapInTemplate(String htmlContent, String title, String date, String owner) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>%s – %s</title>
              <link rel="stylesheet" href="/css/report-theme.css" />
              <script defer src="/js/report-theme.js"></script>
            </head>
            <body>
              <div class="report-page">
                <header class="report-header">
                  <div class="badge">report</div>
                  <h1>%s</h1>
                  <p class="meta">
                    <strong>Date:</strong> %s · <strong>Owner:</strong> %s
                  </p>
                </header>
                <div class="report-section markdown-content">
                  %s
                </div>
              </div>
            </body>
            </html>
            """.formatted(date, title, title, date, owner, htmlContent);
    }
}
