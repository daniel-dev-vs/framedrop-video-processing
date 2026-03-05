package com.framedrop.video_processing.adapters.out;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ZipFramesAdapterTest {

    private final ZipFramesAdapter zipFramesAdapter = new ZipFramesAdapter();

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateZipFileWithFrames() throws IOException {
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);
        Path zipDir = tempDir.resolve("zip");
        Files.createDirectories(zipDir);

        File frame1 = framesDir.resolve("frame_000001.png").toFile();
        File frame2 = framesDir.resolve("frame_000002.png").toFile();
        Files.writeString(frame1.toPath(), "frame1 content");
        Files.writeString(frame2.toPath(), "frame2 content");

        File result = zipFramesAdapter.zipFrames(List.of(frame1, frame2), zipDir, "test_frames.zip");

        assertNotNull(result);
        assertTrue(result.exists());
        assertEquals("test_frames.zip", result.getName());
    }

    @Test
    void shouldContainAllFramesInZip() throws IOException {
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);
        Path zipDir = tempDir.resolve("zip");
        Files.createDirectories(zipDir);

        File frame1 = framesDir.resolve("frame_000001.png").toFile();
        File frame2 = framesDir.resolve("frame_000002.png").toFile();
        File frame3 = framesDir.resolve("frame_000003.png").toFile();
        Files.writeString(frame1.toPath(), "content1");
        Files.writeString(frame2.toPath(), "content2");
        Files.writeString(frame3.toPath(), "content3");

        File zipFile = zipFramesAdapter.zipFrames(List.of(frame1, frame2, frame3), zipDir, "test.zip");

        List<String> entryNames = getZipEntries(zipFile);
        assertEquals(3, entryNames.size());
        assertTrue(entryNames.contains("frame_000001.png"));
        assertTrue(entryNames.contains("frame_000002.png"));
        assertTrue(entryNames.contains("frame_000003.png"));
    }

    @Test
    void shouldCreateZipWithEmptyFramesList() throws IOException {
        Path zipDir = tempDir.resolve("zip");
        Files.createDirectories(zipDir);

        File result = zipFramesAdapter.zipFrames(List.of(), zipDir, "empty.zip");

        assertNotNull(result);
        assertTrue(result.exists());
        List<String> entries = getZipEntries(result);
        assertTrue(entries.isEmpty());
    }

    @Test
    void shouldCreateZipInCorrectDirectory() throws IOException {
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);
        Path zipDir = tempDir.resolve("output");
        Files.createDirectories(zipDir);

        File frame = framesDir.resolve("frame.png").toFile();
        Files.writeString(frame.toPath(), "content");

        File result = zipFramesAdapter.zipFrames(List.of(frame), zipDir, "output.zip");

        assertEquals(zipDir.toFile(), result.getParentFile());
    }

    @Test
    void shouldPreserveFrameContent() throws IOException {
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);
        Path zipDir = tempDir.resolve("zip");
        Files.createDirectories(zipDir);

        String originalContent = "This is the frame content";
        File frame = framesDir.resolve("frame_000001.png").toFile();
        Files.writeString(frame.toPath(), originalContent);

        File zipFile = zipFramesAdapter.zipFrames(List.of(frame), zipDir, "test.zip");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            String content = new String(zis.readAllBytes());
            assertEquals(originalContent, content);
        }
    }

    @Test
    void shouldThrowRuntimeExceptionForInvalidFrameFile() {
        Path zipDir = tempDir.resolve("zip");

        File nonExistentFrame = new File(tempDir.toFile(), "nonexistent.png");

        assertThrows(RuntimeException.class,
                () -> zipFramesAdapter.zipFrames(List.of(nonExistentFrame), zipDir, "fail.zip"));
    }

    private List<String> getZipEntries(File zipFile) throws IOException {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
            }
        }
        return entries;
    }
}
