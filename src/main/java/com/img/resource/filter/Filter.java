package com.img.resource.filter;

import com.img.resource.utils.Image;
import reactor.core.publisher.Mono;

import java.util.concurrent.Executor;

public interface Filter {
    Mono<Image> applyFilter(Image in, Image out, final int PARALLELISM, final Executor executor);
}
