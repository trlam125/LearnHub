package vn.learnhub.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;

public final class DocumentRules {
    public static final int MAX_BYTES = 1_000_000;
    public static final String SIZE_MESSAGE = "Chỉ tải trực tiếp tài liệu dưới 1 MB. Với file lớn hơn, hãy lưu trên Google Drive, OneDrive hoặc dịch vụ khác rồi thêm liên kết.";
    private static final Map<String, String> TYPES = Map.of(
            "pdf", "application/pdf", "txt", "text/plain",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    private DocumentRules() { }

    public static String extension(String name) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT);
        int dot = value.lastIndexOf('.');
        String ext = dot < 0 ? "" : value.substring(dot + 1);
        if (!TYPES.containsKey(ext))
            throw new IllegalArgumentException("Chọn tài liệu PDF, Word (.docx), Excel (.xlsx), PowerPoint (.pptx) hoặc văn bản (.txt).");
        return ext;
    }

    public static String validate(String name, byte[] bytes) throws IOException {
        if (bytes.length == 0) throw new IllegalArgumentException("File đang trống. Hãy chọn tài liệu khác.");
        if (bytes.length >= MAX_BYTES) throw new IllegalArgumentException(SIZE_MESSAGE);
        String ext = extension(name);
        if (ext.equals("pdf")) {
            if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
                throw new IllegalArgumentException("File chưa đúng định dạng PDF.");
        } else if (ext.equals("txt")) {
            try {
                String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
                if (text.chars().anyMatch(c -> c == 0 || Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t'))
                    throw new IllegalArgumentException("Hãy chọn file văn bản thuần, không chứa nội dung lạ.");
            } catch (CharacterCodingException ex) {
                throw new IllegalArgumentException("Chưa đọc được file văn bản này. Hãy xuất sang PDF rồi thử lại.");
            }
        } else {
            String required = Map.of("docx", "word/document.xml", "xlsx", "xl/workbook.xml", "pptx", "ppt/presentation.xml").get(ext);
            boolean manifest = false, document = false;
            Set<String> entries = new java.util.HashSet<>();
            long total = 0;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                java.util.zip.ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName();
                    if (!entries.add(path) || entries.size() > 1000 || path.contains("..") || path.startsWith("/")
                            || path.toLowerCase(Locale.ROOT).endsWith("vbaproject.bin"))
                        throw new IllegalArgumentException("Tài liệu không được hỗ trợ. Hãy xuất sang PDF hoặc thêm liên kết chia sẻ.");
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        total += read;
                        if (total > 10_000_000) throw new IllegalArgumentException("Tài liệu quá phức tạp. Hãy thêm bằng liên kết chia sẻ.");
                    }
                    manifest |= path.equals("[Content_Types].xml");
                    document |= path.equals(required);
                }
            } catch (java.util.zip.ZipException ex) {
                throw new IllegalArgumentException("Tài liệu bị lỗi hoặc không đúng định dạng.");
            }
            if (!manifest || !document) throw new IllegalArgumentException("Tài liệu không đúng định dạng đã chọn.");
        }
        return TYPES.get(ext);
    }
}
