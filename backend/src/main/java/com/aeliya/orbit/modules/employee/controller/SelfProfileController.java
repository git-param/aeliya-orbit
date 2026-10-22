package com.aeliya.orbit.modules.employee.controller;

import com.aeliya.orbit.modules.employee.service.ProfileFileStore;
import com.aeliya.orbit.modules.employee.service.SelfProfileService;
import java.io.IOException;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/profile")
public class SelfProfileController {
    private final SelfProfileService profiles;
    public SelfProfileController(SelfProfileService profiles) { this.profiles = profiles; }

    @GetMapping public SelfProfileService.Profile get(Authentication actor) throws IOException { return profiles.get(actor); }
    @PutMapping public SelfProfileService.Profile update(@RequestBody SelfProfileService.Update input, Authentication actor) throws IOException {
        return profiles.update(input, actor);
    }
    @PostMapping("/phones") public List<SelfProfileService.SecondaryPhone> addPhone(
            @RequestBody SelfProfileService.PhoneInput input, Authentication actor) { return profiles.addPhone(input, actor); }
    @DeleteMapping("/phones/{id}") public List<SelfProfileService.SecondaryPhone> deletePhone(
            @PathVariable long id, Authentication actor) { return profiles.deletePhone(id, actor); }

    @PostMapping(path = "/{kind}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@PathVariable String kind, @RequestParam MultipartFile file, Authentication actor) throws IOException {
        profiles.upload(kind(kind), file, actor);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/picture")
    public ResponseEntity<Void> deletePicture(Authentication actor) throws IOException {
        profiles.deletePicture(actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{kind}")
    public ResponseEntity<Resource> download(@PathVariable String kind, Authentication actor) throws IOException {
        ProfileFileStore.Kind parsed = kind(kind);
        if (parsed == ProfileFileStore.Kind.PROFILE) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var stored = profiles.file(parsed, actor);
        if (stored == null) return ResponseEntity.notFound().build();
        String extension = switch (stored.contentType()) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> "";
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(stored.contentType()))
            .header("Cache-Control", "private, no-store")
            .header("Content-Disposition", "inline; filename=\"" + parsed.filename() + extension + "\"")
            .body(new FileSystemResource(stored.path()));
    }

    private ProfileFileStore.Kind kind(String value) {
        return switch (value) {
            case "picture" -> ProfileFileStore.Kind.PROFILE;
            case "aadhar" -> ProfileFileStore.Kind.AADHAAR;
            case "pan" -> ProfileFileStore.Kind.PAN;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }
}
