package com.pipemasters.serveraudio;

import com.pipemasters.serveraudio.config.MinioConfig;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public class test {
//    public static void main(String[] args) throws InterruptedException, IOException {
//        MinioConfig minioConfig = new MinioConfig();
//        S3Presigner s3Presigner = minioConfig.s3Presigner();
//        String bucketName = "dev-bucket";
//        String s3Key = "81e10071-e297-4919-92c2-51bf019ff8c6/meow";
//
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                .bucket(bucketName)
//                .key(s3Key)
//                .build();
//
//        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                .signatureDuration(Duration.ofMinutes(10))
//                .getObjectRequest(getObjectRequest)
//                .build();
//
//        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
//
//        File videoFile = new File("C://test_files/meow.mp4");
//        File audioFile = new File("C://test_files/meowAudio.mp3");
//        Process ffmpeg = new ProcessBuilder(
//                "ffmpeg",
//                "-y",
//                "-i", videoFile.toString(),
//                "-vn",
//                "-acodec", "libmp3lame",
//                audioFile.toString()
//        ).redirectErrorStream(true).start();
//
//        try (InputStream ffmpegOut = ffmpeg.getInputStream()) {
//            System.out.println(new String(ffmpegOut.readAllBytes()));
//        }
//        int exitCode = ffmpeg.waitFor();
//        if (exitCode != 0) {
//            throw new RuntimeException("FFmpeg exited with code " + exitCode);
//        }
//
//    }
}
