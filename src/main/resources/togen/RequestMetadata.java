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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Contains most information from the openapi schema for a given endpoint.
 * Instances of this class are immutable.
 *
 * The code generator generates the invocations of the Constructor as static fields in the API.
 */
public class RequestMetadata {

    private final String operationId;
    private final String method;
    private final String path;
    private final Class<?> returnType;
    public static final String NO_REQUEST_BODY_CONTENT_TYPE_MAP_KEY = "no-request-body";
    private final Map<String, String> methodNamesForRequestBodyContentType;
    private final Set<Class<?>> parameterClasses;
    private final Set<String> headerParameters;
    private final Set<String> queryParameters;
    private final Set<String> pathParameters;
    private final Set<String> patternPathParameters;


    public RequestMetadata(String operationId, String method, String path, Class<?> returnType, Map<String, String> methodNamesForRequestBodyContentType, Set<Class<?>> parameterClasses, Set<String> headerParameters, Set<String> queryParameters, Set<String> pathParameters, Set<String> patternPathParameters) {
        this.operationId = operationId;
        this.method = method;
        this.path = path;
        this.returnType = returnType;
        this.parameterClasses = Collections.unmodifiableSet(parameterClasses);
        this.headerParameters = Collections.unmodifiableSet(headerParameters);
        this.queryParameters = Collections.unmodifiableSet(queryParameters);
        this.pathParameters = Collections.unmodifiableSet(pathParameters);
        this.patternPathParameters = Collections.unmodifiableSet(patternPathParameters);

        this.methodNamesForRequestBodyContentType = Collections.unmodifiableMap(methodNamesForRequestBodyContentType);
    }

    public Set<String> getPatternPathParameters() {
        return patternPathParameters;
    }

    public Map<String, String> getMethodNamesForRequestBodyContentType() {
        return methodNamesForRequestBodyContentType;
    }


    public Set<Class<?>> getParameterClasses() {
        return parameterClasses;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Set<String> getHeaderParameters() {
        return headerParameters;
    }

    public Set<String> getQueryParameters() {
        return queryParameters;
    }

    public Set<String> getPathParameters() {
        return pathParameters;
    }
}
