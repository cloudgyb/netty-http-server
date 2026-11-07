package com.github.cloudgyb.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ByteBufWriteFileTest {

    @Test
    public void testWriteFile() {
        try (RandomAccessFile raf = new RandomAccessFile("test.txt", "rw")) {
            raf.write("Hello, World!".getBytes());
            raf.seek(200);
            raf.write("Hello, World!2".getBytes());
            long filePointer = raf.getFilePointer();
            System.out.printf("filePointer: %d", filePointer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testFileChannelWriteFile() {
        try (RandomAccessFile raf = new RandomAccessFile("test.txt", "rw");
             FileChannel fileChannel = raf.getChannel()) {
            fileChannel.write(ByteBuffer.wrap("11111111111111".getBytes()));
            fileChannel.force(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testByteBufFileChannelWriteFile() {
        try (RandomAccessFile raf = new RandomAccessFile("test.txt", "rw");
             FileChannel fileChannel = raf.getChannel()) {
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeBytes("33333333333333".getBytes());
            buffer.getBytes(0, fileChannel, 2, buffer.readableBytes());
            fileChannel.force(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
