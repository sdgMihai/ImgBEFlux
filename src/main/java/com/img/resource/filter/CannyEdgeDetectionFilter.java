package com.img.resource.filter;


import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import com.img.resource.utils.Pixel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Mono;

import java.util.concurrent.Executor;

@Slf4j
public class CannyEdgeDetectionFilter implements Filter {

    /**
     * @param image       input image reference.
     * @param newImage    output image reference.
     * @param PARALLELISM the async futures that can run in parallel
     * @return
     */
    @Override
    public Mono<Image> applyFilter(Image image, Image newImage, int PARALLELISM, final Executor executor) {
        BlackWhiteFilter step1 = new BlackWhiteFilter();
        GaussianBlurFilter step2 = new GaussianBlurFilter();
        GradientFilter step3 = new GradientFilter();
        DoubleThresholdFilter step5 = new DoubleThresholdFilter();
        EdgeTrackingFilter step6 = new EdgeTrackingFilter();
        Pair<Integer, Integer>[] ranges = ImageUtils.getRange(PARALLELISM, image.height);
        return step1
                .applyFilter(image, newImage, PARALLELISM, executor)
                .doAfterTerminate(() -> {
                    log.debug("bw filter done");
                    System.out.println("teeeeeeeeest");
                    log.debug("using executor: " + executor.toString());
                })
                .flatMap((bwImage) -> {
                    log.debug(bwImage.matrix[1][1] + " -> check bw image");
                    return step2.applyFilter(bwImage, image, PARALLELISM, executor);
                })
                .map((tmpImage) -> {
                    log.debug("blur done" + image.matrix[2][3]);
                    return tmpImage;
                })
                .flatMap((gImage) -> step3.applyFilter(gImage, newImage, PARALLELISM, executor))
                .doAfterTerminate(() -> log.debug("gradient done"))
                .flatMap((gradImage) -> {
                    float[][] auxTheta = step3.theta;
                    log.debug("debugging gradient data sent to nms:");
                    log.debug(" step 3 theta height:" + step3.thetaHeight);
                    log.debug("step3 theta width:" + step3.thetaWidth);
                    log.debug("step3 auxTheta rnd float" + auxTheta[2][2]);
                    NonMaximumSuppressionFilter step4 = new NonMaximumSuppressionFilter(auxTheta, step3.thetaHeight, step3.thetaWidth);
                    return step4.applyFilter(gradImage, image, PARALLELISM, executor);
                });
//                .doAfterTerminate(() -> log.debug("non max supp done"))
//                .flatMap((nmsImage) -> step5.applyFilter(nmsImage, newImage, PARALLELISM, executor));
//                .map(
//                        (imgetmp) -> {
//                            log.debug("double threshold done");
//                            return imgetmp;
//                        }
//                   )
//                .flatMap((dtImage) -> step6.applyFilter(dtImage, image, PARALLELISM, executor));
//                .doAfterTerminate(() -> {
//                        System.out.println("!!!!!!!!!edge tracking done");
//                        log.debug("!!!!!!!!!edge tracking done");
//                }).flatMap(result -> {
//                            log.debug("swapping in ced final step!!");
//                            return Flux.fromArray(ranges)
//                                    .flatMap(range -> Mono.fromRunnable(() ->
//                                                    applyFilterPh1(result, newImage, range.getFirst(), range.getSecond())
//                                            ).subscribeOn(Schedulers.fromExecutor(executor))
//                                    )
//                                    .then(Mono.just(newImage));
//                        }
//
//                );
    }

    public void applyFilterPh1(Image image, Image newImage, int start, int stop) {
        log.debug("final step of c-e-d");
        for (int i = start; i < stop; ++i) {
            final Pixel[] swp = image.matrix[i];
            image.matrix[i] = newImage.matrix[i];
            newImage.matrix[i] = swp;
            for (int j = 1; j < image.width - 1; ++j) {
                if (newImage.matrix[i][j].r < 100) {
                    newImage.matrix[i][j] = new Pixel((char) 0, (char) 0, (char) 0, newImage.matrix[i][j].a);
                }
            }
        }
        log.debug ("last step ph1 ced" + newImage.matrix[2][3]);
    }
}
