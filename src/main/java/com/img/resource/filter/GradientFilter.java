package com.img.resource.filter;

import com.img.resource.utils.Image;
import com.img.resource.utils.ImageUtils;
import com.img.resource.utils.Pixel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Date;
import java.util.concurrent.Executor;

@Slf4j
public class GradientFilter implements Filter {
    public float[][] theta; /* place to save theta calculation */
    public int thetaHeight;
    public int thetaWidth;
    private static final float[][] Gx = new float[][]{{-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}};

    private static final float[][] Gy = new float[][]{{1, 2, 1},
            {0, 0, 0},
            {-1, -2, -1}};

    private float[][] Ix;
    private float[][] Iy;

    /**
     * @param in          input image reference.
     * @param out         output image reference.
     * @param PARALLELISM integer value denoting the number of task running in parallel.
     * @return
     */
    @Override
    public Mono<Image> applyFilter(Image in, Image out, final int PARALLELISM, final Executor executor) {
        Ix = new float[in.height][];
        Iy = new float[in.height][];
        theta = new float[in.height][];

        for (int i = 0; i < in.height; ++i) {
            Ix[i] = new float[in.width];
            Iy[i] = new float[in.width];
            theta[i] = new float[in.width];
        }

        this.thetaHeight = in.height;
        this.thetaWidth = in.width;
        log.debug("within gradient");
        log.debug("Running: " + Thread.currentThread().getId() + " | " + new Date());

        // ph1
        Pair<Integer, Integer>[] ranges = ImageUtils.getRange(PARALLELISM, in.height);

        return Flux.fromArray(ranges)
                // ph1
                .flatMap(range -> Mono.fromCallable(() ->
                                applyFilterPh1(in, out, range.getFirst(), range.getSecond()))
                        .subscribeOn(Schedulers.fromExecutor(executor)))
                .reduce(Math::max)
                // ph2
                .flatMap((Float gMax) -> Flux.fromArray(ranges)
                        .flatMap(range -> Mono.fromRunnable(() ->
                                        applyFilterPh2(in, out, range.getFirst(), range.getSecond(), gMax))
                                        .subscribeOn(Schedulers.fromExecutor(executor)).then()
                        )
                        .then(Mono.just(out))
                );
    }

    public float applyFilterPh1(Image image, Image newImage, int start, int stop) {
        // 1. Se aplica kernelul Gx pe imagine si se obtine Ix
        log.debug("applying filter gradient phase 1");
        log.debug("Running: " + Thread.currentThread().getId() + " | " + new Date());

        for (int i = start; i < stop; ++i) {
            for (int j = 1; j < image.width - 1; ++j) {
                float gray = 0;

                for (int ki = -1; ki <= 1; ++ki) {
                    for (int kj = -1; kj <= 1; ++kj) {
                        gray += (float) (image.matrix[i + ki][j + kj].r) * Gx[ki + 1][kj + 1];
                    }
                }
                Ix[i][j] = gray;
            }
        }

        // 2. Se aplica kernelul Gy pe imagine si se obtine Iy
        for (int i = start; i < stop; ++i) {
            for (int j = 1; j < image.width - 1; ++j) {
                float gray = 0;

                for (int ki = -1; ki <= 1; ++ki) {
                    for (int kj = -1; kj <= 1; ++kj) {
                        gray += (float) (image.matrix[i + ki][j + kj].r) * Gy[ki + 1][kj + 1];
                    }
                }
                Iy[i][j] = gray;
            }
        }

        float threadgMax = -3.40282347e+38F;
        // 3. Se calculeaza G = sqrt(Gx**2 + Gy**2) pe fiecare element, se foloseste Ix ca depozit
        // Se calculeaza theta = arctangenta(Iy, Ix) pe fiecare element
        for (int i = start; i < stop; ++i) {
            for (int j = 1; j < image.width - 1; ++j) {
                float gray;
                gray = (float)Math.sqrt(Ix[i][j] * Ix[i][j] + Iy[i][j] * Iy[i][j]);
                if (threadgMax < gray) {
                    threadgMax = gray;
                }
                this.theta[i][j] = (float) (Math.atan2(Iy[i][j], Ix[i][j]) * 180 / Math.PI);
                Ix[i][j] = gray;
                if (this.theta[i][j] < 0) {
                    this.theta[i][j] =  this.theta[i][j] + 180;
                }
            }
        }
        return threadgMax;
    }

    public void applyFilterPh2(Image image, Image newImage, int start, int stop, float gMax) {
//        log.debug("applying filter gradient phase 2");
        // 4. Se calculeaza G = G / G.max() * 255
        for (int i = start; i < stop; ++i) {
            for (int j = 1; j < image.width - 1; ++j) {
                float gray = Ix[i][j];
                gray = (gray / gMax) * 255;
                gray = (gray < 0) ? 0 : gray;
                gray = (gray > 255) ? 255 : gray;
                newImage.matrix[i][j] = new Pixel((char) gray, (char) gray, (char) gray, image.matrix[i][j].a);
            }
        }
        log.debug("Running: " + Thread.currentThread().getId() + " | " + new Date());
        log.debug("prepare finish grad phase 2");
    }
}
