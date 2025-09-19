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
import java.util.*;

/**
 * Marker interface for responses.
 */
public interface Response {

    Object getRawBody();

    Enum<?> getVariant();

    int getStatusCode();

    Map<String, List<String>> getRawHeaders();

    @FunctionalInterface
    interface ResponseMapper<R extends Response, T, E extends Throwable> {
        T map(R response) throws E;
    }

    /**
     * Returns the content type of the response.
     */
    default Optional<String> getContentType() {
        return getRawHeaders().getOrDefault("Content-Type", Collections.emptyList()).stream().findFirst();
    }


    /**
     * Returns the content length of the response.
     */
    default OptionalLong getContentLength() {
        try {
            return getRawHeaders().getOrDefault("Content-Length", Collections.emptyList()).stream().mapToLong(Long::valueOf).findFirst();
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

}
