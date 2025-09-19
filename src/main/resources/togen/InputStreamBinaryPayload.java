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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * java.io.InputStream implementation of the {@link BinaryPayload} interface.
 */
public class InputStreamBinaryPayload implements BinaryPayload {

    protected volatile boolean closed;
    protected final InputStream inputStream;

    public InputStreamBinaryPayload(InputStream inputStream) {
        this.inputStream = Objects.requireNonNull(inputStream);
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (closed) {
            throw new IOException("Payload already read/closed");
        }
        try {
            inputStream.transferTo(outputStream);
        } finally {
            close();
        }
    }

    @Override
    public InputStream read() throws IOException {
        if (closed) {
            throw new IOException("Payload already read/closed");
        }
        closed = true;
        return inputStream;
    }

    @Override
    public void discard() {
        try {
            if (closed) {
                return;
            }

            byte[] buffer = new byte[4096];
            int read = 0;
            while(read != -1) {
                read = inputStream.read(buffer);
            }
        } catch (Exception e) {
            //DC
        } finally {
            close();
        }
    }


    @Override
    public void close() throws RuntimeException {
        closed = true;
        try {
            inputStream.close();
        } catch (IOException e) {
            //DC
        }
    }

    @Override
    public String toString() {
        if (closed) {
            return "InputStreamBinaryPayload{closed}";
        }
        return "InputStreamBinaryPayload{open}";
    }
}
