package com.img.resource.web;

import com.img.resource.error.InternalServerErrorException;
import com.img.resource.service.ImageFormatIO;
import com.img.resource.service.ImgSrv;
import com.img.resource.utils.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@CrossOrigin(value = { "http://localhost" },
        maxAge = 900
)
@RequestMapping("/api")
@RestController
public class Controller {
    private final ImgSrv imgSrv;
    private final ImageFormatIO imageFormatIO;

    @Autowired
    public Controller(ImgSrv imgSrv, ImageFormatIO imageFormatIO) {
        this.imgSrv = imgSrv;
        this.imageFormatIO = imageFormatIO;
    }

    @PostMapping(value = "/filter", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<ResponseEntity<byte[]>> filterImage(@RequestParam("filter") String filter
            , @RequestPart("file") FilePart request) { //
        log.debug("start image http req processing");
        if (request == null) {
            return retBadRequest(new Throwable("null request"));
        }
        return
                mapRequestInput(request, filter, null)
                .map(this::mapImageToMono)
                .flatMap(this::processImage)
                .map(this::mapProcessedImageToMono)
                .onErrorResume(InternalServerErrorException.class, e ->
                        Mono.just(ResponseEntity
                                .internalServerError().build()
                                ));
    }

    private Mono<byte[]> getImageBytes(FilePart request) {
        return request.content().reduce(DataBuffer::write)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }

    private Mono<Request> mapRequestInput(FilePart request, String filter, String level) {
        return getImageBytes(request)
                .map(bytes -> new Request( filter, level, bytes));
    }

    private Mono<ResponseEntity<byte[]>> retBadRequest(Throwable throwable) {
        log.error(throwable.getMessage());
        return Mono.just(
                ResponseEntity.badRequest()
                        .build()
        );
    }

    private PipeRequest mapImageToMono(Request input) throws InternalServerErrorException {
        byte[] image = input.image();
        String filter = input.filter();
        String level = input.level();

        String filterName = null;
        if (filter != null) {
            filterName = filter;
        } else throw new IllegalArgumentException("filter cannot be empty");
        String filterParams = null;
        if (level != null) {
            filterParams = level;
        }

        log.debug("image has length:" + image.length);
        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException e) {
            e.printStackTrace();
            log.debug(e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }

        final Image inImage = imageFormatIO.bufferedToModelImage(bufferedImage);

        return new PipeRequest(inImage, filterName, filterParams);
    }

    private Mono<Image> processImage(PipeRequest pipeRequest) {
        return imgSrv.process(pipeRequest.image()
                , pipeRequest.filterName()
                , pipeRequest.filterParams())
                .doAfterTerminate(() -> log.debug("processImage function termination"));
    }


    private ResponseEntity<byte[]> mapProcessedImageToMono(Image image2)  throws InternalServerErrorException {
        BufferedImage image1;
        image1 = imageFormatIO.modelToBufferedImage(image2);
        log.debug("buffered result image obtained");

        final byte[] bytes;
        try {
            bytes = imageFormatIO.bufferedToByteArray(image1);
        } catch (IOException e) {
            e.printStackTrace();
            log.debug(e.getMessage());
            throw new InternalServerErrorException(e.getMessage());
        }
        log.debug("bytes result image obtained");

        log.debug("imaged has been saved");
        return ResponseEntity.ok(bytes);
    }
}

record Request(String filter, String level, byte[] image) {
}

record PipeRequest(Image image, String filterName, String filterParams) {}