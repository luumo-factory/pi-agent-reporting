package ai.luumo.tools.picodingagent.reporting.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static assets (CSS, JS, images) - cache for 1 hour but allow revalidation
        registry.addResourceHandler("/css/**", "/js/**", "/img/**", "/favicon.*")
                .addResourceLocations("classpath:/static/css/", "classpath:/static/js/", 
                                     "classpath:/static/img/", "classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).mustRevalidate());
        
        // PWA files (manifest) - no cache (always check for updates)
        registry.addResourceHandler("/manifest.json")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().noStore().mustRevalidate());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // HTML pages - no cache (always check for updates)
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.addCacheMapping(
            CacheControl.noCache().noStore().mustRevalidate(), 
            "/**/*.html", "/"
        );
        registry.addInterceptor(interceptor);
    }
}
