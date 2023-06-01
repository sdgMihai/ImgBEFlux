package com.img.resource.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.Executor;

@Data
@AllArgsConstructor
public class ThreadSpecificData {
    final int PARALLELISM;
    Image image;
    Image newImage;
    final Executor executor;
}
