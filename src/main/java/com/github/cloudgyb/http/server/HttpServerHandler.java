package com.github.cloudgyb.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    private final FullHttpResponse clientRequestErrorResponse =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.BAD_REQUEST,
                    Unpooled.EMPTY_BUFFER);
    private HttpPostRequestDecoder postRequestDecoder;
    private DefaultNettyRequest nettyRequest;
    private ByteBuf body;
    private DiskCacheHttpData tempFile;// 用于缓存大请求体

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        if (msg instanceof HttpRequest request) {
            if (nettyRequest != null) { //在未结束当前请求数据的处理，又来一个 request 肯定是非法数据请求，直接响应 400并关闭连接
                ctx.writeAndFlush(clientRequestErrorResponse);
                ctx.close();
                return;
            }
            nettyRequest = new DefaultNettyRequest(request);
            boolean isXWwwFormEncoded = request.headers()
                    .containsValue(HttpHeaderNames.CONTENT_TYPE,
                            HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED, true);
            if (isXWwwFormEncoded || HttpPostRequestDecoder.isMultipart(request)) {
                postRequestDecoder = new HttpPostRequestDecoder(request);
            } else {
                if (request instanceof HttpContent content) {
                    cacheBody(ctx, content);
                } else {
                    ctx.fireChannelRead(msg);
                }
            }
        } else if (msg instanceof HttpContent httpContent) {
            boolean isLast = httpContent instanceof LastHttpContent;
            if (postRequestDecoder != null) {
                postRequestDecoder.offer(httpContent);
            } else {
                cacheBody(ctx, httpContent);
            }
            if (isLast) {
                if (postRequestDecoder == null) {
                    nettyRequest.setBody(body == null ? tempFile : new DefaultByteBufHolder(body));
                } else {
                    nettyRequest.setPostRequestDecoder(postRequestDecoder);
                    nettyRequest.setBody(new DefaultByteBufHolder(Unpooled.EMPTY_BUFFER));
                }
                ctx.fireChannelRead(nettyRequest);
                nettyRequest = null;
                body = null;
                tempFile = null;
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void cacheBody(ChannelHandlerContext ctx, HttpContent content) {
        ByteBuf buf = content.content();
        boolean isLast = content instanceof LastHttpContent;
        try {
            if (tempFile != null) {
                tempFile.addContent(buf, isLast);
                return;
            }
            if (body == null) {
                body = ctx.alloc().buffer(buf.readableBytes()).writeBytes(buf);
            } else {
                body.writeBytes(buf);
            }
            if (tempFile == null && body.readableBytes() > 2 * 1024 * 1024) {
                tempFile = new DiskCacheHttpData("temp_" + UUID.randomUUID().toString().replace("-", ""),
                        StandardCharsets.UTF_8,
                        10 * 1024 * 1024);
                tempFile.addContent(body, isLast);
                if (body.refCnt() > 0) {
                    ReferenceCountUtil.release(body);
                }
                body = null;
            }
        } catch (IOException ignore) {
        }
    }
}
