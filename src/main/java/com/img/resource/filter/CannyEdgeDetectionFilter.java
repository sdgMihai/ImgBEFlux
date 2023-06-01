package com.img.resource.filter;


import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import com.img.resource.utils.Pixel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

@Slf4j
public class CannyEdgeDetectionFilter implements Filter {

    /**
     * @param image       input image reference.
     * @param newImage    output image reference.
     * @param PARALLELISM the async futures that can run in parallel
     */
    @Override
    public void applyFilter(Image image, Image newImage, int PARALLELISM, final Executor executor) {

        BlackWhiteFilter step1 = new BlackWhiteFilter();
        step1.applyFilter(image, newImage, PARALLELISM, executor);
        log.debug("bw filter done");
        log.debug("using executor: " + executor.toString());

        GaussianBlurFilter step2 = new GaussianBlurFilter();
        step2.applyFilter(newImage, image, PARALLELISM, executor);
        log.debug("blur done");

        GradientFilter step3 = new GradientFilter();
        step3.applyFilter(image, newImage, PARALLELISM, executor);
        float[][] auxTheta = step3.theta;
        log.debug("gradient done");

        NonMaximumSuppressionFilter step4 = new NonMaximumSuppressionFilter(auxTheta, step3.thetaHeight, step3.thetaWidth);
        step4.applyFilter(newImage, image, PARALLELISM, executor);
        log.debug("non max supp done");

        DoubleThresholdFilter step5 = new DoubleThresholdFilter();
        step5.applyFilter(image, newImage, PARALLELISM, executor);
        log.debug("double threshold done");

        EdgeTrackingFilter step6 = new EdgeTrackingFilter();
        step6.applyFilter(newImage, image, PARALLELISM, executor);
        log.debug("edge tracking done");

        Pair<Integer, Integer>[] ranges = ImageUtils.getRange(PARALLELISM, image.height);
        CompletableFuture<Void>[] partialFilters2 = new CompletableFuture[PARALLELISM];
        for (int i = 0; i < PARALLELISM; i++) {
            int start = ranges[i].getFirst();
            int stop = ranges[i].getSecond();

            partialFilters2[i] = CompletableFuture.runAsync(
                    () -> applyFilterPh1(image, newImage, start, stop)
                    , executor
            );
        }
//        CompletableFuture.allOf(partialFilters2).join();
        Stream.of(partialFilters2)
                .map(CompletableFuture::join)
                .forEach((Void) -> {
                    log.debug("finish ph2 c-e-d");
                });
        log.debug("c-e-d done");
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
    }
}
