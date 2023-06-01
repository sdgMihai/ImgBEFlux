package com.img.resource.web;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@CrossOrigin(value = { "http://localhost" },
        maxAge = 900
)
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
    public Mono<ResponseEntity<byte[]>> filterImage(@RequestParam("filter") String filter,
                                                    @RequestParam("level") String level,
                                                    @RequestPart("file") FilePart request) { //
        log.debug("debug message in image filter !");
        if (request == null) {
            return Mono.just(
                    ResponseEntity.badRequest().build()
            );// .body("No file was uploaded.")
        }
//        String userId = principal.getClaimAsString("sub");

        return    request.content().reduce(DataBuffer::write)
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return bytes;
                    })
                .publishOn(Schedulers.boundedElastic())
                    .map(image -> {

                        final Mono<ResponseEntity<byte[]>> internalServerError = Mono.just(
                                ResponseEntity.internalServerError().body(new byte[0])
                        );

                        try {
                            String filterName = null;
                            if (filter != null) {
                                filterName = filter;
                            }
                            String filterParams = null;
                            if (level != null) {
                                filterParams = level;
                            }
                            assert (image.length != 0);
                            BufferedImage bufferedImage;
                            bufferedImage = ImageIO.read(new ByteArrayInputStream(image));

                            final Image input = imageFormatIO.bufferedToModelImage(bufferedImage);
                            final Mono<Image> res;
                            imgSrv.process(input, filterName, filterParams)
                                    .map(image2 -> {
                                        log.debug("future of res obtained");
                                        BufferedImage image1;
                                        image1 = imageFormatIO.modelToBufferedImage(image2);
                                        log.debug("buffered result image obtained");
                                        final byte[] bytes;
                                        bytes = imageFormatIO.bufferedToByteArray(image1);
                                        log.debug("bytes result image obtained");
                                        return bytes;
                                    }).subscribe();




                            log.debug("imaged has been saved");
                            return Mono.just(ResponseEntity.ok(bytes));
                        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
                            e.printStackTrace();
                            log.debug(e.getMessage());
                            return internalServerError;
                        }


                        return Mono.just(
                                ResponseEntity.badRequest()
                                        .body(new byte[0])
                        );
                    }).defaultIfEmpty(Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping(value = "/filter/ret", produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<ResponseEntity<byte[]>> filterImage(@RequestParam("ord_id") String ordId) {

    }

}
