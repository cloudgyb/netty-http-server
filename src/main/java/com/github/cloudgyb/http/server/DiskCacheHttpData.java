package com.github.cloudgyb.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.multipart.AbstractDiskHttpData;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.nio.charset.Charset;

/**
 * http 请求大body缓存，临时文件存储
 */
public class DiskCacheHttpData extends AbstractDiskHttpData {
    private String baseDirectory;

    protected DiskCacheHttpData(String name, Charset charset, long size, String baseDirectory) {
        super(name, charset, size);
        this.baseDirectory = baseDirectory;
    }

    protected DiskCacheHttpData(String name, Charset charset, long size) {
        super(name, charset, size);
    }

    @Override
    protected String getDiskFilename() {
        return "cache";
    }

    @Override
    protected String getPrefix() {
        return "cache";
    }

    @Override
    protected String getBaseDirectory() {
        return baseDirectory;
    }

    @Override
    protected String getPostfix() {
        return ".tmp";
    }

    @Override
    protected boolean deleteOnExit() {
        return false;
    }

    @Override
    public HttpData copy() {
        throw new UnsupportedOperationException("copy");
    }

    @Override
    public HttpData duplicate() {
        throw new UnsupportedOperationException("duplicate");
    }

    @Override
    public HttpData retainedDuplicate() {
        throw new UnsupportedOperationException("retainedDuplicate");
    }

    @Override
    public HttpData replace(ByteBuf content) {
        throw new UnsupportedOperationException("replace");
    }

    @Override
    public HttpDataType getHttpDataType() {
        throw new UnsupportedOperationException("getHttpDataType");
    }

    @Override
    public int compareTo(InterfaceHttpData o) {
        if (!(o instanceof DiskCacheHttpData)) {
            throw new ClassCastException("Cannot compare " + DiskCacheHttpData.class.getSimpleName() +
                    " with " + o.getClass().getSimpleName());
        }
        return getName().compareToIgnoreCase(o.getName());
    }
}
