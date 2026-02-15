package top.rymc.phira.main.game.record;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.rymc.phira.main.util.PhiraFetcher;
import top.rymc.phira.protocol.PacketRegistry;
import top.rymc.phira.protocol.codec.Encodeable;
import top.rymc.phira.protocol.data.monitor.judge.JudgeEvent;
import top.rymc.phira.protocol.data.monitor.touch.TouchFrame;
import top.rymc.phira.protocol.handler.server.ServerBoundPacketHandler;
import top.rymc.phira.protocol.handler.server.SimpleServerBoundPacketHandler;
import top.rymc.phira.protocol.packet.ServerBoundPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundJudgesPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundPingPacket;
import top.rymc.phira.protocol.packet.serverbound.ServerBoundTouchesPacket;
import top.rymc.phira.protocol.util.NettyPacketUtil;
import top.rymc.phira.protocol.util.PacketWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Getter
public final class PhiraRecord implements Encodeable {

    private static final Logger LOGGER = LogManager.getLogger("PhiraRecord");

    private static final FileHeader fileHeader = new FileHeader();
    private static final int fileVersion = 0;

    private FileHeader.FileType fileType;
    private int id;
    private int chart;
    private String chartName;
    private int user;
    private String userName;
    private List<TouchFrame> touchFrames;
    private List<JudgeEvent> judgeEvents;

    private PhiraRecord() {
    }

    public PhiraRecord(int id, int chart, String chartName, int user, String userName, List<TouchFrame> touchFrames, List<JudgeEvent> judgeEvents) {
        this.id = id;
        this.chart = chart;
        this.chartName = chartName;
        this.user = user;
        this.userName = userName;
        this.touchFrames = touchFrames;
        this.judgeEvents = judgeEvents;
    }

    @Override
    public void encode(ByteBuf byteBuf) {
        PacketWriter.write(byteBuf, fileHeader);
        PacketWriter.write(byteBuf, fileVersion);
        PacketWriter.write(byteBuf, id);
        PacketWriter.write(byteBuf, chart);
        PacketWriter.write(byteBuf, chartName);
        PacketWriter.write(byteBuf, user);
        PacketWriter.write(byteBuf, userName);
        PacketWriter.write(byteBuf, touchFrames);
        PacketWriter.write(byteBuf, judgeEvents);
    }

    public void decode(ByteBuf buf) {
        this.fileType = fileHeader.check(buf);
        if (fileType == FileHeader.FileType.Unknown) {
            throw new IllegalArgumentException("Unknown file type: file header does not match JPhiraRec or TPhiraRec format");
        }

        if (fileType == FileHeader.FileType.TPhiraRec) {
            try {
                decodeAsTPhiraRec(buf);
            } catch (IOException | CodecException e) {
                throw new RuntimeException("Failed to decode TPhiraRec format record", e);
            }
            return;
        }

        int version = buf.readIntLE();
        if (version != fileVersion) {
            throw new IllegalArgumentException(
                String.format("Unsupported file version: %d (supported version: %d)", version, fileVersion));
        }
        this.id = buf.readIntLE();
        this.chart = buf.readIntLE();
        this.chartName = NettyPacketUtil.decodeString(buf,Short.MAX_VALUE);
        this.user = buf.readIntLE();
        this.userName = NettyPacketUtil.decodeString(buf,Short.MAX_VALUE);
        this.touchFrames = NettyPacketUtil.decodeList(buf, TouchFrame::decode);
        this.judgeEvents = NettyPacketUtil.decodeList(buf, JudgeEvent::decode);
    }

