package com.github.cloudgyb.http.server;

import io.netty.buffer.ByteBufHolder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author cloudgyb
 * @since 2026/10/31 17:33
 */
public class DefaultNettyRequest implements NettyRequest {
    private final HttpRequest request;
    private Map<String, List<String>> queryParams;
    private ByteBufHolder body;
    /**
     * 用于文件上传 multipart或者x-www-form-encoded 的数据解码器
     */
    private HttpPostRequestDecoder postRequestDecoder;

    public DefaultNettyRequest(HttpRequest request) {
        this.request = request;
    }

    @Override
    public HttpHeaders httpHeaders() {
        return request.headers().copy();
    }

    @Override
    public Map<String, List<String>> queryParams() {
        if (queryParams == null) {
            synchronized (this) {
                if (queryParams != null) {
                    return queryParams;
                }
                QueryStringDecoder queryStringDecoder = QueryStringDecoder.builder().build(request.uri());
                queryParams = queryStringDecoder.parameters();
            }
        }
        return queryParams;
    }

    @Override
    public List<String> getQueryParams(String name) {
        Map<String, List<String>> map = queryParams();
        return map.get(name);
    }

    @Override
    public String getQueryParam(String name) {
        List<String> params = getQueryParams(name);
        return params != null && !params.isEmpty() ? params.get(0) : null;
    }

    @Override
    public String getParam(String name) {
        if (postRequestDecoder == null) {
            return null;
        }
        InterfaceHttpData bodyHttpData = postRequestDecoder.getBodyHttpData(name);
        if (bodyHttpData == null) {
            return null;
        }
        InterfaceHttpData.HttpDataType httpDataType = bodyHttpData.getHttpDataType();
        if (httpDataType == InterfaceHttpData.HttpDataType.Attribute && bodyHttpData instanceof Attribute attr) {
            try {
                return attr.getValue();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public ByteBufHolder body() {
        return body;
    }

    @Override
    public boolean isMultipart() {
        return postRequestDecoder != null && postRequestDecoder.isMultipart();
    }

    @Override
    public FileUpload getFileUpload(String name) {
        if (isMultipart()) {
            InterfaceHttpData bodyHttpData = postRequestDecoder.getBodyHttpData(name);
            if (bodyHttpData != null) {
                if (bodyHttpData instanceof FileUpload fileUpload) {
                    return fileUpload;
                }
            }
        }
        return null;
    }

    public void setBody(ByteBufHolder body) {
        if (this.body != null) {
            this.body.release();
        }
        this.body = body;
    }

    public void setPostRequestDecoder(HttpPostRequestDecoder postRequestDecoder) {
        this.postRequestDecoder = postRequestDecoder;
    }

    @Override
    public void destroy() {
        if (postRequestDecoder != null) {
            postRequestDecoder.destroy();
        }
        if (body != null) {
            body.release();
        }
        if (queryParams != null) {
            queryParams.clear();
            queryParams = null;
        }
        postRequestDecoder = null;
        body = null;
    }
}
