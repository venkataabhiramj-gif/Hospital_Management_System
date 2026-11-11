package com.hospital.management.server.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;

@Controller
public class DownloadController {

    @GetMapping(value = {"/downloads/apexcare-mobile.apk", "/api/download/apk"})
    @ResponseBody
    public ResponseEntity<Resource> downloadApk() throws IOException {
        File[] candidateFiles = new File[]{
                new File("webapp/downloads/apexcare-mobile.apk"),
                new File("../webapp/downloads/apexcare-mobile.apk"),
                new File("android/app/build/outputs/apk/debug/app-debug.apk"),
                new File("../android/app/build/outputs/apk/debug/app-debug.apk")
        };

        for (File f : candidateFiles) {
            if (f.exists() && f.isFile() && f.length() > 0) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"apexcare-mobile.apk\"")
                        .header(HttpHeaders.CONTENT_TYPE, "application/vnd.android.package-archive")
                        .contentLength(f.length())
                        .body(new FileSystemResource(f));
            }
        }

        ClassPathResource cp = new ClassPathResource("static/downloads/apexcare-mobile.apk");
        if (cp.exists()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"apexcare-mobile.apk\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.android.package-archive")
                    .contentLength(cp.contentLength())
                    .body(cp);
        }

        return ResponseEntity.notFound().build();
    }
}
