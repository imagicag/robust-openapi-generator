// Copyright (C) 2025, Imagic Bildverarbeitung AG, Sägereistrasse 29, CH-8152 Glattbrugg
//
// This file will be replaced as part of the open api generation process DO NOT EDIT
//
// This file is provided under the following conditions:
// THE SOFTWARE IS PROVIDED “AS IS” AND THE AUTHOR DISCLAIMS ALL
// WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES
// OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE
// FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY
// DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
// AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
// OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
//
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * java.io.File implementation of the {@link BinaryPayload} interface.
 */
public class FileBinaryPayload implements BinaryPayload {

    protected volatile boolean closed;
    protected final boolean deleteOnClose;
    protected final File theFile;
    protected final List<WeakReference<InputStream>> streams = new ArrayList<>();

    public FileBinaryPayload(Path path) throws FileNotFoundException {
        this(path.toFile());
    }

    public FileBinaryPayload(Path path, boolean deleteOnClose) throws FileNotFoundException {
        this(path.toFile(), deleteOnClose);
    }

    public FileBinaryPayload(File file) throws FileNotFoundException {
        this(file, false);
    }

    public FileBinaryPayload(File file, boolean deleteOnClose) throws FileNotFoundException {
        if (!file.exists() || file.isDirectory()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        this.theFile = file;
        this.deleteOnClose = deleteOnClose;
    }

    @Override
    public OptionalLong contentLength() throws IOException {
        if (closed) {
            throw new IOException("Payload already closed");
        }
        return OptionalLong.of(Files.size(theFile.toPath()));
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public synchronized void writeTo(OutputStream outputStream) throws IOException {
        if (closed) {
            throw new IOException("Payload already closed");
        }

        try(FileInputStream inputStream = new FileInputStream(theFile)) {
            inputStream.transferTo(outputStream);
        }
    }

    @Override
    public synchronized InputStream read() throws IOException {
        if (closed) {
            throw new IOException("Payload already closed");
        }
        InputStream is = new FileInputStream(this.theFile);
        //Should we remove all expired stream references here?
        //For now no. I do not expect there to be thousands of streams opened during the lifetime of the payload.
        this.streams.add(new WeakReference<>(is));
        return is;
    }

    @Override
    public void close() throws RuntimeException {
        closed = true;

        synchronized (this) {
            for (WeakReference<InputStream> stream : streams) {
                InputStream inputStream = stream.get();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        //DC.
                    }

                }
            }

            if (deleteOnClose) {
                // Best effort, on ordinary linux/bsd this will work 100% of the time for a regular file we have permissions to delete.
                // On windows, it might not (thank you Windows-Defender!).
                // There are ways to work around this problem, such as enqueuing this file into a thread that tries to delete it once per minute until its gone.
                // However, such specialized needs should be handled by someone subclassing this class.
                theFile.delete();
            }
        }
    }
}
