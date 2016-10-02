package me.jiangcai.wxtest.config;

import me.jiangcai.wx.MessageReply;
import me.jiangcai.wx.SingleAccountSpringConfig;
import me.jiangcai.wx.classic.ClassicMessageReply;
import me.jiangcai.wx.web.WeixinWebSpringConfig;
import me.jiangcai.wx.web.thymeleaf.WeixinDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * @author CJ
 */
@Configuration
@Import({MyConfig.Pre.class, SingleAccountSpringConfig.class, WeixinWebSpringConfig.class, MyConfig.Config.class})
@ComponentScan("me.jiangcai.wxtest.controller")
@EnableWebMvc
public class MyConfig extends WebMvcConfigurerAdapter {

    @Bean
    public MessageReply messageReply() {
        return new ClassicMessageReply();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        super.addViewControllers(registry);
        registry.addViewController("/js.html")
                .setViewName("js.html");
    }

    @Configuration
    @PropertySource("classpath:/demo.properties")
    static class Pre {

    }

    @Configuration
    @EnableWebMvc
    @Import(Config.ThymeleafConfig.class)
    static class Config extends WebMvcConfigurerAdapter {

        @Autowired
        private ThymeleafViewResolver thymeleafViewResolver;

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            super.addViewControllers(registry);
            registry.addViewController("/js")
                    .setViewName("js.html");
        }

        @Override
        public void configureViewResolvers(ViewResolverRegistry registry) {
            super.configureViewResolvers(registry);
            registry.viewResolver(thymeleafViewResolver);
        }

        @Import(ThymeleafConfig.ThymeleafTemplateConfig.class)
        static class ThymeleafConfig {
            @Autowired
            private TemplateEngine engine;

            @Bean
            private ThymeleafViewResolver thymeleafViewResolver() {
                ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
                viewResolver.setCache(false);
                viewResolver.setTemplateEngine(engine);
                viewResolver.setCharacterEncoding("UTF-8");
                viewResolver.setContentType("text/html;charset=UTF-8");
                return viewResolver;
            }

            static class ThymeleafTemplateConfig {
                @Autowired
                private WebApplicationContext webApplicationContext;
                @Autowired
                private WeixinDialect weixinDialect;

                @Bean
                public TemplateEngine templateEngine() {
                    SpringTemplateEngine engine = new SpringTemplateEngine();
                    engine.setEnableSpringELCompiler(true);
                    engine.setTemplateResolver(templateResolver());
                    engine.addDialect(weixinDialect);
                    return engine;
                }

                private ITemplateResolver templateResolver() {
                    SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
                    resolver.setCacheable(false);
                    resolver.setApplicationContext(webApplicationContext);
                    resolver.setCharacterEncoding("UTF-8");
                    resolver.setPrefix("/");
                    resolver.setTemplateMode(TemplateMode.HTML);
                    return resolver;
                }
            }

        }
    }
}
