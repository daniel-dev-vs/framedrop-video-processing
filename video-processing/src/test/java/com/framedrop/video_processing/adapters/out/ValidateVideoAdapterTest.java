package com.framedrop.video_processing.adapters.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ValidateVideoAdapterTest {

    private final ValidateVideoAdapter validateVideoAdapter = new ValidateVideoAdapter();

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnFalseForTextFile() throws IOException {
        File textFile = tempDir.resolve("file.txt").toFile();
        Files.writeString(textFile.toPath(), "This is plain text content");

        assertFalse(validateVideoAdapter.isValidFormatVideo(textFile));
    }

    @Test
    void shouldReturnFalseForPdfFile() throws IOException {
        File pdfFile = tempDir.resolve("document.pdf").toFile();
        // PDF magic bytes: %PDF
        Files.writeString(pdfFile.toPath(), "%PDF-1.4 fake pdf content");

        assertFalse(validateVideoAdapter.isValidFormatVideo(pdfFile));
    }

    @Test
    void shouldReturnFalseForHtmlFile() throws IOException {
        File htmlFile = tempDir.resolve("page.html").toFile();
        Files.writeString(htmlFile.toPath(), "<html><body>Hello</body></html>");

        assertFalse(validateVideoAdapter.isValidFormatVideo(htmlFile));
    }

    @Test
    void shouldReturnFalseForJsonFile() throws IOException {
        File jsonFile = tempDir.resolve("data.json").toFile();
        Files.writeString(jsonFile.toPath(), "{\"key\":\"value\"}");

        assertFalse(validateVideoAdapter.isValidFormatVideo(jsonFile));
    }

    @Test
    void shouldReturnFalseForPngImage() throws IOException {
        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        File pngFile = tempDir.resolve("image.png").toFile();
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        Files.write(pngFile.toPath(), pngHeader);

        assertFalse(validateVideoAdapter.isValidFormatVideo(pngFile));
    }

    @Test
    void shouldThrowExceptionForNonExistentFile() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.mp4");

        assertThrows(RuntimeException.class,
                () -> validateVideoAdapter.isValidFormatVideo(nonExistent));
    }

    @Test
    void shouldReturnTrueForMp4WithValidMagicBytes() throws IOException {
        // MP4 files start with ftyp box: offset 4 contains "ftyp"
        File mp4File = tempDir.resolve("video.mp4").toFile();
        byte[] mp4Header = new byte[12];
        mp4Header[0] = 0x00;
        mp4Header[1] = 0x00;
        mp4Header[2] = 0x00;
        mp4Header[3] = 0x18; // box size
        mp4Header[4] = 0x66; // 'f'
        mp4Header[5] = 0x74; // 't'
        mp4Header[6] = 0x79; // 'y'
        mp4Header[7] = 0x70; // 'p'
        mp4Header[8] = 0x69; // 'i'
        mp4Header[9] = 0x73; // 's'
        mp4Header[10] = 0x6F; // 'o'
        mp4Header[11] = 0x6D; // 'm'
        Files.write(mp4File.toPath(), mp4Header);

        assertTrue(validateVideoAdapter.isValidFormatVideo(mp4File));
    }

    @Test
    void shouldReturnTrueForAviWithValidMagicBytes() throws IOException {
        // AVI files start with RIFF....AVI
        File aviFile = tempDir.resolve("video.avi").toFile();
        byte[] aviHeader = new byte[12];
        aviHeader[0] = 0x52; // 'R'
        aviHeader[1] = 0x49; // 'I'
        aviHeader[2] = 0x46; // 'F'
        aviHeader[3] = 0x46; // 'F'
        aviHeader[4] = 0x00;
        aviHeader[5] = 0x00;
        aviHeader[6] = 0x00;
        aviHeader[7] = 0x00;
        aviHeader[8] = 0x41; // 'A'
        aviHeader[9] = 0x56; // 'V'
        aviHeader[10] = 0x49; // 'I'
        aviHeader[11] = 0x20; // ' '
        Files.write(aviFile.toPath(), aviHeader);

        assertTrue(validateVideoAdapter.isValidFormatVideo(aviFile));
    }

    @Test
    void shouldReturnFalseForXmlFile() throws IOException {
        File xmlFile = tempDir.resolve("data.xml").toFile();
        Files.writeString(xmlFile.toPath(), "<?xml version=\"1.0\"?><root/>");

        assertFalse(validateVideoAdapter.isValidFormatVideo(xmlFile));
    }
}
