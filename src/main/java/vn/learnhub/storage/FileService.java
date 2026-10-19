package vn.learnhub.storage;

import vn.learnhub.auth.Access;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.user.Account;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import java.util.*;

@Service
public class FileService {
    private final Store db;
    private final Access access;
    private final Path root;

    public FileService(Store db, Access access, AppProperties app) {
        this.db = db;
        this.access = access;
        root = app.storage().toAbsolutePath().normalize();
    }

    @Transactional(rollbackFor = IOException.class)
    public void upload(Account actor, Long lessonId, String title, MultipartFile file, boolean reviewed, boolean allowDownload) throws IOException {
        access.requireStaff(actor);
        String name = title(title);
        Lesson lesson = db.get(Lesson.class, lessonId);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chọn tài liệu cần tải lên.");
        if (file.getSize() >= DocumentRules.MAX_BYTES) throw new IllegalArgumentException(DocumentRules.SIZE_MESSAGE);
        byte[] bytes;
        try (InputStream stream = file.getInputStream()) {
            bytes = stream.readNBytes(DocumentRules.MAX_BYTES);
        }
        String original = Objects.toString(file.getOriginalFilename(), "");
        String contentType = DocumentRules.validate(original, bytes);
        String extension = DocumentRules.extension(original);
        Files.createDirectories(root);
        String stored = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(stored);
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) removeQuietly(target);
                }
            });
        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            Resource resource = new Resource();
            resource.lesson = lesson;
            resource.title = name;
            resource.storedName = stored;
            resource.originalName = original.replaceAll("[\\p{Cntrl}\\\\/]", "_");
            if (resource.originalName.length() > 200) resource.originalName = "tai-lieu." + extension;
            resource.contentType = contentType;
            resource.sizeBytes = bytes.length;
            resource.reviewed = reviewed;
            resource.allowDownload = allowDownload;
            db.add(resource);
        } catch (IOException | RuntimeException ex) {
            removeQuietly(target);
            throw ex;
        }
    }

    @Transactional
    public void link(Account actor, Long id, Long lessonId, String title, String materialType,
                     String externalUrl, String embedUrl, boolean reviewed) {
        access.requireStaff(actor);
        if (!Set.of("video", "document").contains(materialType)) throw new IllegalArgumentException("Chọn video hoặc tài liệu.");
        String name = title(title);
        String url = MediaLinks.checked(externalUrl);
        String player = materialType.equals("video") ? MediaLinks.embed(url, embedUrl) : "";
        Lesson lesson = db.get(Lesson.class, lessonId);
        Resource resource = id == null ? new Resource() : db.lock(Resource.class, id);
        if (id != null && !resource.isExternal()) throw new IllegalArgumentException("Tài liệu này đã được tải trực tiếp. Hãy thêm liên kết thành một mục mới.");
        resource.lesson = lesson;
        resource.title = name;
        resource.materialType = materialType;
        resource.externalUrl = url;
        resource.embedUrl = player;
        resource.reviewed = reviewed;
        resource.contentType = "application/octet-stream";
        if (id == null) db.add(resource);
    }

    @Transactional
    public void details(Account actor, Long id, String title, boolean reviewed, boolean allowDownload) {
        access.requireStaff(actor);
        Resource resource = db.lock(Resource.class, id);
        resource.title = title(title);
        resource.reviewed = reviewed;
        resource.allowDownload = !resource.isExternal() && allowDownload;
    }

    private String title(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 180)
            throw new IllegalArgumentException("Nhập tên video hoặc tài liệu, tối đa 180 ký tự.");
        return title.trim();
    }

    private void removeQuietly(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            System.getLogger(FileService.class.getName()).log(System.Logger.Level.WARNING, "Cannot remove unused material file", ex);
        }
    }

    public Path path(Resource resource) {
        if (resource.isExternal() || resource.storedName.isBlank()) throw new IllegalArgumentException("Tài liệu được mở từ liên kết chia sẻ.");
        Path path = root.resolve(resource.storedName).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Tài liệu chưa sẵn sàng.");
        return path;
    }
}
