package com.pipemasters.serveraudio.kafka;

import com.pipemasters.serveraudio.service.impl.AudioServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessingQueueConsumer {
    private final Logger log = LoggerFactory.getLogger(ProcessingQueueConsumer.class);

    private final AudioServiceImpl audioService;
    private final KafkaProducerService producerService;

    public ProcessingQueueConsumer(AudioServiceImpl audioService, KafkaProducerService producerService) {
        this.audioService = audioService;
        this.producerService = producerService;
    }


    @KafkaListener(topics = "audio-extraction")
    @Transactional
    public void process(String message) {
        log.info("Accept message with :{}",message);
        audioService.extractAudio(message)
                .thenAccept(result -> producerService.send("processed",result))
                .exceptionally(e -> {
                    log.error("Audio extraction failed", e);
                    return null;
                });
    }
}