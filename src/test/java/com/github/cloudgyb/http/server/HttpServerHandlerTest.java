package com.github.cloudgyb.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.logging.LoggingHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/**
 * @author cloudgyb
 * @since 2026/10/31 17:50
 */
public class HttpServerHandlerTest {
    EmbeddedChannel embeddedChannel;

    @Before
    public void init() {
        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new LoggingHandler())
                .addLast(new HttpServerHandler());
    }

    @After
    public void destroy() {
        embeddedChannel.close();
    }

    @Test
    public void testNoQueryParams() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertTrue("QueryParams should be empty", msg.queryParams().isEmpty());
                        Assert.assertNull("name should be null", msg.getQueryParam("name"));
                        Assert.assertNull("age should be null", msg.getQueryParam("age"));
                        Assert.assertEquals(0, msg.body().content().readableBytes());
                    }
                });
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("GET / HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);

        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testQueryParams() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals("test", msg.getQueryParam("name"));
                        Assert.assertEquals("123", msg.getQueryParam("age"));
                        Assert.assertEquals(1, msg.httpHeaders().size());
                    }
                });
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes("GET /?name=test&age=123 HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPutSmallBody() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals(1024, msg.body().content().readableBytes());
                    }
                });
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes("PUT /?name=test&age=123 HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
        buffer.writeBytes("Host: test.com\r\n".getBytes(StandardCharsets.UTF_8));
        buffer.writeBytes("Content-Length: 1024\r\n".getBytes(StandardCharsets.UTF_8));
        buffer.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < 1024; i++) {
            buffer.writeByte('a');
        }
        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPut1MBody() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals(1024 * 1024, msg.body().content().readableBytes());
                    }
                });
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("PUT /?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Length: 1048576\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        for (int i = 0; i < 1024 * 1024; i++) {
            buffer.writeByte('a');
        }
        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPutEQ2MBody() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals(1024 * 1024 * 2, msg.body().content().readableBytes());
                        Assert.assertFalse(msg.body() instanceof DiskCacheHttpData);
                    }
                });
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("PUT /?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Length: 2097152\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        for (int i = 0; i < 1024 * 1024 * 2; i++) {
            buffer.writeByte('a');
        }
        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPutGT2MBody() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals(1024 * 1024 * 2 + 10, msg.body().content().readableBytes());
                        Assert.assertTrue(msg.body() instanceof DiskCacheHttpData);
                        msg.destroy();
                    }
                });
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("PUT /?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Length: 2097162\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        for (int i = 0; i < 1024 * 1024 * 2 + 10; i++) {
            buffer.writeByte('a');
        }
        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPostXWwwFormUrlencodedBody() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals("test", msg.getQueryParam("name"));
                        Assert.assertEquals("123", msg.getQueryParam("age"));
                        Assert.assertEquals(0, msg.body().content().readableBytes());
                        Assert.assertEquals("value1", msg.getParam("param1"));
                        Assert.assertEquals("value2", msg.getParam("param2"));
                        Assert.assertNull(msg.getParam("param3"));
                    }
                });
        byte[] body = "param1=value1&param2=value2".getBytes(StandardCharsets.UTF_8);
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("POST /?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence(String.format("Content-Length: %d\r\n", body.length), StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Type: application/x-www-form-urlencoded\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);

        buffer.writeBytes(body);

        embeddedChannel.writeInbound(buffer);
    }


    @Test
    public void testPostMultipartBody() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertEquals("test", msg.getQueryParam("name"));
                        Assert.assertEquals("123", msg.getQueryParam("age"));
                        Assert.assertEquals(0, msg.body().content().readableBytes());
                        Assert.assertEquals("value1", msg.getParam("param1"));
                        Assert.assertEquals("value2", msg.getParam("param2"));
                        Assert.assertNull(msg.getParam("param3"));
                    }
                });
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String contentType = "multipart/form-data; boundary=" + boundary;
        // param1
        String bodyBuilder = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"param1\"\r\n\r\n" +
                "value1\r\n" +
                // param2
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"param2\"\r\n\r\n" +
                "value2\r\n" +
                // end
                "--" + boundary + "--\r\n";
        byte[] body = bodyBuilder.getBytes(StandardCharsets.UTF_8);

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("POST /upload?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Type: " + contentType + "\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence(String.format("Content-Length: %d\r\n", body.length), StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        buffer.writeBytes(body);

        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPostMultipartBodyWithFile() {
        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertTrue(msg.isMultipart());
                        Assert.assertEquals("test", msg.getQueryParam("name"));
                        Assert.assertEquals("123", msg.getQueryParam("age"));
                        Assert.assertEquals(0, msg.body().content().readableBytes());
                        // form-data 属性
                        Assert.assertEquals("value1", msg.getParam("param1"));
                        Assert.assertEquals("value2", msg.getParam("param2"));
                        Assert.assertNull(msg.getParam("param3"));
                        // 文件上传
                        try {
                            FileUpload fileUpload = msg.getFileUpload("file");
                            Assert.assertNotNull(fileUpload);
                            String content = fileUpload.content().toString(StandardCharsets.UTF_8);
                            Assert.assertEquals("hello world\nthis is file content", content);
                        } finally {
                            msg.destroy();
                        }
                    }
                });
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String contentType = "multipart/form-data; boundary=" + boundary;
        String fileContent = "hello world\nthis is file content";
        // param1
        String bodyBuilder = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"param1\"\r\n\r\n" +
                "value1\r\n" +
                // param2
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"param2\"\r\n\r\n" +
                "value2\r\n" +
                // file
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
                "Content-Type: text/plain\r\n\r\n" +
                fileContent + "\r\n" +
                // end
                "--" + boundary + "--\r\n";
        byte[] body = bodyBuilder.getBytes(StandardCharsets.UTF_8);

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("POST /upload?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Type: " + contentType + "\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence(String.format("Content-Length: %d\r\n", body.length), StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        buffer.writeBytes(body);

        embeddedChannel.writeInbound(buffer);
    }

    @Test
    public void testPostMultipartLargeFileUpload() {
        final int fileSize = 5 * 1024 * 1024;
        final byte[] fileContent = new byte[fileSize];
        for (int i = 0; i < fileSize; i++) {
            fileContent[i] = (byte) (i % 256);
        }

        embeddedChannel.pipeline()
                .addLast(new SimpleChannelInboundHandler<NettyRequest>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, NettyRequest msg) {
                        Assert.assertTrue(msg.isMultipart());
                        // 混合 form-data 属性
                        Assert.assertEquals("value1", msg.getParam("param1"));
                        try {
                            FileUpload fileUpload = msg.getFileUpload("file");
                            Assert.assertNotNull(fileUpload);
                            Assert.assertEquals("bigfile.bin", fileUpload.getFilename());
                            Assert.assertEquals(fileSize, fileUpload.length());
                            // 大文件应溢出到磁盘
                            Assert.assertFalse("large file should be spilled to disk", fileUpload.isInMemory());
                            // 校验文件内容完整无截断
                            ByteBuf content = fileUpload.content();
                            byte[] actual = new byte[content.readableBytes()];
                            content.getBytes(content.readerIndex(), actual);
                            Assert.assertArrayEquals(fileContent, actual);
                        } finally {
                            msg.destroy();
                        }
                    }
                });

        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String contentType = "multipart/form-data; boundary=" + boundary;
        byte[] prefix = ("--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"param1\"\r\n\r\n" +
                "value1\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"bigfile.bin\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        int bodyLength = prefix.length + fileContent.length + suffix.length;

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeCharSequence("POST /upload?name=test&age=123 HTTP/1.1\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Host: test.com\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Type: " + contentType + "\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("Content-Length: " + bodyLength + "\r\n", StandardCharsets.UTF_8);
        buffer.writeCharSequence("\r\n", StandardCharsets.UTF_8);
        buffer.writeBytes(prefix);
        buffer.writeBytes(fileContent);
        buffer.writeBytes(suffix);

        embeddedChannel.writeInbound(buffer);
    }


}
