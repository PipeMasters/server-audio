package com.pipemasters.serveraudio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Component
public class FfmpegVerifier implements ApplicationRunner {
    private static Logger log = LoggerFactory.getLogger(FfmpegVerifier.class);
    @Override
    public void run(ApplicationArguments args) throws Exception {
        Process process = new ProcessBuilder("ffmpeg", "-version")
                .redirectErrorStream(true)
                .start();

        String banner;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            banner = r.lines().collect(Collectors.joining("\n"));
        }

        int exit = process.waitFor();
        if (exit == 0) {
            log.info("FFmpeg detected: {}", banner.split("\n")[0]);
        } else {
            log.error("FFmpeg not found or returned exit code {}", exit);
            throw new IllegalStateException("FFmpeg binary is missing from PATH");
        }
    }
}