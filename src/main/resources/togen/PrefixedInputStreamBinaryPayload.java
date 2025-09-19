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

/**
 * InputStreamBinaryPayload that prefixes the stream with a fixed byte array.
 */
public class PrefixedInputStreamBinaryPayload extends InputStreamBinaryPayload {

    protected byte[] prefix;
    protected final int off;
    protected final int len;

    public PrefixedInputStreamBinaryPayload(byte[] prefix, InputStream inputStream) {
        this(prefix, 0, prefix.length, inputStream);
    }

    public PrefixedInputStreamBinaryPayload(byte[] prefix, int off, int len, InputStream inputStream) {
        super(inputStream);
        this.prefix = prefix;
        this.off = off;
        this.len = len;
        if (off >= prefix.length || off + len > prefix.length || off+len < 0) {
            throw new IllegalArgumentException("invalid offsets");
        }
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (closed) {
            throw new IOException("Payload already read/closed");
        }

        try {
            outputStream.write(prefix, off, len);
            inputStream.transferTo(outputStream);
        } finally {
            close();
        }
    }

    @Override
    public InputStream read() throws IOException {
        byte[] data = prefix;
        InputStream is = inputStream;
        if (closed || data == null || is == null) {
            throw new IOException("Payload already read/closed");
        }

        closed = true;
        prefix = null;
        return new InputStream() {
            private final byte[] blob = data;
            private int o = off;
            private final int l = o+len;
            private final InputStream inner = is;

            @Override
            public int read() throws IOException {
                if (o < l) {
                    return blob[o++] & 0xFF;
                }
                return is.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) {
                    return 0;
                }

                if (o < l) {
                    int toCopy = Math.min(len, l-o);
                    System.arraycopy(blob, o, b, off, toCopy);
                    o += toCopy;
                    return toCopy;
                }

                return super.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                inner.close();
            }
        };
    }

    @Override
    public void close() throws RuntimeException {
        super.close();
        prefix = null;
    }
}
