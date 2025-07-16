package com.pipemasters.serveraudio.service;

import java.util.concurrent.CompletableFuture;

public interface AudioService {
    CompletableFuture<String> extractAudio (String s3Key);
}
