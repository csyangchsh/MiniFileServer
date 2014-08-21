package com.csyangchsh.fs.fileupload;

import org.apache.commons.fileupload.RequestContext;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author csyangchsh
 * Date: 14/8/21
 */
public class RequestContextImpl implements RequestContext{

    private BasicHttpEntityEnclosingRequest request;

    public RequestContextImpl(BasicHttpEntityEnclosingRequest request) {
        this.request = request;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return request.getEntity().getContentType().getValue();
    }

    @Override
    public int getContentLength() {
        return (int)request.getEntity().getContentLength();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return request.getEntity().getContent();
    }
}
