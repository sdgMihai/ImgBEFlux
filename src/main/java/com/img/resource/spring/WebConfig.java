package com.img.resource.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
@ComponentScan(basePackages = { "com.img.resource" })
@Slf4j
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
//        DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
//        partReader.setMaxParts(1);
//        partReader.setMaxDiskUsagePerPart(10L * 1024L);
//        partReader.setEnableLoggingRequestDetails(true);
//
//        MultipartHttpMessageReader multipartReader = new MultipartHttpMessageReader(partReader);
//        multipartReader.setEnableLoggingRequestDetails(true);
//
//        configurer.defaultCodecs().multipartReader(multipartReader);
        configurer.defaultCodecs().maxInMemorySize(1024 * 1024 * 10);
    }

}
