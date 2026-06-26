package com.cognitera.platform.api.ingestion;

import com.cognitera.platform.document.api.TextExtractionService;
import com.cognitera.platform.document.model.DocumentType;
import com.cognitera.platform.document.model.DocumentVersion;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Default implementation of {@link TextExtractionService} that extracts plain text
 * from TXT, HTML, DOCX, and PDF files stored on the local filesystem.
 */
@Component
public class DefaultTextExtractionService implements TextExtractionService {

    /**
     * Extracts plain text from a document version based on its type and storage location.
     */
    @Override
    public String extractText(DocumentType type, DocumentVersion version) {
        if (!"local-fs".equalsIgnoreCase(version.storageProvider())) {
            throw new IllegalStateException("Unsupported storage provider: " + version.storageProvider());
        }
        Path path = Path.of(version.storageKey());
        if (!Files.exists(path)) {
            throw new IllegalStateException("Stored file not found: " + version.storageKey());
        }
        try (InputStream in = Files.newInputStream(path)) {
            return switch (type) {
                case TXT -> new String(in.readAllBytes(), StandardCharsets.UTF_8);
                case HTML -> Jsoup.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)).text();
                case DOCX -> DocxTextExtractor.extract(in);
                case PDF -> extractPdfText(in);
                default -> throw new IllegalStateException("Unsupported document type for file-based extraction: " + type);
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract document text", ex);
        }
    }

    private String extractPdfText(InputStream in) throws Exception {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}
