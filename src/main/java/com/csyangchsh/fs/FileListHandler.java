package com.csyangchsh.fs;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Thanks to nanohttpd (https://github.com/NanoHttpd/nanohttpd),
 * uploading file processing was a simple version of it's POST request handler.
 *
 * @uthor csyangchsh
 * Date: 14/8/7
 *
 * Example of BasicHttpEntityEnclosingRequest.getEntity().getContent():
 *
 * ------WebKitFormBoundaryfAOFAQAemYvqSBBA\r\n
 * Content-Disposition: form-data; name="uploadfile"; filename="stoptest.sh"\r\n
 * Content-Type: application/octet-stream \r\n
 * \r\n
 * .....file content..............
 * ------WebKitFormBoundaryfAOFAQAemYvqSBBA--\r\n
 *
 */
public class FileListHandler implements HttpRequestHandler {

    private File docRoot;
    private TempFileFactory tempFileFactory;

    public FileListHandler(final String root, TempFileFactory fileFactory) {
        super();
        docRoot = new File(root);
        tempFileFactory = fileFactory;
    }

    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        String uri = request.getRequestLine().getUri();

        if (method.equals("POST")) {
            // handle file upload
            if ("/upload_action".equals(uri) && request instanceof BasicHttpEntityEnclosingRequest) {
                BasicHttpEntityEnclosingRequest req = (BasicHttpEntityEnclosingRequest) request;
                doFileUpload(req, response);
            }  else {
                doBadRequest(response);
            }
        } else if (method.equals("GET")) {
            // file list
            if("/".equals(uri)) {
                response.setStatusCode(HttpStatus.SC_OK);
                StringEntity output = new StringEntity(buildList(),
                        ContentType.create("text/html", Charset.forName("UTF-8")));
                response.setEntity(output);
            } else if (uri.length() > 1) {
                // file
                String fileName = uri.substring(1);
                fileName = URLDecoder.decode(fileName, "UTF-8");
                FileEntity file = new FileEntity(new File(docRoot, fileName),ContentType.APPLICATION_OCTET_STREAM);
                response.setEntity(file);
            }
        }
    }

    private void doBadRequest(HttpResponse response) throws IOException {
        StringEntity output = new StringEntity("Invalid Request");
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        response.setEntity(output);
        System.out.println("Invalid request...");
    }

    private void doFileUpload(BasicHttpEntityEnclosingRequest req, HttpResponse response) throws IOException {
        PushbackInputStream inputStream = new PushbackInputStream(req.getEntity().getContent());

        RandomAccessFile randomAccessFile = null;
        BufferedReader in = null;
        randomAccessFile = createTmpFile();
        long size = req.getEntity().getContentLength();
        int rlen = 0;

        // Write to temp file
        byte[] buf = new byte[512];
        while (rlen >= 0 && size > 0) {
            rlen = inputStream.read(buf, 0, (int)Math.min(size, 512));
            size -= rlen;
            if (rlen > 0) {
                randomAccessFile.write(buf, 0, rlen);
            }
        }

        // raw body
        ByteBuffer fbuf = randomAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, randomAccessFile.length());
        randomAccessFile.seek(0);

        // read as string, find boundary
        InputStream bin = new FileInputStream(randomAccessFile.getFD());
        in = new BufferedReader(new InputStreamReader(bin));
        String contentType = req.getEntity().getContentType().getValue();
        String boundaryStartString = "boundary=";
        int boundaryContentStart = contentType.indexOf(boundaryStartString) + boundaryStartString.length();
        String boundary = contentType.substring(boundaryContentStart, contentType.length());
        extractFile(boundary, fbuf, in);
        Util.safeClose(randomAccessFile);
        Util.safeClose(in);
        response.setStatusCode(HttpStatus.SC_SEE_OTHER);
        response.addHeader("Location","/");
    }

    private void extractFile(String boundary, ByteBuffer fbuf, BufferedReader in) throws IOException {
        int[] bpositions = getBoundaryPositions(fbuf, boundary.getBytes());
        int boundarycount = 1;
        String mpline = in.readLine();
        while (mpline != null) {
            if (!mpline.contains(boundary)) {
                //TODO throw exception
            }
            boundarycount++;
            Map<String, String> item = new HashMap<String, String>();
            mpline = in.readLine();
            while (mpline != null && mpline.trim().length() > 0) {
                int p = mpline.indexOf(':');
                if (p != -1) {
                    item.put(mpline.substring(0, p).trim().toLowerCase(Locale.US), mpline.substring(p + 1).trim());
                }
                mpline = in.readLine();
            }
            if (mpline != null) {
                String contentDisposition = item.get("content-disposition");
                if (contentDisposition == null) {
                    //TODO
                }
                StringTokenizer st = new StringTokenizer(contentDisposition, ";");
                Map<String, String> disposition = new HashMap<String, String>();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken().trim();
                    int p = token.indexOf('=');
                    if (p != -1) {
                        disposition.put(token.substring(0, p).trim().toLowerCase(Locale.US), token.substring(p + 1).trim());
                    }
                }
                String pname = disposition.get("name");
                pname = pname.substring(1, pname.length() - 1);

                String value = "";
                if (item.get("content-type") == null) {
                    while (mpline != null && !mpline.contains(boundary)) {
                        mpline = in.readLine();
                        if (mpline != null) {
                            int d = mpline.indexOf(boundary);
                            if (d == -1) {
                                value += mpline;
                            } else {
                                value += mpline.substring(0, d - 2);
                            }
                        }
                    }
                } else {
                    if (boundarycount > bpositions.length) {
                        //TODO
                    }
                    int offset = findFileStartPosition(fbuf, bpositions[boundarycount - 2]);
                    value = disposition.get("filename");
                    value = value.substring(1, value.length() - 1);
                    //Save file
                    saveFile(fbuf, offset, bpositions[boundarycount - 1] - offset - 4, value);
                    do {
                        mpline = in.readLine();
                    } while (mpline != null && !mpline.contains(boundary));
                }

        }
        }

    }

    /**
     * Save uploaded file to doc root
     *
     */
    private void saveFile(ByteBuffer b, int offset, int len, String fileName) throws IOException {
        if (len > 0) {
            FileOutputStream fileOutputStream = null;
            try {
                File file = new File(docRoot, fileName);
                fileOutputStream = new FileOutputStream(file);
                ByteBuffer src = b.duplicate();
                FileChannel dest = fileOutputStream.getChannel();
                src.position(offset).limit(offset + len);
                dest.write(src.slice());
            } catch (Exception e) {
                throw new IOException(e);
            } finally {
                Util.safeClose(fileOutputStream);
            }
        }
    }

    /**
     * Find start position of file content
     *
     */
    private int findFileStartPosition(ByteBuffer b, int offset) {
        int i;
        for (i = offset; i < b.limit(); i++) {
            if (b.get(i) == '\r' && b.get(++i) == '\n' && b.get(++i) == '\r' && b.get(++i) == '\n') {
                break;
            }
        }
        return i + 1;
    }

    /**
     * Find boundary position
     *
     */
    private int[] getBoundaryPositions(ByteBuffer b, byte[] boundary) {
        int matchcount = 0;
        int matchbyte = -1;
        List<Integer> matchbytes = new ArrayList<Integer>();
        for (int i = 0; i < b.limit(); i++) {
            if (b.get(i) == boundary[matchcount]) {
                if (matchcount == 0)
                    matchbyte = i;
                matchcount++;
                if (matchcount == boundary.length) {
                    matchbytes.add(matchbyte);
                    matchcount = 0;
                    matchbyte = -1;
                }
            } else {
                i -= matchcount;
                matchcount = 0;
                matchbyte = -1;
            }
        }
        int[] ret = new int[matchbytes.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = matchbytes.get(i);
        }
        return ret;
    }

    private RandomAccessFile createTmpFile() throws IOException {
        File tempFile = tempFileFactory.createTempFile();
        return new RandomAccessFile(tempFile.getAbsolutePath(), "rw");
    }

    /**
     * Build file list page
     *
     */
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
