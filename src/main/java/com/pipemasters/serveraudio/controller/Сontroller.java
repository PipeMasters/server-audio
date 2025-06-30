package com.pipemasters.serveraudio.controller;

import com.pipemasters.serveraudio.service.impl.AudioServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/")
public class Сontroller {
    private final AudioServiceImpl audioService;

    public Сontroller(AudioServiceImpl audioService) {
        this.audioService = audioService;
    }

    @GetMapping("/")
    public ResponseEntity<String> getPresignedDownloadUrl() throws IOException, InterruptedException {
        String url = String.valueOf(audioService.extractAudio("81e10071-e297-4919-92c2-51bf019ff8c6/meow"));
        return ResponseEntity.ok(url);
    }
}
