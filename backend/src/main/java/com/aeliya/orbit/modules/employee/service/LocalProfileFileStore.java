package com.aeliya.orbit.modules.employee.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalProfileFileStore implements ProfileFileStore {
    private final Path root;

    public LocalProfileFileStore(@Value("${USER_DATA_DIR:}") String directory) {
        Path working = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (directory != null && !directory.isBlank()) root = Path.of(directory).toAbsolutePath().normalize();
        else if (working.getFileName() != null && working.getFileName().toString().equals("backend"))
            root = working.getParent().resolve("user-data");
        else root = working.resolve("user-data");
    }

    @Override
    public void save(long userId, Kind kind, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw invalid();
        Path folder = folder(userId);
        if (Files.isSymbolicLink(root) || Files.isSymbolicLink(folder)) throw invalid();
        Files.createDirectories(folder);
        ownerOnly(root, true);
        ownerOnly(folder, true);
        Path target = folder.resolve(kind.filename());
        if (Files.isSymbolicLink(target)) throw invalid();
        Path temporary = Files.createTempFile(folder, ".upload-", ".tmp");
        try {
            try (InputStream source = file.getInputStream(); PushbackInputStream input = new PushbackInputStream(source, 16)) {
                byte[] header = input.readNBytes(16);
                String contentType = contentType(header);
                if (contentType == null || (kind == Kind.PROFILE && !contentType.startsWith("image/"))) throw invalid();
                input.unread(header);
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            ownerOnly(target, false);
        } finally { Files.deleteIfExists(temporary); }
    }

    @Override
    public StoredFile find(long userId, Kind kind) throws IOException {
        Path folder = folder(userId);
        Path target = folder.resolve(kind.filename());
        if (Files.isSymbolicLink(root) || Files.isSymbolicLink(folder) || Files.isSymbolicLink(target)) throw invalid();
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) return null;
        try (InputStream input = Files.newInputStream(target)) {
            String contentType = contentType(input.readNBytes(16));
            if (contentType == null || (kind == Kind.PROFILE && !contentType.startsWith("image/"))) throw invalid();
            return new StoredFile(target, contentType);
        }
    }

    @Override
    public void delete(long userId, Kind kind) throws IOException {
        Path folder = folder(userId);
        Path target = folder.resolve(kind.filename());
        if (Files.isSymbolicLink(root) || Files.isSymbolicLink(folder) || Files.isSymbolicLink(target)) throw invalid();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS))
            throw invalid();
        Files.deleteIfExists(target);
    }

    private Path folder(long userId) {
        if (userId < 1) throw invalid();
        return root.resolve(Long.toString(userId));
    }

    private void ownerOnly(Path path, boolean directory) throws IOException {
        try {
            Files.setPosixFilePermissions(path, directory
                ? Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
                : Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) { /* Non-POSIX filesystems use their native ACLs. */ }
    }

    private String contentType(byte[] header) {
        if (header.length >= 8 && (header[0] & 255) == 137 && header[1] == 80 && header[2] == 78 && header[3] == 71
                && header[4] == 13 && header[5] == 10 && header[6] == 26 && header[7] == 10) return "image/png";
        if (header.length >= 3 && (header[0] & 255) == 255 && (header[1] & 255) == 216 && (header[2] & 255) == 255) return "image/jpeg";
        if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return "image/webp";
        if (header.length >= 5 && header[0] == '%' && header[1] == 'P' && header[2] == 'D'
                && header[3] == 'F' && header[4] == '-') return "application/pdf";
        return null;
    }

    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported or unsafe file"); }
}
