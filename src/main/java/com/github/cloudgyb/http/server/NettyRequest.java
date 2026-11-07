package com.github.cloudgyb.http.server;

import io.netty.buffer.ByteBufHolder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.multipart.FileUpload;

import java.util.List;
import java.util.Map;

/**
 * @author cloudgyb
 * @since 2026/10/31 17:27
 */
public interface NettyRequest {
    HttpHeaders httpHeaders();

    // Query parameters
    Map<String, List<String>> queryParams();

    // Get query parameters by name
    List<String> getQueryParams(String name);

    // Get the first value of query parameter
    String getQueryParam(String name);

    /**
     * Get the value of body parameter by name
     *
     * @param name The name of the query parameter
     * @return The value of the query parameter, or null if not found
     */
    String getParam(String name);

    // Get request body
    ByteBufHolder body();

    /**
     * Check if the request is multipart
     *
     * @return true if the request is multipart, false otherwise
     */
    boolean isMultipart();

    /**
     * Get file by name, only works for multipart request
     *
     * @param name The name of the form field
     * @return The file, or null if not found
     */
    FileUpload getFileUpload(String name);

    // Destroy the request
    void destroy();
}
