package com.img.resource.filter;

import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Slf4j
public class AbstractFilter implements Filter{

    /**
     * @param in          input image reference.
     * @param out         output image reference.
     * @param PARALLELISM integer value denoting the number of task running in parallel.
     */
    public void applyFilter(Image in, Image out, final int PARALLELISM, final Executor executor) {
        CompletableFuture<Void>[] partialFilters = new CompletableFuture[PARALLELISM];
        Pair<Integer, Integer>[] ranges = ImageUtils.getRange(PARALLELISM, in.height);
        for (int i = 0; i < PARALLELISM; i++) {
            int start = ranges[i].getFirst();
            int stop = ranges[i].getSecond();
            partialFilters[i] = CompletableFuture.runAsync(
                    () -> applyFilterPh1(in, out, start, stop)
                    , executor
            );
        }
//        CompletableFuture.allOf(partialFilters).join();
        Stream.of(partialFilters)
                .map(CompletableFuture::join)
                .forEach((Void) -> {
                    log.debug("finish abstract");
                });
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
