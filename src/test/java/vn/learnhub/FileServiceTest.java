package vn.learnhub;

import vn.learnhub.auth.Access;
import vn.learnhub.config.AppProperties;
import vn.learnhub.course.Lesson;
import vn.learnhub.shared.persistence.Store;
import vn.learnhub.storage.FileService;
import vn.learnhub.storage.Resource;
import vn.learnhub.user.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceTest {
    @TempDir
    Path directory;

    private FileService service(Store db, Access access) {
        AppProperties app = mock(AppProperties.class);
        when(app.storage()).thenReturn(directory);
        return new FileService(db, access, app);
    }

    @Test
    void uploadedDocumentIsRemovedOnTransactionRollback() throws Exception {
        Store db = mock(Store.class);
        Access access = mock(Access.class);
        when(db.get(Lesson.class, 1L)).thenReturn(new Lesson());
        TransactionSynchronizationManager.initSynchronization();
        try {
            service(db, access).upload(new Account(), 1L, "Tài liệu", new MockMultipartFile("file", "lesson.pdf", "application/pdf", "%PDF-1.7\n%%EOF".getBytes()), true, true);
            try (var paths = Files.list(directory)) {
                assertEquals(1, paths.count());
            }
            for (var sync : TransactionSynchronizationManager.getSynchronizations())
                sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            try (var paths = Files.list(directory)) {
                assertEquals(0, paths.count());
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void databaseWriteFailureDoesNotLeaveAFile() throws Exception {
        Store db = mock(Store.class);
        when(db.get(Lesson.class, 1L)).thenReturn(new Lesson());
        doThrow(new IllegalStateException("failed")).when(db).add(any(Resource.class));
        assertThrows(IllegalStateException.class, () -> service(db, mock(Access.class)).upload(new Account(), 1L, "Lesson", new MockMultipartFile("file", "lesson.pdf", "application/pdf", "%PDF-1.7".getBytes()), true, true));
        try (var paths = Files.list(directory)) {
            assertEquals(0, paths.count());
        }
    }

    @Test
    void oversizedFileAndStudentCannotReachStorage() throws Exception {
        Store db = mock(Store.class);
        when(db.get(Lesson.class, 1L)).thenReturn(new Lesson());
        Account actor = new Account();
        Access access = mock(Access.class);
        assertThrows(IllegalArgumentException.class, () -> service(db, access).upload(actor, 1L, "Large", new MockMultipartFile("file", "large.pdf", "application/pdf", new byte[1_000_000]), true, true));
        doThrow(new org.springframework.security.access.AccessDeniedException("Denied")).when(access).requireStaff(actor);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service(db, access).link(actor, null, 1L, "Video", "video", "https://drive.google.com/file/d/example/view", "", true));
        verify(db, never()).add(any(Resource.class));
        try (var paths = Files.list(directory)) {
            assertEquals(0, paths.count());
        }
    }
}
