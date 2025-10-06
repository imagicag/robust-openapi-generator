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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * byte[] implementation of the {@link BinaryPayload} interface.
 */
public class ByteArrayBinaryPayload implements BinaryPayload {

    protected byte[] data;
    protected final int off;
    protected final int len;

    public ByteArrayBinaryPayload(byte[] data, int off, int len) {
        this.data = Objects.requireNonNull(data);
        this.off = off;
        this.len = len;
        if ((off >= data.length && off != 0) || off + len > data.length || len < 0 || off < 0) {
            throw new IllegalArgumentException("invalid offsets");
        }
    }

    public ByteArrayBinaryPayload(byte[] data) {
        this(data, 0, data.length);
    }

    public ByteArrayBinaryPayload(String data) {
        this(data, StandardCharsets.UTF_8);
    }

    public ByteArrayBinaryPayload(String data, Charset charset) {
        this(data.getBytes(charset));
    }

    @Override
    public OptionalLong contentLength() {
        return OptionalLong.of(len);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public byte[] toByteArray() {
        byte[] copy = new byte[len];
        System.arraycopy(data, off, copy, 0, len);
        return copy;
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(data, off, len);
    }

    @Override
    public InputStream read() {
        return new ByteArrayInputStream(data, off, len);
    }

    @Override
    public void close() throws RuntimeException {
        data = null;
    }

    @Override
    public String toString() {
        if (data == null) {
            return "ByteArrayBinaryPayload{closed}";
        }
        byte[] data = toByteArray();
        return "ByteArrayBinaryPayload{data=" + Arrays.toString(data) +"}";
    }

    @Override
    public boolean isExceptionSafe() {
        return true;
    }
}
