package com.csyangchsh.fs;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import com.csyangchsh.fs.fileupload.FileUploadImpl;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * @Author csyangchsh
 * Date: 14/8/7
 */
public class FileListHandler implements HttpRequestHandler {

    private File docRoot;
    private FileUploadImpl upload;

    public FileListHandler(final String root) {
        super();
        docRoot = new File(root);
        DiskFileItemFactory df = new DiskFileItemFactory();
        upload = new FileUploadImpl(df);
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        String uri = request.getRequestLine().getUri();

        if (method.equals("POST")) {
            if (!"/upload_action".equals(uri)) {
                StringEntity output = new StringEntity("Invalid Request");
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                response.setEntity(output);
                System.out.println("Invalid request...");
                return;
            }

            if (request instanceof BasicHttpEntityEnclosingRequest) {
                BasicHttpEntityEnclosingRequest req = (BasicHttpEntityEnclosingRequest) request;

                try {
                    List<FileItem> files = upload.parseRequest(req);
                    Iterator<FileItem> iterator = files.iterator();
                    while (iterator.hasNext()) {
                        FileItem item = iterator.next();
                        if (!item.isFormField()) {
                            String fileName = item.getName();
                            File f = new File(docRoot, fileName);
                            try {
                                item.write(f);
                            } catch (Exception e) {
                                System.out.println("Error during write file...");
                            }
                        }
                    }
                    response.setStatusCode(HttpStatus.SC_SEE_OTHER);
                    response.addHeader("Location","/");
                } catch (FileUploadException e) {
                    System.out.println("Error during process file...");
                }

            }  else {
                StringEntity output = new StringEntity("Invalid Request");
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                response.setEntity(output);
                System.out.println("Invalid request...");
            }
            return;
        }

        if("/".equals(uri)) {
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity output = new StringEntity(buildList(),
                    ContentType.create("text/html", Charset.forName("UTF-8")));
            response.setEntity(output);
            System.out.println("Serving file list...");
        } else if (uri.length() > 1) {
            String fileName = uri.substring(1);
            fileName = URLDecoder.decode(fileName, "UTF-8");
            FileEntity file = new FileEntity(new File(docRoot, fileName),ContentType.APPLICATION_OCTET_STREAM);
            response.setEntity(file);
            System.out.println("Serving file: " + fileName);
        }
    }


    private String buildList() {
        StringBuffer sb = new StringBuffer(500);
        sb.append("<html><title>File List</title><body>");
        String[] files = docRoot.list();
        int len = files.length;
        for (int i = 0; i < len; i++) {
            File curFile = new File(docRoot, files[i]);
            if (curFile.isDirectory()) {
                sb.append(files[i]);
                sb.append("<br />");
            } else {
                sb.append("<a href=\"" + "/" + files[i] + "\">" + files[i] + "</a>");
                sb.append("<br />");
            }
        }
        sb.append("<br /><br /><form enctype=\"multipart/form-data\" action=\"/upload_action\" method=\"post\">" +
                "<label>File:</label>&nbsp;<input type=\"file\" name=\"uploadfile\" /><br />" +
                "  <input type=\"submit\" value=\"upload\" />" +
                "</form>");
        sb.append("</body></html>");
        return sb.toString();
    }
}
