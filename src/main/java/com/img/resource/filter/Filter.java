package com.img.resource.filter;

import com.img.resource.utils.Image;

import java.util.concurrent.Executor;

public interface Filter {
    void applyFilter(Image in, Image out, final int PARALLELISM, final Executor executor);
}
