package com.pipemasters.serveraudio.feignTest;

import com.pipemasters.serveraudio.feign.FileClient;
import feign.Feign;
import org.junit.jupiter.api.Test;

public class FeignTests {

    @Test
    public void givenS3key_shouldReturnUploadUrl() {
        String s3Key = "5e4e313d-c040-4031-aa63-70d36d823082/caticks.mp4";
        FileClient fileClient = Feign.builder()
                .target(FileClient.class,
                        "http://localhost:8080");

        String uploadUrl = fileClient.getUploadUrlAudio(s3Key);

        System.out.println(uploadUrl);
    }

    @Test
    public void givenS3key_shouldReturnDownloadUrl() {
        String s3Key = "5e4e313d-c040-4031-aa63-70d36d823082/caticks.mp4";
        FileClient fileClient = Feign.builder()
                .target(FileClient.class,
                        "http://localhost:8080");
        String downloadUrl = fileClient.getDownloadUrl(s3Key);

        System.out.println(downloadUrl);
    }
}
