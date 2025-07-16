package com.pipemasters.serveraudio.feign;

import feign.Param;
import feign.RequestLine;

public interface FileClient {
    @RequestLine("POST /api/v1/files/upload-url-audio?sourceKey={sourceKey}")
    String getUploadUrlAudio(@Param("sourceKey") String sourceKey);

    @RequestLine("GET /api/v1/files/download-url?sourceKey={sourceKey}")
    String getDownloadUrl(@Param("sourceKey") String sourceKey);
}
