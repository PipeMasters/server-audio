package com.pipemasters.serveraudio.feignTest;

import com.pipemasters.serveraudio.feign.FileClient;
import feign.Feign;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FeignTests {
    @Value("${server-main.url}")
    private String mainServerUrl;

    @Test
    @Disabled
    public void givenS3key_shouldReturnUploadUrl() {
        String s3Key = "5e4e313d-c040-4031-aa63-70d36d823082/caticks.mp4";
        FileClient fileClient = Feign.builder()
                .target(FileClient.class, mainServerUrl);

        String uploadUrl = fileClient.getUploadUrlAudio(s3Key);

        System.out.println(uploadUrl);
    }

    @Test
    @Disabled
    public void givenS3key_shouldReturnDownloadUrl() {
        System.out.println(mainServerUrl);

        String s3Key = "5e4e313d-c040-4031-aa63-70d36d823082/caticks.mp4";
        FileClient fileClient = Feign.builder()
                .target(FileClient.class, mainServerUrl);
        String downloadUrl = fileClient.getDownloadUrl(s3Key);

        System.out.println(downloadUrl);
    }
}
