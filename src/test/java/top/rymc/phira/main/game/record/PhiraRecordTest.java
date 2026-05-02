package top.rymc.phira.main.game.record;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhiraRecordTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("phira-record-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }

    private PhiraRecord createEmptyRecord() throws Exception {
        Constructor<PhiraRecord> constructor = PhiraRecord.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Test
    @DisplayName("should generate correct byte stream with file header when encode")
    void shouldGenerateCorrectByteStreamWithFileHeaderWhenEncode() {
        List<TouchFrame> touchFrames = new ArrayList<>();
        List<JudgeEvent> judgeEvents = new ArrayList<>();
        PhiraRecord record = new PhiraRecord(1, System.currentTimeMillis(), 100, "TestChart", 1, "Player1", touchFrames, judgeEvents);

        ByteBuf buf = Unpooled.buffer();
        record.encode(buf);

        assertThat(buf.readableBytes()).isGreaterThan(8);

        byte[] header = new byte[8];
        buf.readBytes(header);
        assertThat(new String(header)).isEqualTo("PHIRAREC");

        buf.release();
    }

    @Test
    @DisplayName("should correctly parse JPhiraRec format when decode")
    void shouldCorrectlyParseJPhiraRecFormatWhenDecode() throws Exception {
        List<TouchFrame> touchFrames = new ArrayList<>();
        List<JudgeEvent> judgeEvents = new ArrayList<>();
        PhiraRecord original = new PhiraRecord(1, System.currentTimeMillis(), 100, "TestChart", 1, "Player1", touchFrames, judgeEvents);

        ByteBuf buf = Unpooled.buffer();
        original.encode(buf);

        PhiraRecord decoded = createEmptyRecord();
        decoded.decode(buf);

        assertThat(decoded.getId()).isEqualTo(original.getId());
        assertThat(decoded.getChart()).isEqualTo(original.getChart());
        assertThat(decoded.getChartName()).isEqualTo(original.getChartName());
        assertThat(decoded.getUser()).isEqualTo(original.getUser());
        assertThat(decoded.getUserName()).isEqualTo(original.getUserName());

        buf.release();
    }

    @Test
    @DisplayName("should throw exception for unknown file type when decode")
    void shouldThrowExceptionForUnknownFileTypeWhenDecode() throws Exception {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("UNKNOWN!!!".getBytes());

        PhiraRecord record = createEmptyRecord();

        assertThatThrownBy(() -> record.decode(buf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown file type");

        buf.release();
    }

    @Test
    @DisplayName("should save record to correct path with correct filename")
    void shouldSaveRecordToCorrectPathWithCorrectFilename() {
        List<TouchFrame> touchFrames = new ArrayList<>();
        List<JudgeEvent> judgeEvents = new ArrayList<>();
        PhiraRecord record = new PhiraRecord(12345, System.currentTimeMillis(), 100, "TestChart", 1, "Player1", touchFrames, judgeEvents);

        boolean success = PhiraRecord.saveAsFile(record, tempDir);

        assertThat(success).isTrue();
        assertThat(tempDir.resolve("12345.phirarec")).exists();
    }

    @Test
    @DisplayName("should read all phirarec files from directory")
    void shouldReadAllPhirarecFilesFromDirectory() {
        List<TouchFrame> touchFrames = new ArrayList<>();
        List<JudgeEvent> judgeEvents = new ArrayList<>();
        PhiraRecord record1 = new PhiraRecord(1, System.currentTimeMillis(), 100, "Chart1", 1, "Player1", touchFrames, judgeEvents);
        PhiraRecord record2 = new PhiraRecord(2, System.currentTimeMillis(), 200, "Chart2", 2, "Player2", touchFrames, judgeEvents);

        PhiraRecord.saveAsFile(record1, tempDir);
        PhiraRecord.saveAsFile(record2, tempDir);

        List<PhiraRecord> records = PhiraRecord.readFromDirectory(tempDir);

        assertThat(records).hasSize(2);
    }

    @Test
    @DisplayName("should return empty list for non-existent directory")
    void shouldReturnEmptyListForNonExistentDirectory() {
        Path nonExistentDir = tempDir.resolve("non-existent");

        List<PhiraRecord> records = PhiraRecord.readFromDirectory(nonExistentDir);

        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("should filter non-phirarec files")
    void shouldFilterNonPhirarecFiles() throws IOException {
        List<TouchFrame> touchFrames = new ArrayList<>();
        List<JudgeEvent> judgeEvents = new ArrayList<>();
        PhiraRecord record = new PhiraRecord(1, System.currentTimeMillis(), 100, "Chart1", 1, "Player1", touchFrames, judgeEvents);
        PhiraRecord.saveAsFile(record, tempDir);

        Files.createFile(tempDir.resolve("not-a-record.txt"));
        Files.createFile(tempDir.resolve("another-file.log"));

        List<PhiraRecord> records = PhiraRecord.readFromDirectory(tempDir);

        assertThat(records).hasSize(1);
    }

    @Test
    @DisplayName("should return empty list when directory has no phirarec files")
    void shouldReturnEmptyListWhenDirectoryHasNoPhirarecFiles() throws IOException {
        Files.createFile(tempDir.resolve("not-a-record.txt"));
        Files.createFile(tempDir.resolve("another-file.log"));

        List<PhiraRecord> records = PhiraRecord.readFromDirectory(tempDir);

        assertThat(records).isEmpty();
    }
}
