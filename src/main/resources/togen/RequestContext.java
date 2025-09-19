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

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Context for request creation.
 * This class acts like a higher level abstraction over the {@link HttpRequest.Builder} class,
 * that additionally contains various information/states from the openapi schema.
 */
public class RequestContext {
    private final RequestParameters rawParam;
    private final RequestMetadata metadata;
    private HttpRequest.BodyPublisher requestBody;
    private Map<String, String> pathParameters;
    private Map<String, List<String>> queryParameters;
    private Map<String, String> headerParameters;
    private String baseUrl;
    private String method;
    private String contentType;
    private String path;
    private Duration requestTimeout;
    private Duration responseBodyReadTimeout;
    private Duration responseBodyTotalTimeout;
    private final Executor executor;
    private final CompletableFuture<Future<?>> cancelFuture = new CompletableFuture<>();
    private final CompletableFuture<Throwable> errorFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> requestBodyCompletedFuture = new CompletableFuture<>();



    /**
     * Constructor.
     *
     * @param metadata refers to the metadata of the operation method,
     *                 the metadata object itself is usually generated and contains all information from the openapi schema.
     *
     * @param rawParam refers to the ParameterObject passed to the operation method as is.
     */
    public RequestContext(RequestMetadata metadata, RequestParameters rawParam, Executor executor) {
        this.metadata = metadata;
        this.method = metadata.getMethod();
        this.path = metadata.getPath();
        this.rawParam = rawParam;
        this.executor = executor;
    }

    public void onRequestBodyCompleted() {
        this.requestBodyCompletedFuture.complete(null);
    }

    public CompletableFuture<Void> requestBodyCompletedFuture() {
        return this.requestBodyCompletedFuture;
    }

    public void cancel(Throwable err) {
        errorFuture.complete(err);
        cancelFuture.thenAcceptAsync(a -> a.cancel(true), executor);
    }

    public void setCancelFuture(Future<?> future) {
        cancelFuture.complete(future);
    }

    public Throwable getCancelError() {
        return errorFuture.getNow(null);
    }

    public RequestParameters getRawParam() {
        return rawParam;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Adds a query parameter to the request.
     * Both the key and value are percent encoded before being sent out if necessary.
     */
    public RequestContext addQueryParam(String key, String value) {
        if (queryParameters == null) {
            queryParameters = new LinkedHashMap<>();
        }
        queryParameters.computeIfAbsent(key, k -> new ArrayList<>(1)).add(value);
        return this;
    }

    /**
     * Adds a path parameter to the request.
     * The value is percent encoded before being sent out if necessary.
     * If the key does not exist in the path, then the value is ignored.
     *
     * The value is inserted into the url by using the String.replace method.
     * The key is prefixed and suffixed with curly braces.
     *
     * IMPORTANT:
     * Do not use untrusted values for the key, as it is assumed that the key is valid.
     * A malicious key may be able to transform the url in unexpected ways.
     */
    public RequestContext addPathParam(String key, String value) {
        if (pathParameters == null) {
            pathParameters = new LinkedHashMap<>();
        }
        pathParameters.put(key, value);
        return this;
    }

    /**
     * Adds a header parameter to the request.
     *
     * The values are passed through to HttpRequest.Builder.setHeader later.
     * If invalid values are passed, then an exception is thrown there later.
     */
    public RequestContext addHeaderParam(String key, String value) {
        if (headerParameters == null) {
            headerParameters = new LinkedHashMap<>();
        }
        headerParameters.put(key, value);
        return this;
    }

    public String getUri() {
        String uri = baseUrl + getPath();
        if (pathParameters != null) {
            for (Map.Entry<String, String> e : pathParameters.entrySet()) {
                StringBuilder sb = new StringBuilder();
                appendPercentEncoded(sb, e.getValue(), this.metadata.getPatternPathParameters().contains(e.getKey()));
                uri = uri.replace("{"+e.getKey()+"}", sb.toString());
            }
        }

        return uri + getQuery();
    }

    /**
     * Transfers all headers from this RequestContext into the given HttpRequest.Builder.
     */
    public RequestContext setHeadersIntoRequest(HttpRequest.Builder builder) {
        if (headerParameters != null) {
            for (Map.Entry<String, String> e : headerParameters.entrySet()) {
                builder.setHeader(e.getKey(), e.getValue());
            }
        }



        if (rawParam.getAdditionalHeaderParameter() != null) {
            for (Map.Entry<String, List<String>> e : rawParam.getAdditionalHeaderParameter().entrySet()) {
                for (String v : e.getValue()) {
                    builder.header(e.getKey(), v);
                }
            }
        }

        if (contentType != null) {
            builder.setHeader("Content-Type", contentType);
        }

        return this;
    }

    /**
     * Returns the query string as it would be used in the url.
     * All values in the returned string are percent encoded.
     *
     * If no query parameters are present, then an empty string is returned.
     */
    public String getQuery() {
        Map<String, List<String>> additionalQueryParameter = rawParam.getAdditionalQueryParameter();

        StringBuilder sb = new StringBuilder();
        if (queryParameters != null) {
            for (Map.Entry<String, List<String>> entry : queryParameters.entrySet()) {
                for (String value : entry.getValue()) {
                    if (sb.length() == 0) {
                        sb.append('?');
                        appendPercentEncoded(sb, entry.getKey(), false);
                        sb.append('=');
                        appendPercentEncoded(sb, value, false);
                        continue;
                    }

                    sb.append('&');
                    appendPercentEncoded(sb, entry.getKey(), false);
                    sb.append('=');
                    appendPercentEncoded(sb, value, false);
                }
            }
        }

        if (additionalQueryParameter != null) {
            for (Map.Entry<String, List<String>> entry : additionalQueryParameter.entrySet()) {
                for (String value : entry.getValue()) {
                    if (sb.length() == 0) {
                        sb.append('?');
                        appendPercentEncoded(sb, entry.getKey(), false);
                        sb.append('=');
                        appendPercentEncoded(sb, value, false);
                        continue;
                    }

                    sb.append('&');
                    appendPercentEncoded(sb, entry.getKey(), false);
                    sb.append('=');
                    appendPercentEncoded(sb, value, false);
                }
            }
        }

        return sb.toString();
    }

    public RequestMetadata getMetadata() {
        return metadata;
    }

    public HttpRequest.BodyPublisher getRequestBody() {
        return requestBody;
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }

    public Map<String, String> getHeaderParameters() {
        return headerParameters;
    }

    public void setRequestBody(HttpRequest.BodyPublisher requestBody) {
        this.requestBody = requestBody;
    }

    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }

