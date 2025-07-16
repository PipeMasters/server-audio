package com.pipemasters.serveraudio.service.impl;

import com.pipemasters.serveraudio.exceptions.audio.AudioExtractionException;
import com.pipemasters.serveraudio.feign.FileClient;
import com.pipemasters.serveraudio.service.AudioService;
import feign.Feign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AudioServiceImpl implements AudioService {
    private final Logger log = LoggerFactory.getLogger(AudioServiceImpl.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    @Async("ffmpegExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<String> extractAudio(String s3Key) {
        return CompletableFuture.supplyAsync(() -> {
            String prefix = UUID.randomUUID().toString().substring(0, 8) + "_";
            Path videoFile = null;
            Path audioFile = null;

            try {
                videoFile = Files.createTempFile(prefix + "vid", ".mp4");
                audioFile = Files.createTempFile(prefix + "aud", ".mp3");

                HttpRequest downloadRequest = HttpRequest.newBuilder()
                        .uri(URI.create(getDownloadUrl(s3Key)))
                        .timeout(Duration.ofMinutes(2))
                        .GET()
                        .build();
                try (InputStream in = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream()).body()) {
                    Files.copy(in, videoFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException | InterruptedException e) {
                    log.error("Failed to download video file with s3Key={}", s3Key, e);
                    throw new Error("Failed to download video file with s3Key: ", e);
                }

                Process ffmpeg = new ProcessBuilder(
                        "ffmpeg",
                        "-y",
                        "-i", videoFile.toString(),
                        "-vn",
                        "-acodec", "libmp3lame",
                        audioFile.toString()
                ).redirectErrorStream(true).start();
                try (InputStream ffmpegOut = ffmpeg.getInputStream()) {
                    System.out.println(new String(ffmpegOut.readAllBytes()));
                }
                int exitCode = ffmpeg.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("FFmpeg exited with code " + exitCode);
                }

                String baseName = Paths.get(s3Key).getFileName().toString().replaceFirst("[.][^.]+$", "");
                String targetName = baseName + "_audio.mp3";

                HttpRequest uploadRequest = HttpRequest.newBuilder()
                        .uri(URI.create(getUploadUrl(s3Key)))
//                        .uri(URI.create(getUploadUrl(s3Key.replaceFirst("[.][^.]+$", "") + "_audio.mp3")))
                        .timeout(Duration.ofMinutes(2))
                        .PUT(HttpRequest.BodyPublishers.ofFile(audioFile))
                        .build();
                log.debug("Uploading audio file to URL: {}", uploadRequest.uri());
                httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());
                log.debug("Audio file uploaded successfully: {}", targetName);
                return targetName;
            } catch (IOException | InterruptedException e) {
                log.error("Audio extraction failed with s3Key={}", s3Key == null ? new Error() : s3Key, e);
                throw new AudioExtractionException("Failed to extract audio with s3Key: " + s3Key, e);
            } finally {
                safeDelete(videoFile);
                safeDelete(audioFile);
            }
        });
    }

    private String getDownloadUrl(String s3Key) {
        FileClient fileClient = Feign.builder()
                .target(FileClient.class,
                        "http://localhost:8080");

        return fileClient.getDownloadUrl(s3Key);
    }

    private String getUploadUrl(String s3Key) {
        FileClient fileClient = Feign.builder()
                .target(FileClient.class,
                        "http://localhost:8080");
        return fileClient.getUploadUrlAudio(s3Key);
    }

    private static void safeDelete(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}
