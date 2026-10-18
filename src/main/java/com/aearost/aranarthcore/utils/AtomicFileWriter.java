package com.aearost.aranarthcore.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Writes a file by writing to a temporary file first and only replacing the real file once the writer is closed.
 */
public class AtomicFileWriter extends FileWriter {

    private final File target;
    private final File temp;
    private boolean isClosed = false;

    public AtomicFileWriter(String path) throws IOException {
        this(new File(path));
    }

    public AtomicFileWriter(File target) throws IOException {
        this(target, new File(target.getPath() + ".tmp"));
    }

    private AtomicFileWriter(File target, File temp) throws IOException {
        super(temp);
        this.target = target;
        this.temp = temp;
    }

    /**
     * Closes the temporary file and moves it over the real file.
     */
    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }
        isClosed = true;
        super.close();
        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
