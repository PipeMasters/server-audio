package com.pipemasters.serveraudio.service.impl;

import com.pipemasters.serveraudio.exceptions.audio.AudioExtractionException;
import com.pipemasters.serveraudio.feign.FileClient;
import com.pipemasters.serveraudio.service.AudioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class AudioServiceImpl implements AudioService {
    private final Logger log = LoggerFactory.getLogger(AudioServiceImpl.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final FileClient fileClient;

    public AudioServiceImpl(FileClient fileClient) {
        this.fileClient = fileClient;
    }

    @Override
    @Transactional(readOnly = true)
    public String extractAudio(String s3Key) {
        String prefix = UUID.randomUUID().toString().substring(0, 8) + "_";
        Path videoFile = null;
        Path audioFile = null;
        try {
            videoFile = Files.createTempFile(prefix + "vid", ".mp4");
            audioFile = Files.createTempFile(prefix + "aud", ".mp3");

            HttpRequest downloadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getDownloadUrl(s3Key)))
                    .timeout(Duration.ofMinutes(15))
                    .GET()
                    .build();

            try (InputStream in = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream()).body()) {
                Files.copy(in, videoFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | InterruptedException e) {
                log.error("Failed to download video file with s3Key={}", s3Key, e);
                throw new AudioExtractionException("Failed to download video", e);
            }

            long downloadedSize = Files.size(videoFile);
            log.info("Downloaded file size for {}: {} bytes", s3Key, downloadedSize);

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-hide_banner",
                    "-loglevel", "error",
                    "-i", videoFile.toString(),
                    "-vn",
                    "-acodec", "libmp3lame",
                    audioFile.toString()
            ).redirectErrorStream(true);

            Process ffmpeg = pb.start();

            CompletableFuture.runAsync(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(ffmpeg.getInputStream()))) {
                    br.lines().forEach(log::info);
                } catch (IOException ignored) {
                }
            });

            if (!ffmpeg.waitFor(4, TimeUnit.MINUTES) || ffmpeg.exitValue() != 0) {
                ffmpeg.destroyForcibly();
                throw new AudioExtractionException("ffmpeg failed or timed out");
            }

            String targetName = s3Key.substring(s3Key.lastIndexOf('/') + 1, s3Key.lastIndexOf('.')) + "_audio.mp3";

            log.debug("Audiofile extracted successfully: {} with duration of {} ms, size of {} bytes, hash {}",
                    targetName, getAudioDuration(audioFile), getFileSize(audioFile), getSha256(audioFile));

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getUploadUrl(s3Key, getAudioDuration(audioFile), getSha256(audioFile))))
                    .timeout(Duration.ofMinutes(15))
                    .PUT(HttpRequest.BodyPublishers.ofFile(audioFile))
                    .build();

            httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());
            return targetName;
        } catch (IOException | InterruptedException e) {
            log.error("Audio extraction failed with s3Key={}", s3Key, e);
            throw new AudioExtractionException("Failed to extract audio", e);
        } finally {
            safeDelete(videoFile);
            safeDelete(audioFile);
        }
    }

    private String getDownloadUrl(String s3Key) {
        return fileClient.getDownloadUrl(s3Key);
    }

    private String getUploadUrl(String s3Key, Long duration, String hash) {
        return fileClient.getUploadUrlAudio(s3Key, duration, hash);
    }

    private static void safeDelete(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }

    private static long getFileSize(Path file) throws IOException {
        return Files.size(file);
    }

    private static String getSha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path)) {
                is.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
            }
            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static long getAudioDuration(Path file) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "ffprobe", "-v", "error", "-show_entries",
                "format=duration", "-of",
                "default=noprint_wrappers=1:nokey=1", file.toString()
        ).redirectErrorStream(true).start();
        try (InputStream is = process.getInputStream()) {
            String output = new String(is.readAllBytes());
            process.waitFor();
            double seconds = Double.parseDouble(output.trim());
            return Math.round(seconds * 1000);
        }
    }
}
