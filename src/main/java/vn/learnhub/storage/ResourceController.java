package vn.learnhub.storage;

import vn.learnhub.auth.Access;
import vn.learnhub.shared.persistence.Store;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.AccessDeniedException;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller
public class ResourceController {
    private final Store db;
    private final Access access;
    private final FileService files;

    public ResourceController(Store db, Access access, FileService files) {
        this.db = db;
        this.access = access;
        this.files = files;
    }

    @GetMapping("/resources/{id}")
    ResponseEntity<?> resource(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download, Principal p) {
        Resource r = db.get(Resource.class, id);
        if (!r.reviewed) throw new AccessDeniedException("Tài liệu chưa sẵn sàng.");
        access.requireLesson(access.current(p), r.lesson);
        if (r.isExternal()) {
            if (download) throw new AccessDeniedException("Hãy mở tài liệu để xem các lựa chọn từ nơi chia sẻ.");
            return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create(MediaLinks.checked(r.externalUrl)))
                    .cacheControl(CacheControl.noStore()).build();
        }
        boolean pdf = r.contentType.equals("application/pdf");
        boolean attachment = download || !pdf;
        if (attachment && !r.allowDownload) throw new AccessDeniedException("Tài liệu này chưa được cho phép tải xuống.");
        String name = r.originalName.isBlank() ? "tai-lieu-" + id + ".pdf" : r.originalName;
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(r.contentType)).cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .header(HttpHeaders.CONTENT_DISPOSITION, (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(name, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(new FileSystemResource(files.path(r)));
    }
}
