package com.pipemasters.serveraudio.service.impl;

import com.pipemasters.serveraudio.exceptions.audio.AudioExtractionException;
import com.pipemasters.serveraudio.service.AudioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
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
public class AudioServiceImpl
//        implements AudioService
{
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String minioBucketName;

    private final Logger log = LoggerFactory.getLogger(AudioServiceImpl.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public AudioServiceImpl(S3Presigner s3Presigner, S3Client s3Client, String minioBucketName) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.minioBucketName = minioBucketName;
    }

    //    @Override
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
//            log.error("Failed to download video file for mediaFile={}", mediaFileId, e);
                    throw new Error("Failed to download video file for mediaFile: ", e);
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

//                String sourceFileName = source.getFilename().substring(0, source.getFilename().lastIndexOf('.'));
                String baseName = Paths.get(s3Key).getFileName().toString().replaceFirst("[.][^.]+$", "");
                String targetName = baseName + "_audio.mp3";


//                FileUploadRequestDto uploadDto = new FileUploadRequestDto(
//                        source.getUploadBatch().getId(), targetName, FileType.AUDIO, mediaFileId);

                HttpRequest uploadRequest = HttpRequest.newBuilder()
                        .uri(URI.create(getUploadUrl(s3Key + "_audio.mp3")))
                        .timeout(Duration.ofMinutes(2))
                        .PUT(HttpRequest.BodyPublishers.ofFile(audioFile))
                        .build();
                log.debug("Uploading audio file to URL: {}", uploadRequest.uri());
                httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());
                log.debug("Audio file uploaded successfully: {}", targetName);
                return "i will fix this i promise";

            } catch (IOException | InterruptedException e) {
                log.error("Audio extraction failed for mediaFile={}", "mediaFileId", e);
                throw new AudioExtractionException("Failed to extract audio for mediaFile: " + "mediaFileId", e);
            } finally {
                safeDelete(videoFile);
                safeDelete(audioFile);
            }
        });
    }

    //    @Override
    public void processUploadedVideo(String uuid, String filename) {

    }

    public String test() throws IOException, InterruptedException {
        String s3Key = "81e10071-e297-4919-92c2-51bf019ff8c6/meow";
//        String prefix = UUID.randomUUID().toString().substring(0, 8) + "_";
        String prefix = "";
        Path videoFile = Files.createTempFile(prefix + "vid", ".mp4");
        Path audioFile = Files.createTempFile(prefix + "aud", ".mp3");
//        File audioFile = new File("C://test_files/meowAudio.mp3");
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                .bucket(minioBucketName)
//                .key(s3Key)
//                .build();
//        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                .signatureDuration(Duration.ofMinutes(10))
//                .getObjectRequest(getObjectRequest)
//                .build();
//        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(getDownloadUrl(s3Key)))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        try (InputStream in = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream()).body()) {
            Files.copy(in, videoFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("               " + videoFile.toAbsolutePath());
        } catch (IOException | InterruptedException e) {
//            log.error("Failed to download video file for mediaFile={}", mediaFileId, e);
            throw new Error("Failed to download video file for mediaFile: ", e);
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


        return "xui";
    }


    private String getDownloadUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(minioBucketName)
                .key(s3Key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    private String getUploadUrl(String s3Key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(minioBucketName)
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

//    @Override
//    @Async("ffmpegExecutor")
//    @Transactional(readOnly = true)
//    public CompletableFuture<String> extractAudio(Long mediaFileId) {
//
//        return CompletableFuture.supplyAsync(() -> {
//
//            MediaFile source = mediaFileRepository.findById(mediaFileId)
//                    .orElseThrow(() -> new MediaFileNotFoundException("Media file not found: " + mediaFileId));
//
//            Path videoFile = null;
//            Path audioFile = null;
//            try {
//                String prefix = UUID.randomUUID().toString().substring(0, 8) + "_";
//                videoFile = Files.createTempFile(prefix + "vid", ".tmp");
//
//                HttpRequest downloadRequest = HttpRequest.newBuilder()
//                        .uri(URI.create(fileService.generatePresignedDownloadUrl(mediaFileId)))
//                        .timeout(Duration.ofMinutes(2))
//                        .GET()
//                        .build();
//                log.debug("Downloading video file from URL: {}", downloadRequest.uri());
//                try (InputStream in = httpClient.send(downloadRequest, HttpResponse.BodyHandlers.ofInputStream())
//                        .body()) {
//                    Files.copy(in, videoFile, StandardCopyOption.REPLACE_EXISTING);
//                } catch (IOException | InterruptedException e) {
//                    log.error("Failed to download video file for mediaFile={}", mediaFileId, e);
//                    throw new FileDownloadException("Failed to download video file for mediaFile: " + mediaFileId, e);
//                }
//                log.debug("Video file downloaded to: {}", videoFile);
//                audioFile = Files.createTempFile(prefix + "aud", ".mp3");
//                log.debug("Starting audio extraction for mediaFile={}", mediaFileId);
//                Process ffmpeg = new ProcessBuilder(
//                        "ffmpeg", "-y", "-i", videoFile.toString(),
//                        "-vn", "-acodec", "libmp3lame", audioFile.toString())
//                        .redirectErrorStream(true)
//                        .start();
//                try (InputStream ffmpegOut = ffmpeg.getInputStream()) {
//                    String output = new String(ffmpegOut.readAllBytes());
//                    log.debug("ffmpeg output: {}", output);
//                }
//                int exit = ffmpeg.waitFor();
//                if (exit != 0) {
//                    throw new AudioExtractionException("FFmpeg exited with status " + exit + " for mediaFile: " + mediaFileId);
//                }
//
//                String sourceFileName = source.getFilename().substring(0, source.getFilename().lastIndexOf('.'));
//
//                String targetName = sourceFileName + "_audio.mp3";
//
//                FileUploadRequestDto uploadDto = new FileUploadRequestDto(
//                        source.getUploadBatch().getId(), targetName, FileType.AUDIO, mediaFileId);
//
//                String presignedPut = fileService.generatePresignedUploadUrl(uploadDto);
//
//                HttpRequest uploadRequest = HttpRequest.newBuilder()
//                        .uri(URI.create(presignedPut))
//                        .timeout(Duration.ofMinutes(2))
//                        .PUT(HttpRequest.BodyPublishers.ofFile(audioFile))
//                        .build();
//                log.debug("Uploading audio file to URL: {}", uploadRequest.uri());
//                httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());
//                log.debug("Audio file uploaded successfully: {}", targetName);
//                return "i will fix this i promise";
//
//            } catch (IOException | InterruptedException e) {
//                log.error("Audio extraction failed for mediaFile={}", mediaFileId, e);
//                throw new AudioExtractionException("Failed to extract audio for mediaFile: " + mediaFileId, e);
//            } finally {
//                safeDelete(videoFile);
//                safeDelete(audioFile);
//            }
//        });
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public void processUploadedVideo(String uuid, String filename) {
//        MediaFile mediaFile = mediaFileRepository.findByFilenameAndUploadBatchDirectory(filename, UUID.fromString(uuid))
//                .orElseThrow(() -> new MediaFileNotFoundException("Media file not found: " + filename + " in batch " + uuid));
//        extractAudio(mediaFile.getId());
//

    /// /        UploadBatch uploadBatch = uploadBatchRepository.findByDirectory(UUID.fromString(uuid))
    /// /                .orElseThrow(() -> new IllegalArgumentException("Upload batch not found for UUID: " + uuid));
    /// /        uploadBatch.getFiles().forEach(f -> {
    /// /            if (f.getFilename().equals(filename)) {
    /// /                log.debug("Processing file: {}", f.getFilename());
    /// /                extractAudio(f.getId());
    /// /            } else {
    /// /                log.debug("Skipping file: {}", f.getFilename());
    /// /            }
    /// /        });
//    }
//
//
//    private static void safeDelete(Path path) {
//        if (path != null) {
//            try {
//                Files.deleteIfExists(path);
//            } catch (IOException ignored) {
//            }
//        }
//    }
    private static void safeDelete(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }
    }
}
