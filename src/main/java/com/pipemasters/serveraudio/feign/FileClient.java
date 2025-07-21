package com.pipemasters.serveraudio.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "file-service", url = "${server-main.url}")
public interface FileClient {
    @PostMapping("/api/v1/files/upload-url-audio")
    String getUploadUrlAudio(@RequestParam("sourceKey") String sourceKey,
                             @RequestParam("duration") Long duration,
                             @RequestParam("hash") String hash);

    @GetMapping("/api/v1/files/download-url")
    String getDownloadUrl(@RequestParam("sourceKey") String sourceKey);
}