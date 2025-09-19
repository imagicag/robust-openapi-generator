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

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Marker interface for request parameters.
 */
public interface RequestParameters {

    /**
     * Returns the desired request timeout.
     * Null means use default timeout from the api client.
     */
    Duration getRequestTimeout();

    /**
     * Set the desired request timeout.
     * Null means use default timeout from the api client.
     */
    void setRequestTimeout(Duration requestTimeout);

    /**
     * Set the desired response body read timeout.
     * <br>
     * The read timeout is the maximum amount of time the client will wait for the server to send any amount of data,
     * after having received the status codes and headers. Once any amount of data is received from the server,
     * the read timeout will be restarted.
     * <br>
     * Null means use default timeout from the api client.
     */
    void setResponseBodyReadTimeout(Duration timeout);

    /**
     * Returns the desired response body read timeout.
     * <br>
     * The read timeout is the maximum amount of time the client will wait for the server to send any amount of data,
     * after having received the status codes and headers. Once any amount of data is received from the server,
     * the read timeout will be restarted.
     * <br>
     * Null means use default timeout from the api client.
     */
    Duration getResponseBodyReadTimeout();

    /**
     * Set the desired total response body read timeout.
     * <br>
     * The total read timeout is the maximum amount of time the client will wait for the server to send the entire response body.
     * After this amount of time, if the entire body has not been received yet, the client will abort the request.
     * <br>
     * Null means use default timeout from the api client.
     */
    void setResponseBodyTotalTimeout(Duration timeout);

    /**
     * Returns the desired total response body read timeout.
     * <br>
     * The total read timeout is the maximum amount of time the client will wait for the server to send the entire response body.
     * After this amount of time, if the entire body has not been received yet, the client will abort the request.
     * <br>
     * Null means use default timeout from the api client.
     */
    Duration getResponseBodyTotalTimeout();

    /**
     * Set all additional arbitrary query parameters that are to be added to the request.
     * These are added after all other query parameters are already added.
     * Call with null or empty map to remove all additional query parameters.
     */
    void setAdditionalQueryParameter(Map<String, List<String>> additionalQueryParameter);

    /**
     * Returns all additional arbitrary query parameters that are to be added to the request.
     * May return null or empty map if there are no additional query parameters.
     */
    Map<String, List<String>> getAdditionalQueryParameter();

    /**
     * Returns all additional arbitrary header parameters that are to be added to the request.
     * These headers are added in addition to the headers that are already present in the request.
     * Call with null or empty map to remove all additional header parameters.
     */
    void setAdditionalHeaderParameter(Map<String, List<String>> additionalHeaderParameter);

    /**
     * Returns all additional arbitrary header parameters that are to be added to the request.
     * May return null or empty map if there are no additional header parameters.
     */
    Map<String, List<String>> getAdditionalHeaderParameter();


    /**
     * Returns the request body if the request has one.
     */
    default Object getRequestBody() {
        return null;
    }
}
