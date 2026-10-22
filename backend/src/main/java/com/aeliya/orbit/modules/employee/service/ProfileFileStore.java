package com.aeliya.orbit.modules.employee.service;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileFileStore {
    enum Kind { PROFILE("profile"), AADHAAR("aadhar"), PAN("pan");
        private final String filename;
        Kind(String filename) { this.filename = filename; }
        public String filename() { return filename; }
    }
    record StoredFile(Path path, String contentType) {}
    void save(long userId, Kind kind, MultipartFile file) throws IOException;
    StoredFile find(long userId, Kind kind) throws IOException;
    void delete(long userId, Kind kind) throws IOException;
}
