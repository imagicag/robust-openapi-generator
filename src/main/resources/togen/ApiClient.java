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
import java.lang.reflect.Type;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

public abstract class ApiClient {

    private static volatile Executor defaultExecutor = null;

    protected Executor getDefaultExecutor() {
        if (defaultExecutor == null) {
            synchronized (ApiClient.class) {
                if (defaultExecutor == null) {
                    defaultExecutor = Executors.newCachedThreadPool();
                }
            }
        }

        return defaultExecutor;
    }

    protected Executor executor;
    protected String baseUrl;
    protected HttpClient client;
    protected Duration requestTimeout;
    protected Duration responseBodyReadTimeout;
    protected Duration responseBodyTotalTimeout;

    public ApiClient(String baseUrl, HttpClient.Builder builder) {
        this.setBaseUrl(baseUrl);
        this.setClient(builder.build());
    }

    public ApiClient(String baseUrl, HttpClient client) {
        this.setBaseUrl(baseUrl);
        this.setClient(client);
    }

    public Executor getExecutor() {
        return executor;
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

    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = Objects.requireNonNull(client);
        this.executor = client.executor().orElseGet(this::getDefaultExecutor);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        this.baseUrl = baseUrl;
    }

    protected HttpRequest.Builder newRequestBuilder(RequestContext metadata) throws ApiException {
        return HttpRequest.newBuilder();
    }

    protected void customizeRequestContext(RequestContext context) throws ApiException, IOException {
        //Subclass may customize the request here
    }

    protected void customizeRequest(RequestContext context, HttpRequest.Builder builder) throws ApiException, IOException {
        //Subclass may customize the Builder here
    }

    protected <T> Optional<T> customizeResponse(RequestContext context, Class<T> desiredType, HttpResponse<InputStream> response) throws ApiException, IOException {
        // Subclass may perform custom handling for the response here
        // Null means no special handling needed and the request processing proceeds as usual. (DEFAULT)
        // returning an Optional.isEmpty() will cause the underlying method to return null.
        // return Optional.isPresent() will cause the underlying method to return the value of the optional skipping response parsing.
        //noinspection OptionalAssignedToNull
        return null;
    }

    //Called to parse every response body
    protected abstract <T> T deserializeJsonData(RequestContext context, Type desiredType, int statusCode, HttpHeaders headers, InputStream stream) throws ApiException, IOException;

    //Called to serialize every json request body
    protected abstract HttpRequest.BodyPublisher serializeJsonData(RequestContext context, Object requestBody) throws ApiException, IOException;

    //Called to turn a BinaryPayload into a HttpRequest.BodyPublisher
    protected HttpRequest.BodyPublisher processBinaryDataRequestBody(RequestContext context, String contentType, BinaryPayload requestBody) throws ApiException, IOException {
        if (requestBody == null) {
            return null;
        }

        InputStream is = requestBody.read();

        if ("*/*".equals(contentType)) {
            context.setContentType("application/octet-stream");
            if (context.getRequestTimeout() == null) {
                return HttpRequest.BodyPublishers.ofInputStream(() -> is);
            }
            return new TimeoutBodyPublisher(getExecutor(), is, context.getRequestTimeout(), Duration.ofNanos(Long.MAX_VALUE), context);
        }

        context.setContentType(contentType);
        if (context.getRequestTimeout() == null) {
            return HttpRequest.BodyPublishers.ofInputStream(() -> is);
        }
        return new TimeoutBodyPublisher(getExecutor(), is, context.getRequestTimeout(), Duration.ofNanos(Long.MAX_VALUE), context);
    }

