package com.csyangchsh.fs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author csyangchsh
 * Date: 14/8/29
 */
public class TempFileFactory {
    private final String tmpdir;
    private final List<File> tempFiles;

    public TempFileFactory() {
        tmpdir = System.getProperty("java.io.tmpdir");
        tempFiles = new ArrayList<File>();
    }

    public File createTempFile() throws IOException {
        File tempFile = File.createTempFile("MiniFileServer-","", new File(tmpdir));
        tempFiles.add(tempFile);
        return tempFile;
    }

    public void clear() {
        for (File file : tempFiles) {
            try {
                file.delete();
            } catch (Exception ignored) {
            }
        }
        tempFiles.clear();
    }
}
