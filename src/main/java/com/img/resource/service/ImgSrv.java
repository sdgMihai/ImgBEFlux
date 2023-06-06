package com.img.resource.service;

import com.img.resource.filter.Filter;
import com.img.resource.utils.Image;
import com.img.resource.utils.ThreadSpecificData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

@Service
@Slf4j
public class ImgSrv {
    public static final BiFunction<List<Filter>, ThreadSpecificData, Mono<Image>> applyFilter = (List<Filter> filters, ThreadSpecificData data) -> {
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImgSrv.class);
        Image inputImage = data.getImage();
        Image outputImage  = data.getNewImage();
        Mono<Image> imageMono = Mono.just(inputImage);

        for (Filter filter : filters) {
            Image finalOutputImage = outputImage;
            imageMono = imageMono.flatMap(image -> filter.applyFilter(image, finalOutputImage, data.getPARALLELISM(), data.getExecutor()));
            // Swap inputImage and outputImage for the next iteration
            Image temp = inputImage;
            inputImage = outputImage;
            outputImage = temp;
        }

        log.debug("!!!!!!after filter operation imgrv applyFilter");
        System.out.println("!!!!!!after filter operation imgrv applyFilter");
        log.debug("image from mono");
        return imageMono;
    };
    @Value("${NUM_THREADS}")
    Integer PARALLELISM;

    @Autowired
    @Qualifier("execFilter")
    private Executor executor;

    public Mono<Image> process(Image image, String filterNames, String filterParams) {
        log.debug("paralellism level set:" + PARALLELISM);
        Image newImage = new Image(image.width - 2, image.height - 2);

        final List<Filter> filters = FilterService.getFilters(filterNames, filterParams);

        Instant start = Instant.now();
        return ImgSrv.applyFilter
                .apply(filters, new ThreadSpecificData(PARALLELISM, image, newImage, executor))
                .map(imagetmp -> {
                    log.debug("processImage function : image has pixel [2 3]" + image.matrix[2][3]);
                    return image;
                })
                .doAfterTerminate(
                        () -> {
                            Duration filterDuration = Duration.between(start, Instant.now());
                            log.debug("image has pixel [2 3]" + newImage.matrix[2][3]);
                            log.debug("active threads after applying Filter in ImgSrv: " + ((ThreadPoolTaskExecutor) executor).getActiveCount());
                            log.info("took filter s:" + filterDuration.getSeconds());
                        }
                );
    }

}