    protected HttpRequest.BodyPublisher processTextRequestBody(RequestContext context, String requestBody) throws ApiException, IOException {
        if (requestBody == null) {
            return null;
        }

        return HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8);
    }


    protected void performPostRequestCleanups(RequestMetadata metadata) {
        //Subclass may perform cleanup actions here, this method is guaranteed to be called in a finally block of each request regardless of its outcome.
    }

    /**
     * This method is called to glimpse some information to feed into an exception.
     * It is rather common that servers return error information in unexpected cases.
     * This method is rather conservative by default and will not read more than 64k of unexpected data.
     * The data is also read into memory by default and the connection is then gracefully closed.
     *
     * If you wish to hand down the InputStream into the exception (then you have to ensure the exception is cought and InputStream body is closed externally)
     * then simply overwrite this method with "return response;" the returned object will be available when calling {@link ApiException#getBody()}.
     */
    protected Object processResponseForException(RequestContext context, HttpResponse<InputStream> response) throws ApiException, IOException {
        byte[] data;
        try(InputStream is = response.body()) {
            data = is.readNBytes(0x1_00_00); //This reads up to 64k from the stream
        } catch (Exception e) {
            //We don't care about this error, as this is a secondary error.
            data = new byte[0];
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase();
        if (contentType.startsWith("text/") || contentType.equals("application/json")) {
            //Probably an error message!
            return new String(data, StandardCharsets.UTF_8);
        }

        return data;
    }

    protected BinaryPayload processResponseForBinaryPayload(RequestContext context, HttpResponse<InputStream> response) throws ApiException, IOException {
        //Try to figure out the Content-Length header if available/possible
        long len = -1;
        try {
            len = response.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);
        } catch (NumberFormatException nfe) {
            //IGNORED
        }

        if (len < 0) {
            InputStream is = response.body();

            //Check if it is less than 64k, if so, then we will only read it into memory.
            byte[] blob = new byte[0x1_00_00];
            int cnt = is.readNBytes(blob, 0, blob.length);
            if (cnt < 0x1_00_00) {
                //It is less than 64k, so we can just return the byte array.
                is.close();
                return new ByteArrayBinaryPayload(blob, 0, cnt);
            }

            //Otherwise we will pass on the input stream to the caller.
            return new PrefixedInputStreamBinaryPayload(blob, is);
        }

        if (len == 0) {
            //There is no response body.
            response.body().close();
            return new ByteArrayBinaryPayload(new byte[0]);
        }

        if (len > 0x1_00_00) {
            //There no point in attempting to buffer it in a byte array, it is too big.
            return new InputStreamBinaryPayloadWithContentLength(response.body(), len);
        }

        //We know the size and its small enough to buffer it in a byte array.
        byte[] blob = new byte[(int) len];
        try(InputStream is = response.body()) {
            int cnt = is.readNBytes(blob, 0, (int) len);
            return new ByteArrayBinaryPayload(blob, 0, cnt);
        }
    }

    protected HttpResponse<InputStream> sendRequest(RequestContext context, HttpRequest request) throws ApiException, IOException, InterruptedException {
        if (context.getRequestTimeout() == null || request.bodyPublisher().isEmpty()) {
            return client.send(request, (responseInfo) -> new HttpResponseInputStream(context.getResponseBodyReadTimeout(), context.getResponseBodyTotalTimeout()));
        }

        HttpRequest.BodyPublisher publisher = request.bodyPublisher().get();
        if (!(publisher instanceof TimeoutBodyPublisher)) {
            return client.send(request, (responseInfo) -> new HttpResponseInputStream(context.getResponseBodyReadTimeout(), context.getResponseBodyTotalTimeout()));
        }

        CompletableFuture<HttpResponse<InputStream>> res = client.sendAsync(request, (responseInfo) -> new HttpResponseInputStream(context.getResponseBodyReadTimeout(), context.getResponseBodyTotalTimeout()));
        context.setCancelFuture(res);

        CompletableFuture<?> joinedFuture = CompletableFuture.anyOf(context.requestBodyCompletedFuture(), res);

        try {
            joinedFuture.get();
        } catch (InterruptedException e) {
            res.cancel(true);
            throw e;
        } catch (Exception e) {
            //DC
        }

        try {
            return res.get(context.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            res.cancel(true);
            throw new HttpTimeoutException("Request timed out");
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof CancellationException) {
                Throwable cancelError = context.getCancelError();
                if (cancelError != null) {
                    cause = cancelError;
                }
            }

            if (cause instanceof IOException) {
                throw (IOException) cause;
            }

            if (cause != null) {
                throw new IOException(cause);
            }

            throw new IOException(ee);
        } catch (CancellationException ce) {
            Throwable cancelError = context.getCancelError();
            if (cancelError == null) {
                cancelError = ce;
            }

            if (cancelError instanceof IOException) {
                throw (IOException) cancelError;
            }

            throw new IOException(cancelError);
        }
    }

    protected RequestContext newRequestContext(RequestMetadata metadata, RequestParameters rawParam) throws ApiException {
        return new RequestContext(metadata, rawParam, getExecutor());
    }

    /**
     * This method is called to validate the request parameters.
     *
     * By default, no validation is done. This function is intended to be overwritten by subclasses to, for example,
     * enable validation using a JSR380 validator.
     *
     * To indicate an error, throwing an ApiException is recommended!
     */
    protected void validateRequest(RequestContext context, RequestParameters param) throws ApiException {
        //DEFAULT IS EMPTY
    }

    /**
     * This method is called to validate the response parameters.
     * If an error is found in the request parameter, then an Optional.of(error message) should be returned.
     * Otherwise, Optional.empty() should be returned.
     * By default, no validation is done. This function is intended to be overwritten by subclasses to, for example,
     * enable validation using a JSR380 validator.
     */
    protected void validateResponse(RequestContext context, Response param) throws ApiException {
        //DEFAULT IS EMPTY
    }
}
