package com.cognitera.platform.api.ingestion;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;

/**
 * Utility for extracting plain text from DOCX files using Apache POI.
 */
final class DocxTextExtractor {

    private DocxTextExtractor() {
    }

    static String extract(InputStream in) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
