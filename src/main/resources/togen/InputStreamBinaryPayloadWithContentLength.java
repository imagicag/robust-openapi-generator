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
import java.io.InputStream;
import java.util.OptionalLong;

/**
 * InputStreamBinaryPayload that knows its length from a 'Content-Length' header.
 */
public class InputStreamBinaryPayloadWithContentLength extends InputStreamBinaryPayload {

    protected final long contentLength;

    public InputStreamBinaryPayloadWithContentLength(InputStream inputStream, long contentLength) {
        super(inputStream);
        if (contentLength < 0) {
            throw new IllegalArgumentException("contentLength must be >= 0");
        }
        this.contentLength = contentLength;
    }

    @Override
    public OptionalLong contentLength() {
        return OptionalLong.of(contentLength);
    }
}
