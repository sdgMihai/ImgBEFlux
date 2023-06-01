package com.img.resource.service;

import com.img.resource.filter.Filter;
import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import com.img.resource.utils.ThreadSpecificData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

@Service
@Slf4j
public class ImgSrv {
    public static final BiConsumer<List<Filter>, ThreadSpecificData> applyFilter = (List<Filter> filters, ThreadSpecificData data) -> {
        final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImgSrv.class);
        ListIterator<Filter> filterIt = filters.listIterator();
        while (filterIt.hasNext()) {
            Filter filter = filterIt.next();
            log.debug("applying " + filter.toString() + "filter");
            filter.applyFilter(data.getImage(), data.getNewImage(), data.getPARALLELISM(), data.getExecutor());
            log.debug("filter applied");
            if (filterIt.hasNext()) {
                ImageUtils.swap(data.getImage(), data.getNewImage(), data.getPARALLELISM());
            }
        }
    };
    @Value("${NUM_THREADS}")
    Integer PARALLELISM;

    public Mono<Image> process(Image image, String filterNames, String filterParams) {
        log.debug("paralellism level set:" + PARALLELISM);
        Image newImage = new Image(image.width - 2, image.height - 2);

        // todo filterNames filterParams now aren't anymore arrays
        final List<Filter> filters = FilterService.getFilters(filterNames, filterParams);

        Instant start = Instant.now();
        ImgSrv.applyFilter
                .accept(filters, new ThreadSpecificData(PARALLELISM, image, newImage, executor));
        Duration filterDuration = Duration.between(start, Instant.now());
        log.debug("active threads after applying Filter in ImgSrv: " + ((ThreadPoolTaskExecutor)executor).getActiveCount());
        log.info("took filter s:"+ filterDuration.getSeconds());
        return CompletableFuture.completedFuture(newImage);
    }

}
