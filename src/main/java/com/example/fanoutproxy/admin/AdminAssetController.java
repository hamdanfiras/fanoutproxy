package com.example.fanoutproxy.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAssetController {

    @GetMapping(value = "/css/admin.css", produces = "text/css")
    public ResponseEntity<String> css() throws IOException {
        return ResponseEntity.ok(read("static/css/admin.css"));
    }

    @GetMapping(value = "/js/admin.js", produces = "application/javascript")
    public ResponseEntity<String> js() throws IOException {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/javascript"))
                .body(read("static/js/admin.js"));
    }

    private String read(String location) throws IOException {
        return StreamUtils.copyToString(new ClassPathResource(location).getInputStream(), StandardCharsets.UTF_8);
    }
}
