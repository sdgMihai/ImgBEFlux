package com.img.resource.filter;

import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import com.img.resource.utils.Pixel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;

@Slf4j
public class DoubleThresholdFilter implements Filter{

    /**
     * @param in          input image reference.
     * @param out         output image reference.
     * @param PARALLELISM integer value denoting the number of task running in parallel.
     * @return
     */
    @Override
    public Mono<Image> applyFilter(Image in, Image out, final int PARALLELISM, final Executor executor) {
        Pair<Integer, Integer>[] ranges = ImageUtils.getRange(PARALLELISM, in.height);

        return Flux.fromArray(ranges)
                // ph1
                .flatMap(range -> Mono.fromCallable(() -> applyFilterPh1(in, range.getFirst(), range.getSecond()))
                        .subscribeOn(Schedulers.fromExecutor(executor)))
                .reduce(Math::max)
                // ph2v
                .flatMap((Float maxVal) -> Flux.fromArray(ranges)
                        .flatMap(range -> Mono.fromRunnable(() -> applyFilterPh2(in, out, range.getFirst(), range.getSecond(), maxVal))
                                .subscribeOn(Schedulers.fromExecutor(executor)).then()
                        )
                        .then(Mono.just(out))
                );
    }

    public float applyFilterPh1(Image image, int start, int stop) {
        float threadMaxVal = -3.40282347e+38F;
        for (int i = start; i < stop; ++i) {
            for (int j = 1; j < image.width - 1; ++j) {
                threadMaxVal = (threadMaxVal < image.matrix[i][j].r) ? image.matrix[i][j].r : threadMaxVal;
            }
        }
        log.debug("double th ph 1");
        return threadMaxVal;
    }

    public void applyFilterPh2(Image image, Image newImage, int start, int stop, float maxVal) {
        float thresholdHigh = 0.06f;
        float high = maxVal * thresholdHigh;
        float thresholdLow = 0.05f;
        float low = high * thresholdLow;

        for (int i = start; i < stop; ++i) {
            for (int j = 1; j < image.width - 1; ++j) {
                if (image.matrix[i][j].r >= high) {
                    newImage.matrix[i][j] = new Pixel((char) 255, (char) 255, (char) 255, image.matrix[i][j].a);
                } else {
                    if (image.matrix[i][j].r >= low) {
                        newImage.matrix[i][j] = new Pixel((char) 100, (char) 100, (char) 100, image.matrix[i][j].a);
                    } else newImage.matrix[i][j] = new Pixel((char) 0, (char) 0, (char) 0, image.matrix[i][j].a);
                }
            }
        }
        log.debug("double th ph 2");
    }
}
