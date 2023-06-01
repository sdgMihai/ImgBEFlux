package com.img.resource.filter;

import com.img.resource.utils.Image;
import com.img.resource.utils.Pixel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

@Slf4j
public class BlackWhiteFilter extends AbstractFilter{

    public void applyFilterPh1(Image image, Image newImage, int start, int stop) {
        Instant startTime = Instant.now();
        for (int i = start; i < stop; ++i) {
            for (int j = 0; j < image.width - 1; ++j) {
                int gray = (int) (0.2126 * image.matrix[i][j].r +
                        0.7152 * image.matrix[i][j].g +
                        0.0722 * image.matrix[i][j].b);
                gray = Math.min(gray, 255);
                newImage.matrix[i][j] = new Pixel((char) gray, (char) gray, (char) gray, image.matrix[i][j].a);
            }
        }
        Duration duration = Duration.between(startTime, Instant.now());
        log.info("time processing " + (stop-start) + " lines of length:" + image.width + " duration:" + duration);
        log.debug("bw filter ph1:" + start);
    }
}
