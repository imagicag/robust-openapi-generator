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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exception thrown when an operation fails due to an unexpected error. <br>
 * <br>
 * The response may contain a response body. <br>
 * If it does then it is guaranteed to already <br>
 * be in a state where closing it is not necessary. <br>
 * even if the contained type might offer a close() method. <br>
 * This mostly applies to {@link BinaryPayload} types. <br>
 * <br>
 * Possible situations where this exception is thrown: <br>
 * - An IOException occurs during communication. <br>
 * - The server returns an unexpected HTTP status code. <br>
 * - The server returns an unexpected response body. <br>
 * - The server doesn't return all mandatory information. <br>
 * - User code (for ex. JSON parser) throws an exception. <br>
 * - User code attempts to use responses in the wrong way. <br>
 *   For example, the server returns HTTP 200 with its associated schema, but the user code calls getResponseBodyS201Json() instead of getResponseBodyS200Json() <br>
 * - Mandatory request parameters are missing. (Path parameters) <br>
 * <br>
 * Depending on at what stage the exception is thrown, the exception may contain more or less information. <br>
 * It always contains the operationId of the failed operation. <br>
 */
public class ApiException extends RuntimeException {

    private final String operationId;
    private final String message;
    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final Object body;

    public ApiException(String operationId, int statusCode, Map<String, List<String>> headers, Object body, String message, Throwable cause) {
        super(cause);
        this.message = message;
        this.operationId = operationId;
        if (statusCode > 999) {
            this.statusCode = 500;
        } else if (statusCode <= 0) {
            this.statusCode = -1;
        } else {
            this.statusCode = statusCode;
        }
        if (headers == null) {
            headers = Collections.emptyMap();
        }
        this.headers = headers;

        if (body instanceof BinaryPayload) {
            BinaryPayload binaryPayload = (BinaryPayload) body;
            // We don't want resource leaks
            // because it is too much to ask the catcher of the exception to close the stream.
            // This can only happen if the getter of an incorrect
            // variant was called in a response that contained a binary payload.
            if (!binaryPayload.isExceptionSafe()) {
                binaryPayload.close();
            }
        } else if (body instanceof AutoCloseable) {
            try {
                ((AutoCloseable) body).close();
            } catch (Exception e) {
                //DC.
            }
        }

        this.body = body;
    }

    public ApiException(String operationId, int statusCode, Map<String, List<String>> headers, Object body, String message) {
        this(operationId, statusCode, headers, body, message, null);
    }

    public ApiException(String operationId, int statusCode, Map<String, List<String>> headers, Throwable cause) {
        this(operationId, statusCode, headers, null, null, cause);
    }

    public ApiException(String operationId, int statusCode, Map<String, List<String>> headers, Object body) {
        this(operationId, statusCode, headers, body, null);
    }

    public ApiException(String operationId, String message) {
        this(operationId, -1, null, null, message);
    }

    public ApiException(String operationId, Throwable throwable) {
        this(operationId, -1, null, null, null, throwable);
    }

    public ApiException(Throwable throwable) {
        this("", throwable);
    }

    public ApiException(String message) {
        this("", message);
    }

    public boolean hasReceivedResponseFromServer() {
        return statusCode > 0;
    }

    public String getOperationId() {
        return operationId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }

    @Override
    public String getMessage() {
        if (!hasReceivedResponseFromServer()) {
            return "Internal error making request operation: " + operationId + " error: " + message;
        }
        if (message != null) {
            if (body == null) {
                return "Internal error while parsing response for operation: " + operationId + " error: " + message + " response received from server: HTTP " + statusCode + " no body";
            }

            Object body = this.body;
            if (body instanceof BinaryPayload) {
                BinaryPayload binaryPayload = (BinaryPayload) body;
                try {
                    if (binaryPayload.isExceptionSafe() && binaryPayload.isRepeatable() && binaryPayload.contentLength().orElse(Long.MAX_VALUE) < 1024) {
                        body = binaryPayload.toByteArray();
                    }
                } catch (IOException e) {
                    //DC, exception safe BinaryPayloads should not block or error.
                }
            }

            if (body instanceof byte[]) {
                byte[] bytes = (byte[]) body;
                body = "Binary data of length " + bytes.length;
                if (bytes.length < 1024) {
                    if (isUTF8(bytes)) {
                        body = new String(bytes, StandardCharsets.UTF_8);
                    } else if (bytes.length < 64) {
                        body = "Binary data " + Arrays.toString(bytes);
                    }
                }
            }

            return "Internal error while parsing response for operation: " + operationId + " error: " + message + " response received from server: HTTP " + statusCode + " body: " + body;
        }
        if (body == null) {
            return "HTTP " +  statusCode + " received from server";
        }

        return "HTTP " +  statusCode + "body: " + body + " received from server";
    }

    private static boolean isUTF8(byte[] bytes) {
        for (int i = 0; i < bytes.length;) {
            if ((bytes[i++] & 0b10000000) == 0b00000000) {
                continue;
            }

            if ((bytes[i] & 0b11100000) == 0b11000000) {
                if ((bytes[i++] & 0b11000000) != 0b10000000) {
                    return false;
                }
                continue;
            }
            if ((bytes[i] & 0b11110000) == 0b11100000) {
                for (int x = 0; x < 2; x++) {
                    if ((bytes[i++] & 0b11000000) != 0b10000000) {
                        return false;
                    }
                }
                continue;
            }
            if ((bytes[i] & 0b11111000) == 0b11110000) {
                for (int x = 0; x < 3; x++) {
                    if ((bytes[i++] & 0b11000000) != 0b10000000) {
                        return false;
                    }
                }
                continue;
            }

            // Yes there is longer utf-8 emojis and stuff, but that is hardly suitable for a log message,
            // so we just pretend that that is not utf-8.

            return false;
        }

        return true;
    }

}
