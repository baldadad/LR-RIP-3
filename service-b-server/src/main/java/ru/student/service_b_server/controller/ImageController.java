package ru.student.service_b_server.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class ImageController {

    // Кэш 
    private final Map<String, String> metadataCache = new ConcurrentHashMap<>();

    // meta usage
    @GetMapping("/images/{id}/metadata")
    public Mono<String> getMetadata(@PathVariable String id) {
        return Mono.fromCallable(() -> loadMetadataOptimized(id))
                .subscribeOn(Schedulers.boundedElastic()).onErrorResume(e -> Mono.just("ERROR while processing image " + id + ": " + e.getMessage())
            );
    }

    // meta load
    private String loadMetadataOptimized(String id) throws IOException {

        // cash check
        String cached = metadataCache.get(id);
        if (cached != null) {
            return cached;
        }

        // header read
        Path path = Path.of("pic.jpg");
        byte[] header = Files.readAllBytes(path);

        Map<String, String> metadataMap = loadMetadata();

        String result = "Image " + id +
                " metadata=" + metadataMap +
                " (header size=" + header.length + " bytes)";

        metadataCache.put(id, result);

        return result;
    }

    private Map<String, String> loadMetadata() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("camera", "nokia3310");
        meta.put("author", "Ryan Gosling");
        meta.put("width", "8800");
        meta.put("height", "5553");
        return meta;
    }

    @GetMapping("/images/metadata/stream")
    public Flux<String> streamMetadata() {
        return Flux.fromIterable(loadMetadata().entrySet())
                .map(e -> e.getKey() + "=" + e.getValue());
    }
}