    private void decodeAsTPhiraRec(ByteBuf buf) throws IOException {
        this.chart = buf.readIntLE();
        this.chartName = PhiraFetcher.GET_CHART_INFO.apply(chart).getName();
        this.user = buf.readIntLE();
        this.userName = PhiraFetcher.GET_USER_INFO_BY_ID.apply(user).getName();
        this.id = buf.readIntLE();

        this.touchFrames = new ArrayList<>();
        this.judgeEvents = new ArrayList<>();

        ServerBoundPacketHandler dataProcessor = new SimpleServerBoundPacketHandler(null) {

            @Override
            public void handle(ServerBoundPingPacket packet) {
            }


            @Override
            public void handle(ServerBoundTouchesPacket packet) {
                touchFrames.addAll(packet.getFrames());
            }

            @Override
            public void handle(ServerBoundJudgesPacket packet) {
                judgeEvents.addAll(packet.getJudges());
            }
        };

        while (buf.readableBytes() > 0) {
            int length = buf.readIntLE();
            if (length < 0) {
                throw new CorruptedFrameException(
                    String.format("Invalid packet length: %d (negative value)", length));
            }
            if (buf.readableBytes() < length) {
                throw new CorruptedFrameException(
                    String.format("Insufficient data for packet: required %d bytes, but only %d bytes available",
                        length, buf.readableBytes()));
            }

            ServerBoundPacket packet = PacketRegistry.ServerBound.decode(buf.readRetainedSlice(length));
            packet.handle(dataProcessor);
        }
    }


    public static boolean saveAsFile(PhiraRecord record, Path path) {
        File file = path.resolve(String.format("%d.phirarec", record.id)).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean success = PhiraRecordIO.saveRecordToFile(record, file);
        if (!success) {
            LOGGER.error("Failed to save record to file: id={}, path={}", record.id, file.getAbsolutePath());
        }
        return success;
    }

    public static List<PhiraRecord> readFromDirectory(Path dirPath) {
        File dir = dirPath.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        if (!dir.isDirectory()) {
            LOGGER.warn("Path is not a directory: {}", dir.getAbsolutePath());
            return new ArrayList<>();
        }

        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".phirarec"));
        if (files == null) {
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .map(PhiraRecordIO::readRecordFromFile)
                .filter(Objects::nonNull)
                .toList();
    }

    private static class PhiraRecordIO {
        private static boolean saveRecordToFile(PhiraRecord record, File file) {
            ByteBuf byteBuf = Unpooled.buffer();

            try (FileOutputStream fos = new FileOutputStream(file);
                 FileChannel channel = fos.getChannel()) {

                record.encode(byteBuf);

                ByteBuffer buffer = byteBuf.nioBuffer();
                channel.write(buffer);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to save record to file: {} - {}", file.getAbsolutePath(), e.getMessage(), e);
                return false;
            } finally {
                byteBuf.release();
            }
        }

        private static PhiraRecord readRecordFromFile(File file) {
            PhiraRecord record = new PhiraRecord();

            try (FileInputStream fis = new FileInputStream(file);
                 FileChannel channel = fis.getChannel()) {

                long fileSize = channel.size();
                if (fileSize > Integer.MAX_VALUE) {
                    LOGGER.error("Record file too large: {} ({} bytes)", file.getAbsolutePath(), fileSize);
                    return null;
                }

                ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
                channel.read(buffer);
                buffer.flip();

                ByteBuf byteBuf = Unpooled.wrappedBuffer(buffer);
                try {
                    record.decode(byteBuf);
                    return record;
                } finally {
                    byteBuf.release();
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Invalid record file format: {} - {}", file.getAbsolutePath(), e.getMessage());
                return null;
            } catch (Exception e) {
                LOGGER.error("Failed to read record from file: {} - {}", file.getAbsolutePath(), e.getMessage(), e);
                return null;
            }
        }
    }

    private static final class FileHeader implements Encodeable {

        private static final byte[] fileHeader = new byte[] { 'P', 'H', 'I', 'R', 'A', 'R', 'E', 'C' };

        @Override
        public void encode(ByteBuf buf) {
            buf.writeBytes(fileHeader);
        }

        public FileType check(ByteBuf buf) {

            short head = buf.getShort(buf.readerIndex());
            if (head == 0x504d || head == 0x4d50) {
                buf.readShort();
                return FileType.TPhiraRec;
            }


            byte[] header = new byte[fileHeader.length];
            buf.readBytes(header);
            for (int i = 0; i < fileHeader.length; i++) {
                if (header[i] != fileHeader[i]) {
                    return FileType.Unknown;
                }
            }

            return FileType.JPhiraRec;
        }

        @Getter
        @RequiredArgsConstructor
        private enum FileType {
            JPhiraRec,
            TPhiraRec,
            Unknown
        }
    }
}
