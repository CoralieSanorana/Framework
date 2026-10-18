package main.com.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.InvocationTargetException;

public class FileUpload {
    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public FileUpload(String fileName, String contentType, byte[] content) {
        this.fileName = Paths.get(fileName).getFileName().toString();
        this.contentType = contentType;
        this.content = content;
    }

    public String getFileName()    { return fileName; }
    public String getContentType() { return contentType; }
    public long   getSize()        { return content.length; }
    public byte[] getBytes()       { return content; }

    public Path save(String directory) throws IOException {
        Path dir = Paths.get(directory);
        Files.createDirectories(dir);
        String uniqueName = java.util.UUID.randomUUID() + "_" + fileName;
        Path target = dir.resolve(uniqueName);
        Files.write(target, content);
        return target;
    }
}