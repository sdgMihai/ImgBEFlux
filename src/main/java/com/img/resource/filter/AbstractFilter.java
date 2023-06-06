package com.img.resource.filter;

import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

@Slf4j
public class AbstractFilter implements Filter{

    /**
     * @param in          input image reference.
     * @param out         output image reference.
     * @param PARALLELISM integer value denoting the number of task running in parallel.
     * @return Mono<Image> The processed Image
     */
    public Mono<Image> applyFilter(Image in, Image out, final int PARALLELISM, final Executor executor) {
        Pair<Integer, Integer>[] ranges = ImageUtils.getRange(PARALLELISM, in.height);
        return Flux.fromArray(ranges)
                .flatMap(range -> Mono.fromRunnable(() ->
                                applyFilterPh1(in, out, range.getFirst(), range.getSecond())
                        ).subscribeOn(Schedulers.fromExecutor(executor))
                )
                .then(Mono.just(out));
    }

    /**
     * @param image    input image reference.
     * @param newImage output image reference.
     * @param start    first line to be processed from input image.
     * @param stop     past last line to be processed from input image.
     */
    public void applyFilterPh1(Image image, Image newImage, int start, int stop) {
        log.debug("do not enter here");
    }
}
