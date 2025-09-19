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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Interface for binary payloads to are sent to the api as a request body or received from the api as a response body.
 * This is typically used in endpoints of the application/octet-stream media type.
 */
public interface BinaryPayload extends AutoCloseable {

    /**
     * Returns the content length of the binary payload if available.
     */
    default OptionalLong contentLength() throws IOException {
        return OptionalLong.empty();
    }

    /**
     * returns true if the binary payload can be read multiple times.
     * If this returns false and another read is attempted then an IOException is thrown.
     */
    default boolean isRepeatable() {
        return false;
    }

    /**
     * Write the binary payload to an output stream.
     */
    void writeTo(OutputStream outputStream) throws IOException;

    /**
     * Returns an input stream that reads from the start of this binary payload.
     * If isRepeatable() returns false, then this method can only be called once.
     * Closing the underlying stream does not necessarily close the binary payload.
     * Closing the BinaryPayload may close all streams returned by this function.
     */
    InputStream read() throws IOException;


    /**
     * Returns the binary payload as a byte array.
     * This may throw an out-of-memory error for large payloads.
     */
    default byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(32, (int) contentLength().orElse(0)));
        writeTo(baos);
        return baos.toByteArray();
    }

    /**
     * Returns the binary payload as a byte array.
     * This may throw an out-of-memory error for large payloads.
     * This method will call close() on the BinaryPayload.
     */
    default byte[] toByteArrayAndClose() throws IOException {
        try {
            return toByteArray();
        } finally {
            close();
        }
    }

    /**
     * Parses the binary payload as a UTF-8 string.
     * This may throw an out-of-memory error for large payloads.
     */
    default String toStringUtf8() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(32, (int) contentLength().orElse(0)));
        writeTo(baos);
        return baos.toString(StandardCharsets.UTF_8);
    }

    /**
     * Discards the binary payload,
     * this operation is inteded to fully consume a response from a server so that the server assumes the request was handled gracefully.
     * It has no effect if the binary payload was not received from a server.
     *
     * Any errors encountered during the discard operation are silently ignored.
     * This method may block until the entire payload has been discarded or until an IO timeout occurs.
     */
    default void discard() {

    }

    /**
     * Write the binary payload to a file.
     * If the file already exists, it is truncated before writing.
     *
     * If an I/O error occurs, an attempt is made to delete the file before this method returny by throwing an exception.
     *
     * @param file the file to write to
     * @return always the same as the file parameter
     * @throws IOException if an I/O error occurs
     */
    default File toFile(File file) throws IOException {
        Objects.requireNonNull(file);

        try(FileOutputStream faos = new FileOutputStream(file, false)) {
            writeTo(faos);
        } catch (Exception e) {
            file.delete();
            throw e;
        }

        return file;
    }

    /**
     * Returns true if the binary payload can be used as a body of an ApiException.
     * Generally, if the BinaryPayload absolutely must run its close() method to prevent resource leaks, then this method should return false.
     * For a byte array backed BinaryPayload not running close() has no negative side-effect and therefore it returns true here.
     *
     * In addition to that, an ExceptionSafe BinaryPayload should ensure that its "toByteArray" method does not block for an unreasonably long time
     * as it is called during the generation of the exception message.
     *
     */
    default boolean isExceptionSafe() {
        return false;
    }

    /**
     * Signals the implementation that it can perform all associated cleanup operations such as closing streams and deleting files.
     * This may be a noop.
     * Calling this method multiple times has no effect.
     * This method should not throw an exception and is expected to silently ignore errors.
     */
    @Override
    void close() throws RuntimeException;


}
