package com.img.resource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
@Slf4j
public class WebFluxImgBeApplication {

    public static void main(String[] args) {
        ReactorDebugAgent.init();
        ApplicationContext context = SpringApplication.run(WebFluxImgBeApplication.class, args);
        log.debug("######################LOGGING FROM MAIN #########");
        String[] beanNames = context.getBeanNamesForAnnotation(org.springframework.web.bind.annotation.RestController.class);
        for (String beanName : beanNames) {
            Class<?> beanType = context.getType(beanName);
            String basePath = extractBasePath(beanType);
            log.debug("Endpoint: " + basePath);
        }

        log.debug("###################### END LOGGING FROM MAIN #########");

    }

    private static String extractBasePath(Class<?> beanType) {
        org.springframework.web.bind.annotation.RequestMapping annotation = beanType.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class);
        if (annotation != null && annotation.value().length > 0) {
            return annotation.value()[0];
        }
        return "/";
    }

    @Bean
    public WebServerFactoryCustomizer<NettyReactiveWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setPort(8081);
    }
}
