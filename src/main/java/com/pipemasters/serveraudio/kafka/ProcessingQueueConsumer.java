package com.pipemasters.serveraudio.kafka;

import com.pipemasters.serveraudio.service.AudioService;
import com.pipemasters.serveraudio.service.impl.AudioServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessingQueueConsumer {
    private final Logger log = LoggerFactory.getLogger(ProcessingQueueConsumer.class);

    private final AudioService audioService;
    private final KafkaProducerService producerService;

    public ProcessingQueueConsumer(AudioService audioService, KafkaProducerService producerService) {
        this.audioService = audioService;
        this.producerService = producerService;
    }


    @KafkaListener(topics = "audio-extraction")
    @Transactional
    public void process(String message) {
        log.info("Received message: {}", message);
        try {
            String result = audioService.extractAudio(message);
            producerService.send("processed", result);
        } catch (Exception e) {
            log.error("Audio extraction failed for message: {}", message, e);
        }
    }
}