    public void setQueryParameters(Map<String, List<String>> queryParameters) {
        this.queryParameters = queryParameters;
    }

    public void setHeaderParameters(Map<String, String> headerParameters) {
        this.headerParameters = headerParameters;
    }

    private static final boolean[] ALLOWED_CHARS = new boolean[0x100];
    static {
        for (int i = 'A'; i <= 'Z'; i++) {
            ALLOWED_CHARS[i] = true;
        }

        for (int i = 'a'; i <= 'z'; i++) {
            ALLOWED_CHARS[i] = true;
        }

        for (int i = '0'; i <= '9'; i++) {
            ALLOWED_CHARS[i] = true;
        }

        ALLOWED_CHARS['-'] = true;
        ALLOWED_CHARS['_'] = true;
        ALLOWED_CHARS['~'] = true;
        ALLOWED_CHARS['.'] = true;
    }
    private static final String[] HEX_TAB = new String[0x100];
    static {
        for (int i = 0; i < HEX_TAB.length; i++) {
            String s = Integer.toHexString(i);
            while (s.length() < 2) {
                s = "0" + s;
            }
            HEX_TAB[i] = "%" + s;
        }
    }


    /**
     * Poor mans percent encoding.
     * Probably does the job,
     * if not then open up a github issue, and subclass {@link RequestContext}
     * and override this method until we address the issue.
     */
    protected void appendPercentEncoded(StringBuilder builder, String value, boolean permitSlash) {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        for (byte b : data) {
            int code = b & 0xFF;
            if (ALLOWED_CHARS[code]) {
                builder.append((char) code);
                continue;
            }

            if (permitSlash && code == '/') {
                builder.append((char) code);
                continue;
            }

            builder.append(HEX_TAB[code]);
        }
    }

    /**
     * Applies the request context to the given HttpRequest.Builder.
     * This method is called in the generated operations code before the request is sent.
     */
    public void apply(HttpRequest.Builder builder) {
        builder.uri(URI.create(this.getUri()))
                .method(method, requestBody == null ? HttpRequest.BodyPublishers.noBody() : requestBody);
        setHeadersIntoRequest(builder);
        if (requestTimeout != null && !(this.requestBody instanceof TimeoutBodyPublisher)) {
            builder.timeout(requestTimeout);
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getResponseBodyReadTimeout() {
        return responseBodyReadTimeout;
    }

    public void setResponseBodyReadTimeout(Duration responseBodyReadTimeout) {
        this.responseBodyReadTimeout = responseBodyReadTimeout;
    }

    public Duration getResponseBodyTotalTimeout() {
        return responseBodyTotalTimeout;
    }

    public void setResponseBodyTotalTimeout(Duration responseBodyTotalTimeout) {
        this.responseBodyTotalTimeout = responseBodyTotalTimeout;
    }
}
