package com.csyangchsh.fs.fileupload;


import org.apache.commons.fileupload.*;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author csyangchsh
 * Date: 14/8/21
 */
public class FileUploadImpl extends FileUpload {

    private static final String POST_METHOD = "POST";

    public static final boolean isMultipartContent(
            BasicHttpEntityEnclosingRequest request) {
        if (!POST_METHOD.equalsIgnoreCase(request.getRequestLine().getMethod())) {
            return false;
        }
        return true;
    }

    public FileUploadImpl() {
        super();
    }

    public FileUploadImpl(FileItemFactory fileItemFactory) {
        super(fileItemFactory);
    }

    public List<FileItem> parseRequest(BasicHttpEntityEnclosingRequest request)
            throws FileUploadException {
        return super.parseRequest(new RequestContextImpl(request));
    }

    public Map<String, List<FileItem>> parseParameterMap(BasicHttpEntityEnclosingRequest request)
            throws FileUploadException {
        return parseParameterMap(new RequestContextImpl(request));
    }

    public FileItemIterator getItemIterator(BasicHttpEntityEnclosingRequest request)
            throws FileUploadException, IOException {
        return super.getItemIterator(new RequestContextImpl(request));
    }
}
