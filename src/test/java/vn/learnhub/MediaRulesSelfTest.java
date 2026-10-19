package vn.learnhub;

import vn.learnhub.storage.DocumentRules;
import vn.learnhub.storage.MediaLinks;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MediaRulesSelfTest {
    private static int checks;
    private interface Action { void run() throws Exception; }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }

    private static void rejects(Action action) throws Exception {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected validation failure");
    }

    private static byte[] zip(String... names) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (String name : names) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write("<document/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        checks = 0;
        byte[] pdf = "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        equal("application/pdf", DocumentRules.validate("bai-hoc.PDF", pdf));
        equal("text/plain", DocumentRules.validate("tu-vung.txt", "你好 · Hello\nXin chào".getBytes(StandardCharsets.UTF_8)));
        byte[] below = Arrays.copyOf(pdf, 999_999);
        equal("application/pdf", DocumentRules.validate("large.pdf", below));
        rejects(() -> DocumentRules.validate("large.pdf", Arrays.copyOf(pdf, 1_000_000)));
        rejects(() -> DocumentRules.validate("large.pdf", Arrays.copyOf(pdf, 1_000_001)));
        rejects(() -> DocumentRules.validate("empty.pdf", new byte[0]));
        rejects(() -> DocumentRules.validate("fake.pdf", "not a PDF".getBytes(StandardCharsets.UTF_8)));
        rejects(() -> DocumentRules.validate("bad.html", "<script/>".getBytes(StandardCharsets.UTF_8)));
        rejects(() -> DocumentRules.validate("movie.mp4", pdf));
        rejects(() -> DocumentRules.validate("file.docm", pdf));
        rejects(() -> DocumentRules.validate("file.txt", new byte[]{0}));
        rejects(() -> DocumentRules.validate("file.txt", new byte[]{(byte) 0xFF}));
        rejects(() -> DocumentRules.validate("script.pdf.exe", pdf));
        equal("application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentRules.validate("file.docx", zip("[Content_Types].xml", "word/document.xml")));
        equal("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", DocumentRules.validate("file.xlsx", zip("[Content_Types].xml", "xl/workbook.xml")));
        equal("application/vnd.openxmlformats-officedocument.presentationml.presentation", DocumentRules.validate("file.pptx", zip("[Content_Types].xml", "ppt/presentation.xml")));
        rejects(() -> DocumentRules.validate("wrong.docx", zip("[Content_Types].xml", "xl/workbook.xml")));
        rejects(() -> DocumentRules.validate("wrong.docx", zip("word/document.xml")));
        rejects(() -> DocumentRules.validate("wrong.docx", zip("[Content_Types].xml", "word/document.xml", "word/vbaProject.bin")));
        rejects(() -> DocumentRules.validate("wrong.docx", zip("[Content_Types].xml", "word/document.xml", "../escape")));
        rejects(() -> DocumentRules.validate("wrong.docx", pdf));
        equal("https://drive.google.com/file/d/abc_123-Z/preview", MediaLinks.embed("https://drive.google.com/file/d/abc_123-Z/view?usp=sharing", ""));
        equal("https://drive.google.com/file/d/abc/preview?resourcekey=key-123", MediaLinks.embed("https://drive.google.com/open?id=abc&resourcekey=key-123", ""));
        equal("https://drive.google.com/file/d/abc/preview", MediaLinks.embed("https://drive.google.com/uc?id=abc", ""));
        equal("https://drive.google.com/file/d/abc/preview", MediaLinks.embed("https://drive.google.com/file/d/abc/preview", ""));
        equal("", MediaLinks.embed("https://1drv.ms/v/s!sample", ""));
        equal("https://onedrive.live.com/embed?resid=A%21123&authkey=ABC", MediaLinks.embed("https://1drv.ms/v/sample", "https://onedrive.live.com/embed?resid=A%21123&authkey=ABC"));
        equal("https://onedrive.live.com/embed?resid=123", MediaLinks.embed("https://onedrive.live.com/embed?resid=123", ""));
        equal("", MediaLinks.embed("https://www.dropbox.com/s/sample/video.mp4?dl=0", ""));
        equal("", MediaLinks.embed("https://drive.google.com.evil.example/file/d/abc/view", ""));
        equal("", MediaLinks.embed("https://docs.google.com/document/d/abc/edit", ""));
        equal("https://school.sharepoint.com/sites/lessons/_layouts/15/embed.aspx?UniqueId=123", MediaLinks.embed("https://school.sharepoint.com/file", "https://school.sharepoint.com/sites/lessons/_layouts/15/embed.aspx?UniqueId=123"));
        for (String invalid : new String[]{"javascript:alert(1)", "data:text/html,test", "http://example.com/video", "//example.com/video", "https://user:pass@example.com/video", "https://example.com:9000/video", "https://example.com/\nvideo", "<iframe src=\"https://drive.google.com\"></iframe>", "https://example.com/\\bad", "https://"})
            rejects(() -> MediaLinks.checked(invalid));
        rejects(() -> MediaLinks.embed("https://example.com/video", "https://drive.google.com.evil.example/file/d/abc/preview"));
        rejects(() -> MediaLinks.embed("https://example.com/video", "https://onedrive.live.com/not-embed"));
        rejects(() -> MediaLinks.embed("https://example.com/video", "https://example.com/embed"));
        System.out.println("PASS: " + checks + " media validation checks");
    }
}
