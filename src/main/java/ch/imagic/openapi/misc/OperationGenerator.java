// Copyright 2025 Imagic Bildverarbeitung AG CH-8152 Glattbrugg
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package ch.imagic.openapi.misc;

import ch.imagic.openapi.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class OperationGenerator {

    public static void generateApis(GenerationContext ctx) {
        Set<String> apiIfaces = prepareTagInterfaces(ctx);

        /// Prepare the primary api interface
        String primaryInterfaceName = ctx.getTagSuffix();
        if (primaryInterfaceName.isEmpty()) {
            primaryInterfaceName = "Api";
        }

        if (apiIfaces.contains(primaryInterfaceName)) {
            int cnt = 0;
            while (apiIfaces.contains(primaryInterfaceName + cnt)) {
                cnt++;
            }
            primaryInterfaceName = primaryInterfaceName + cnt;
        }

        String primaryInterface = ctx.qualifyTagInterfaceClass(primaryInterfaceName);

        ctx.push(primaryInterface, "package " + ctx.getTagPackage() + ";");
        ctx.push(primaryInterface, "");
        if (apiIfaces.isEmpty()) {
            ctx.push(primaryInterface, "public interface "+primaryInterfaceName+" extends java.lang.AutoCloseable {");
        } else {
            ctx.push(primaryInterface, "public interface "+primaryInterfaceName+" extends java.lang.AutoCloseable, " +apiIfaces.stream().map(ctx::qualifyTagInterfaceClass).collect(Collectors.joining(", ")) + " {");
        }
        ctx.addIndent(primaryInterface);
        ctx.push(primaryInterface, "");
        /// //////////////////////////////////


        String typesClass = prepareTypesClass(ctx);

        String className = ctx.qualifyImplClass("ApiImpl");
        prepareApiImpl(ctx, className, primaryInterface);
        Set<String> metaFields = new HashSet<>();
        Set<String> requestMethods = new HashSet<>();

        for (Map.Entry<String, Map<String, PathModel>> paths : ctx.getModel().getPaths().entrySet()) {
            String path = paths.getKey();
            for (Map.Entry<String, PathModel> pm : paths.getValue().entrySet()) {
                String method = pm.getKey();
                PathModel model = pm.getValue();

                String responseObjectClassName = ResponseModelGenerator.generateResponseModel(ctx, model);

                String operationId = model.getOperationId();
                List<String> headerParameters = new ArrayList<>();
                List<String> pathParameters = new ArrayList<>();
                List<String> queryParameters = new ArrayList<>();
                List<String> patternPathParameter = new ArrayList<>();
                if (model.getParameters() != null) {
                    for (ParameterModel parameter : model.getParameters()) {
                        ParameterModel prm = ctx.findParameter(parameter.get$ref());
                        switch (prm.getIn().toLowerCase()) {
                            case "header":
                                headerParameters.add(prm.getName());
                                break;
                            case "query":
                                queryParameters.add(prm.getName());
                                break;
                            case "path":
                                if (prm.getSchema() != null && ".*".equals(prm.getSchema().getPattern())) {
                                    //Special path parameters that are allowed to contain the "/" character without url encoding.
                                    patternPathParameter.add(prm.getName());
                                }
                                pathParameters.add(prm.getName());
                                break;
                            default:
                                //IGNORED
                        }
                    }
                }

                Map<String, String> methodsForOperation = new HashMap<>();
                String metaFieldName =  "REQUEST_METADATA_" + model.getOperationId().toUpperCase();
                if (!metaFields.add(metaFieldName)) {
                    int cnt = 0;
                    while (!metaFields.add(metaFieldName + cnt)) {
                        cnt++;
                    }
                    metaFieldName = metaFieldName + cnt;
                }



                if (model.getRequestBody() == null && (model.getParameters() == null || model.getParameters().length == 0)) {
                    // There is no request body or query parameter.
                    // There is nothing to pass for a parameter.
                    String requestParameterClass = RequestModelGenerator.generateRequestModel(ctx, model, path, null);

                    String methodNameToGenerate = model.getOperationId();
                    if (ctx.isExtensionOperation(model.getOperationId())) {
                        methodNameToGenerate += ctx.getExtensionOperationSuffix();
                    }
                    if (!requestMethods.add(methodNameToGenerate)) {
                        int cnt = 0;
                        while(!requestMethods.add(methodNameToGenerate + cnt)) {
                            cnt++;
                        }
                        methodNameToGenerate = methodNameToGenerate + cnt;
                    }
                    methodsForOperation.put("no-request-body", methodNameToGenerate);

                    generateOperationContent(ctx, className, primaryInterface, responseObjectClassName, methodNameToGenerate, requestParameterClass, metaFieldName, model, operationId, null);
                    generateMetadataStaticField(ctx, className, metaFieldName, operationId, method, path, responseObjectClassName, methodsForOperation, Set.of(), headerParameters, queryParameters, pathParameters, patternPathParameter);
                    continue;
                }

                if (model.getRequestBody() == null) {
                    //We have only stuff like query parameters, but no body
                    String requestParameterClass = RequestModelGenerator.generateRequestModel(ctx, model, path, null);

                    String methodNameToGenerate = model.getOperationId();
                    if (ctx.isExtensionOperation(model.getOperationId())) {
                        methodNameToGenerate += ctx.getExtensionOperationSuffix();
                    }

                    if (!requestMethods.add(methodNameToGenerate)) {
                        int cnt = 0;
                        while(!requestMethods.add(methodNameToGenerate + cnt)) {
                            cnt++;
                        }
                        methodNameToGenerate = methodNameToGenerate + cnt;
                    }

                    methodsForOperation.put("no-request-body", methodNameToGenerate);

                    generateOperationContent(ctx, className, primaryInterface, responseObjectClassName, methodNameToGenerate, requestParameterClass, metaFieldName, model, operationId, null);
                    generateMetadataStaticField(ctx, className, metaFieldName, operationId, method, path, responseObjectClassName, methodsForOperation, Set.of(requestParameterClass), headerParameters, queryParameters, pathParameters, patternPathParameter);
                    continue;
                }

                //We have request bodies or optionally query parameters etc.
                Set<String> requestParameterClasses = new HashSet<>();
                RequestBodyModel requestBody = ctx.findRequestBody(model.getRequestBody().get$ref());
                for (String ctype : requestBody.getContent().keySet()) {
                    String requestParameterClass = RequestModelGenerator.generateRequestModel(ctx, model, path, ctype);
                    requestParameterClasses.add(requestParameterClass);
                    String mctype = Util.mangleContentType(Util.capitalize(ctype));
                    String methodNameToGenerate = model.getOperationId() + mctype;
                    if (ctx.isExtensionOperation(model.getOperationId())) {
                        methodNameToGenerate += ctx.getExtensionOperationSuffix();
                    }

                    if (!requestMethods.add(methodNameToGenerate)) {
                        int cnt = 0;
                        while(!requestMethods.add(methodNameToGenerate + cnt)) {
                            cnt++;
                        }
                        methodNameToGenerate = methodNameToGenerate + cnt;
                    }

                    methodsForOperation.put(ctype, methodNameToGenerate);

                    generateOperationContent(ctx, className, primaryInterface, responseObjectClassName, methodNameToGenerate, requestParameterClass, metaFieldName, model, operationId, ctype);
                }

                generateMetadataStaticField(ctx, className, metaFieldName, operationId, method, path, responseObjectClassName, methodsForOperation, requestParameterClasses, headerParameters, queryParameters, pathParameters, patternPathParameter);
            }
        }

        finishApiImpl(ctx, className, metaFields);

        finishTagInterfaces(ctx, apiIfaces, primaryInterface);

        //Finish the types class
        ctx.subIndent(typesClass);
        ctx.push(typesClass, "}");
    }

    private static void prepareApiImpl(GenerationContext ctx, String className, String primaryInterface) {
        ctx.push(className, "package " + ctx.getImplPackage() + ";");
        ctx.push(className, "");
        ctx.push(className, "public abstract class ApiImpl extends "+ ctx.qualifyCommonImplClass("ApiClient")+" implements " + primaryInterface + "{");
        ctx.addIndent(className);
        ctx.push(className, "");
        ctx.push(className, "public ApiImpl(String baseUrl, java.net.http.HttpClient.Builder builder) {");
        ctx.addIndent(className);
        ctx.push(className, "super(baseUrl, builder);");
        ctx.subIndent(className);
        ctx.push(className, "}");
        ctx.push(className, "");
        ctx.push(className, "public ApiImpl(String baseUrl, java.net.http.HttpClient client) {");
        ctx.addIndent(className);
        ctx.push(className, "super(baseUrl, client);");
        ctx.subIndent(className);
        ctx.push(className, "}");
        ctx.push(className, "");
    }

    private static void finishApiImpl(GenerationContext ctx, String apiImplClassName, Set<String> metaFields) {
        ctx.push(apiImplClassName, "protected static final java.util.List<"+ ctx.qualifyCommonImplClass("RequestMetadata") +"> ALL_REQUEST_METADATA = java.util.Arrays.asList(" + metaFields.stream().sorted().collect(Collectors.joining(", ")) + ");");

        String generatedModelClasses = ctx.getGenerated().keySet().stream().sorted().filter(a -> a.startsWith(ctx.getModelPackage())).map(a -> a + ".class").collect(Collectors.joining(", "));
        String generatedResponseClasses = ctx.getGenerated().keySet().stream().sorted().filter(a -> a.startsWith(ctx.getResponsePackage())).map(a -> a + ".class").collect(Collectors.joining(", "));
        String generatedRequestClasses = ctx.getGenerated().keySet().stream().sorted().filter(a -> a.startsWith(ctx.getReqParamPackage())).map(a -> a + ".class").collect(Collectors.joining(", "));

        ctx.push(apiImplClassName, "protected static final java.util.List<java.lang.Class<?>> ALL_MODEL_CLASSES = java.util.Arrays.asList(" + generatedModelClasses + ");");
        ctx.push(apiImplClassName, "protected static final java.util.List<java.lang.Class<? extends " + ctx.qualifyCommonApiClass("Response") +">> ALL_RESPONSE_CLASSES = java.util.Arrays.asList(" + generatedResponseClasses + ");");
        ctx.push(apiImplClassName, "protected static final java.util.List<java.lang.Class<? extends " + ctx.qualifyCommonApiClass("RequestParameters") +">> ALL_REQUEST_CLASSES = java.util.Arrays.asList(" + generatedRequestClasses + ");");

        ctx.subIndent(apiImplClassName);
        ctx.push(apiImplClassName, "}");
    }

    private static String prepareTypesClass(GenerationContext ctx) {
        String typesClass = ctx.qualifyImplClass("Types");
        ctx.push(typesClass, "package " + ctx.getImplPackage() + ";");
        ctx.push(typesClass, "");
        ctx.push(typesClass, "/**");
        ctx.push(typesClass, " * This class contains generic type information needed for json parsing of response bodies.");
        ctx.push(typesClass, " * All response bodies which need to be parsed as generic type such as List&ltSomeType&gt have an entry here.");
        ctx.push(typesClass, " */");
        ctx.push(typesClass, "public class Types {");
        ctx.addIndent(typesClass);
        ctx.push(typesClass, "private static abstract class TypeInfo<T> {");
        ctx.addIndent(typesClass);
        ctx.push(typesClass, "public java.lang.reflect.Type getType() {");
        ctx.addIndent(typesClass);
        ctx.push(typesClass, "return ((java.lang.reflect.ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];");
        ctx.subIndent(typesClass);
        ctx.push(typesClass, "}");
        ctx.subIndent(typesClass);
        ctx.push(typesClass, "}");
        return typesClass;
    }

    private static void finishTagInterfaces(GenerationContext ctx, Set<String> apiIfaces, String primaryInterface) {
        for (String iface: apiIfaces) {
            String qual = ctx.qualifyTagInterfaceClass(iface);
            ctx.subIndent(qual);
            ctx.push(qual, "}");
        }

        ctx.push(primaryInterface, "");
        ctx.push(primaryInterface, "/**");
        ctx.push(primaryInterface, " * On Java 21 and newer this function should be implemented to call HttpClient.close()/HttpClient.shutdownNow() etc.<br>");
        ctx.push(primaryInterface, " * <br>");
        ctx.push(primaryInterface, " * With older Java versions this method should be implemented to a noop,<br>");
        ctx.push(primaryInterface, " * or set the HttpClient to null and possibly call System.gc() if necesarry.<br>");
        ctx.push(primaryInterface, " * <br>");
        ctx.push(primaryInterface, " * To help you decide: It depends entirely how many instances of HttpClient your application creates and if natural garbage collection is fast enough.<br>");
        ctx.push(primaryInterface, " * An example where where calling System.gc() may be necesarry is if you are running in environments where the number of user mode threads is limited to a fixed very low number (some Cloud Service Providers do this),<br>");
        ctx.push(primaryInterface, " * and your application requires communication with a lot of different servers and therefore multiple instances of HttpClient<br>");
        ctx.push(primaryInterface, " */");
        ctx.push(primaryInterface, "@Override");
        ctx.push(primaryInterface, "void close() throws RuntimeException;");


        ctx.subIndent(primaryInterface);
        ctx.push(primaryInterface, "}");
    }

    private static Set<String> prepareTagInterfaces(GenerationContext ctx) {
        Set<String> apiIfaces = new HashSet<>();
        for (Map.Entry<String, Map<String, PathModel>> paths : ctx.getModel().getPaths().entrySet()) {
            for (Map.Entry<String, PathModel> pm : paths.getValue().entrySet()) {
                PathModel model = pm.getValue();
                if (model.getTags() == null) {
                    continue;
                }

                for (String tag : model.getTags()) {
                    String name = Util.capitalize(Util.mangleName(tag)) + ctx.getTagSuffix();
                    if (apiIfaces.add(name)) {
                        String className = ctx.qualifyTagInterfaceClass(name);
                        ctx.push(className, "package " + ctx.getTagPackage() + ";");
                        ctx.push(className, "");

                        if (ctx.getModel().getTags() != null) {
                            for (TagModel t : ctx.getModel().getTags()) {
                                if (tag.equals(t.getName())) {
                                    Util.pushJavaDoc(ctx, className, t.getDescription());
                                    break;
                                }
                            }
                        }

                        ctx.push(className, "public interface " + name + " {");
                        ctx.addIndent(className);
                    }

                }
            }
        }

        return apiIfaces;
    }

    private static void generateMetadataStaticField(GenerationContext ctx, String apiClassName, String metaFieldName, String operationId, String method, String path, String responseObjectClassName, Map<String, String> methodNamesForRequestBodyContentType, Set<String> requestParameterClasses, List<String> headerParameters, List<String> queryParameters, List<String> pathParameters, List<String> patternPathParameters) {



        ctx.push(apiClassName, "");
        ctx.push(apiClassName, "/**");
        ctx.push(apiClassName, " * Schema metadata of the " + operationId + " operation.");
        ctx.push(apiClassName, " */");
        ctx.push(apiClassName, "protected static " + ctx.qualifyCommonImplClass("RequestMetadata") + " " + metaFieldName +
                " = new " + ctx.qualifyCommonImplClass("RequestMetadata") + "(");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "\"" + Util.escapeForSourceCode(operationId) + "\", \"" + Util.escapeForSourceCode(method.toUpperCase()) + "\", \"" + Util.escapeForSourceCode(path) + "\", " + responseObjectClassName + ".class,",
                "java.util.Map.ofEntries(" + methodNamesForRequestBodyContentType.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(a -> "java.util.Map.entry(\""+Util.escapeForSourceCode(a.getKey()) + "\", \""+Util.escapeForSourceCode(a.getValue())+"\")").collect(Collectors.joining(", ")) +"),",
                "new java.util.LinkedHashSet<>(java.util.Arrays.asList(" + requestParameterClasses.stream().sorted().map(a -> a + ".class").collect(Collectors.joining(", ")) +")),",
                "new java.util.LinkedHashSet<>(java.util.Arrays.asList(" + headerParameters.stream().map(a -> "\"" + Util.escapeForSourceCode(a) + "\"").collect(Collectors.joining(", ")) +")),",
                "new java.util.LinkedHashSet<>(java.util.Arrays.asList(" + queryParameters.stream().map(a -> "\"" + Util.escapeForSourceCode(a) + "\"").collect(Collectors.joining(", ")) +")),",
                "new java.util.LinkedHashSet<>(java.util.Arrays.asList(" + pathParameters.stream().map(a -> "\"" + Util.escapeForSourceCode(a) + "\"").collect(Collectors.joining(", ")) +")),",
                "new java.util.LinkedHashSet<>(java.util.Arrays.asList(" + patternPathParameters.stream().map(a -> "\"" + Util.escapeForSourceCode(a) + "\"").collect(Collectors.joining(", ")) +"))");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, ");");
        ctx.push(apiClassName, "");
    }

    private static void generateOperationContent(GenerationContext ctx, String apiClassName, String primaryInterfaceClassName, String responseClassName, String methodNameToGenerate, String requestParameterClass, String metaFieldName, PathModel model, String operationId, String contentType) {
        Map<String, ResponseModel> responses = model.getResponses();
        if (responses == null) {
            responses = new HashMap<>();
        }
        responses = new HashMap<>(responses);

        for (Map.Entry<String, ResponseModel> res : responses.entrySet()) {
            res.setValue(ctx.findResponse(res.getValue().get$ref()));
        }

        if (model.getTags() != null && !model.getTags().isEmpty()) {
            for (String tag : model.getTags()) {
                String interfaceName = Util.capitalize(Util.mangleName(tag)) + ctx.getTagSuffix();
                String iface = ctx.qualifyTagInterfaceClass(interfaceName);
                ctx.push(iface, "");
                Util.pushJavaDoc(ctx, interfaceName, model.getDescription());
                ctx.push(iface, responseClassName + " " + methodNameToGenerate + "(" + requestParameterClass + " param) throws " + ctx.qualifyCommonApiClass("ApiException") + ctx.getOperationInterfaceSuffix()+ ";");
            }
        } else {
            ctx.push(primaryInterfaceClassName, "");
            Util.pushJavaDoc(ctx, primaryInterfaceClassName, model.getDescription());
            ctx.push(primaryInterfaceClassName, responseClassName + " " + methodNameToGenerate + "(" + requestParameterClass + " param) throws " + ctx.qualifyCommonApiClass("ApiException") + ctx.getOperationInterfaceSuffix()+ ";");
        }

        ctx.push(apiClassName, "/**");
        ctx.push(apiClassName, " * Implementation of the " + operationId + " operation.");
        ctx.push(apiClassName, " */");
        ctx.push(apiClassName, "@Override");
        ctx.push(apiClassName, "public " + responseClassName + " " + methodNameToGenerate + "(" + requestParameterClass + " param) throws " + ctx.qualifyCommonApiClass("ApiException") + "{");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, ctx.qualifyCommonImplClass("RequestContext") + " context = newRequestContext(" + metaFieldName + ", java.util.Objects.requireNonNull(param, \"param must not be null\"));");

        ctx.push(apiClassName, "java.net.http.HttpResponse<java.io.InputStream> response = null;");

        if (contentType != null && !contentType.equals("application/json") && !contentType.equals("text/plain")) {
            ctx.push(apiClassName, "try ("+ctx.qualifyCommonApiClass("BinaryPayload")+" requestBody = param.getRequestBody()) {");
        } else {
            ctx.push(apiClassName, "try {");
        }

        ctx.addIndent(apiClassName);
        if (requestParameterClass != null) {
            ctx.push(apiClassName, "validateRequest(context, param);");
        }

        ctx.push(apiClassName, "context.setRequestTimeout(param.getRequestTimeout() == null ? this.getRequestTimeout() : param.getRequestTimeout());");
        ctx.push(apiClassName, "context.setResponseBodyReadTimeout(param.getResponseBodyReadTimeout() == null ? this.getResponseBodyReadTimeout() : param.getResponseBodyReadTimeout());");
        ctx.push(apiClassName, "context.setResponseBodyTotalTimeout(param.getResponseBodyTotalTimeout() == null ? this.getResponseBodyTotalTimeout() : param.getResponseBodyTotalTimeout());");

        ctx.push(apiClassName, "context.setBaseUrl(this.getBaseUrl());");
        if ("application/json".equals(contentType)) {
            ctx.push(apiClassName, "context.setContentType(\"application/json\");");
            ctx.push(apiClassName, "context.setRequestBody(serializeJsonData(context, param.getRequestBody()));");
        } else if ("text/plain".equals(contentType)) {
            ctx.push(apiClassName, "context.setContentType(\"text/plain\");");
            ctx.push(apiClassName, "context.setRequestBody(processTextRequestBody(context, param.getRequestBody()));");
        } else if (contentType != null) {
            ctx.push(apiClassName, "context.setRequestBody(processBinaryDataRequestBody(context, \""+ Util.escapeForSourceCode(contentType)+"\", requestBody));");
        }

        if (requestParameterClass != null) {
            generateOperationParameterTransferToRequestContext(ctx, model, requestParameterClass, apiClassName);
        }
        ctx.push(apiClassName, "customizeRequestContext(context);");
        ctx.push(apiClassName, "java.net.http.HttpRequest.Builder builder = this.newRequestBuilder(context);");
        ctx.push(apiClassName, "context.apply(builder);");
        ctx.push(apiClassName, "customizeRequest(context, builder);");
        ctx.push(apiClassName, "java.net.http.HttpRequest request = builder.build();");
        ctx.push(apiClassName, "response = sendRequest(context, request);");
        ctx.push(apiClassName, "java.util.Optional<"+ responseClassName +"> customizedResponse = customizeResponse(context, "+ responseClassName +".class, response);");
        ctx.push(apiClassName, "if (customizedResponse != null) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "if (customizedResponse.isEmpty()) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "return null;");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.push(apiClassName, "return customizedResponse.get();");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.push(apiClassName, "");
        ctx.push(apiClassName, "int statusCode = response.statusCode();");
        ctx.push(apiClassName, "java.net.http.HttpHeaders headers =  response.headers();");
        ctx.push(apiClassName, "String contentType = headers.firstValue(\"Content-Type\").orElse(\"no-content-type\").toLowerCase();");
        ctx.push(apiClassName, "int contentTypeEnd = contentType.indexOf(';');");
        ctx.push(apiClassName, "if (contentTypeEnd >= 0) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "contentType = contentType.substring(0, contentTypeEnd);");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");

        ctx.push(apiClassName, "switch (statusCode) {");
        ctx.addIndent(apiClassName);

        for (Map.Entry<String, ResponseModel> res : responses.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            if (res.getKey().equals("default")) {
                continue;
            }
            if (!Util.isStatusCode(res.getKey())) {
                System.out.println("WARNING operation " + operationId + " has a status code value of '" + res.getKey() + "' this is not a valid status code and will be ignored.");
                continue;
            }

            ctx.push(apiClassName,"case " + res.getKey()+  ": {");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName,"switch(contentType) {");
            ctx.addIndent(apiClassName);
            generateResponseSwitchBlockCases(ctx, apiClassName, responseClassName, res.getValue(), "S" + res.getKey());
            if (res.getValue().getContent() == null || !res.getValue().getContent().containsKey("*/*")) {
                //Default case already handled by */*
                ctx.push(apiClassName,"default: {");
                ctx.addIndent(apiClassName);
                ctx.push(apiClassName, "throw new "+ctx.qualifyCommonApiClass("ApiException")+"(\""+operationId+"\", statusCode, headers.map(), processResponseForException(context, response), \"Unexpected content type for status code \" + statusCode + \" \" + contentType);");
                ctx.subIndent(apiClassName);
                ctx.push(apiClassName,"}");
            }

            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");

            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
        }

        ctx.push(apiClassName,"default: {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "switch (contentType) {");
        ctx.addIndent(apiClassName);

        ResponseModel defModel = responses.get("default");
        if (defModel != null) {
            generateResponseSwitchBlockCases(ctx, apiClassName, responseClassName, defModel, "DEFAULT");
        }

        if (defModel == null || defModel.getContent() == null || !defModel.getContent().containsKey("*/*")) {
            //Default case already handled by */*
            ctx.push(apiClassName,"default: {");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, "throw new "+ctx.qualifyCommonApiClass("ApiException")+"(\""+operationId+"\", statusCode, headers.map(), processResponseForException(context, response), \"Unexpected status code and content type \" + statusCode + \" \" + contentType);");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName,"}");
        }

        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");

        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");

        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");

        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "} catch (Throwable throwable) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "if (response != null) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "try {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "response.body().close();");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "} catch (Throwable t) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "//Ignored");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.push(apiClassName, "if (throwable instanceof "+ ctx.qualifyCommonApiClass("ApiException")+ ") {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "throw ("+ ctx.qualifyCommonApiClass("ApiException")+") throwable;");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.push(apiClassName, "if (response != null) {");
        ctx.addIndent(apiClassName);
        ctx.push(apiClassName, "throw new "+ ctx.qualifyCommonApiClass("ApiException")+"(\""+ operationId +"\", response.statusCode(), response.headers().map(), throwable);");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.push(apiClassName, "throw new "+ ctx.qualifyCommonApiClass("ApiException")+"(\""+ operationId +"\", throwable);");
        ctx.subIndent(apiClassName);

        ctx.push(apiClassName, "}");
        ctx.subIndent(apiClassName);
        ctx.push(apiClassName, "}");
        ctx.push(apiClassName, "");
    }

    private static void generateResponseSwitchBlockCases(GenerationContext ctx, String apiClassName, String responseClassName, ResponseModel responseModel, String statusCodeVariantPrefix) {
        Map<String, PathSchemaModel> content = responseModel.getContent();
        if (content == null) {
            //NO-CONTENT
            ctx.push(apiClassName, "case \"no-content-type\": {");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, "response.body().close();");
            ctx.push(apiClassName, responseClassName+" responseObj = new "+ responseClassName +"("+ responseClassName +".Variant."+ statusCodeVariantPrefix + ", statusCode, headers.map(), null);");
            ctx.push(apiClassName, "validateResponse(context, responseObj);");
            ctx.push(apiClassName, "return responseObj;");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
            return;
        }
        PathSchemaModel jzon = content.get("application/json");
        if (jzon != null && !Util.isJsonStringSpecialSchema(jzon.getSchema())) {
            ctx.push(apiClassName, "case \"application/json\": {");
            ctx.addIndent(apiClassName);

            String enumName = null;
            if (Util.getEnumRecursive(jzon.getSchema()) != null) {
                enumName = responseClassName + "." + statusCodeVariantPrefix + "ResponseBodyEnum";
            }
            String modelClass = Util.findRecursiveTypeName(ctx, jzon.getSchema(), enumName);

            ctx.push(apiClassName, "Object result;");
            ctx.push(apiClassName, "try (java.io.InputStream is = response.body()) {");
            ctx.addIndent(apiClassName);


            if (modelClass.contains("<")) {
                //Model class is some generic type, that we need to refer to the type lookup table.
                String typesClass = ctx.qualifyImplClass("Types");
                Long typeCnt = ctx.getTypeInfo(modelClass);
                if (typeCnt == null) {
                    //This is the first time we have seen this type, so we need to add it to the type lookup table.
                    typeCnt = ctx.nextType();
                    ctx.push(typesClass, "");
                    ctx.push(typesClass, "/**");
                    ctx.push(typesClass, " * Generic type for " + modelClass.replace("<", "&lt").replace(">", "&gt"));
                    ctx.push(typesClass, " */");

                    ctx.push(typesClass, "public static final java.lang.reflect.Type TYPE" + typeCnt + ";");
                    ctx.push(typesClass, "static {");
                    ctx.addIndent(typesClass);
                    ctx.push(typesClass, "TypeInfo typ = new TypeInfo<" + modelClass + ">() {};");
                    ctx.push(typesClass, "TYPE" + typeCnt + " = typ.getType();");
                    ctx.subIndent(typesClass);
                    ctx.push(typesClass, "}");
                    ctx.push(typesClass, "");

                    //Add it to the lookup table.
                    ctx.setTypeInfo(modelClass, typeCnt);
                }

                ctx.push(apiClassName, "result = deserializeJsonData(context, " + typesClass + ".TYPE" + typeCnt + ", statusCode, headers, is);");
            } else {
                //Model class is a simple class we can reference by using the .class syntax.
                ctx.push(apiClassName, "result = deserializeJsonData(context, " + modelClass + ".class, statusCode, headers, is);");
            }

            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
            ctx.push(apiClassName, responseClassName + " responseObj = new "+ responseClassName +"("+ responseClassName +".Variant."+ statusCodeVariantPrefix +"_APPLICATION_JSON, statusCode, headers.map(), result);");
            ctx.push(apiClassName, "validateResponse(context, responseObj);");
            ctx.push(apiClassName, "return responseObj;");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
        }

        if (jzon != null && Util.isJsonStringSpecialSchema(jzon.getSchema())) {
            ctx.push(apiClassName, "case \"application/json\": {");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, "Object result;");
            ctx.push(apiClassName, "try (java.io.InputStream input = response.body()){");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, "result = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
            ctx.push(apiClassName, responseClassName + " responseObj = new "+ responseClassName +"("+ responseClassName +".Variant."+ statusCodeVariantPrefix +"_APPLICATION_JSON, statusCode, headers.map(), result);");
            ctx.push(apiClassName, "validateResponse(context, responseObj);");
            ctx.push(apiClassName, "return responseObj;");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
        }

        PathSchemaModel plain = content.get("text/plain");
        if (plain != null) {
            if (!content.containsKey("*/*") && !content.containsKey("application/octet-stream")) {
                ctx.push(apiClassName, "case \"no-content-type\":");
                ctx.push(apiClassName, "//FALL THROUGH");
            }
            ctx.push(apiClassName, "case \"text/plain\": {");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, "Object result;");
            ctx.push(apiClassName, "try (java.io.InputStream input = response.body()){");
            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, "result = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
            ctx.push(apiClassName, responseClassName + " responseObj = new "+ responseClassName +"("+ responseClassName +".Variant."+ statusCodeVariantPrefix +"_TEXT_PLAIN, statusCode, headers.map(), result);");
            ctx.push(apiClassName, "validateResponse(context, responseObj);");
            ctx.push(apiClassName, "return responseObj;");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
        }

        for (Map.Entry<String, PathSchemaModel> e : content.entrySet()) {
            if (e.getKey().equals("application/json") || e.getKey().equals("text/plain")) {
                continue;
            }

            String ct = e.getKey();
            String contentTypeSuffix = Util.contentTypeToEnumPrefix(ct);

            //Special case for ANY
            if (e.getKey().equals("*/*")) {
                ctx.push(apiClassName,"default: {");
            } else {
                if (e.getKey().equals("application/octet-stream") && !content.containsKey("*/*")) {
                    ctx.push(apiClassName, "case \"no-content-type\":");
                    ctx.push(apiClassName, "//FALL THROUGH");
                }
                ctx.push(apiClassName,"case \""+Util.escapeForSourceCode(e.getKey().toLowerCase())+"\": {");
            }

            ctx.addIndent(apiClassName);
            ctx.push(apiClassName, responseClassName + " responseObj = new "+ responseClassName +"("+ responseClassName +".Variant."+ statusCodeVariantPrefix +"_"+contentTypeSuffix + ", statusCode, headers.map(), processResponseForBinaryPayload(context, response));");
            ctx.push(apiClassName, "validateResponse(context, responseObj);");
            ctx.push(apiClassName, "return responseObj;");
            ctx.subIndent(apiClassName);
            ctx.push(apiClassName, "}");
        }
    }

    private static void generateOperationParameterTransferToRequestContext(GenerationContext ctx, PathModel model, String requestParameterClass, String className) {
        if (model.getParameters() == null) {
            return;
        }

        int idx = -1;
        for (ParameterModel parameter : model.getParameters()) {
            idx++;
            String mgl = ctx.getMangledRequestParameterName(requestParameterClass, idx);
            ParameterModel prm = ctx.findParameter(parameter.get$ref());
            SchemaClassification clazz = SchemaClassification.fromSchema("pm " + idx, prm.getSchema());
            String getter = "get" + Util.capitalize(mgl);
            ctx.push(className, "if (param."+getter+"() != null) {");
            ctx.addIndent(className);

            switch (clazz) {
                case STRING:
                case INT64:
                case INT32:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                    RequestParamGenerator.generatePrimitiveParameter(ctx, className, prm, getter);
                    break;

                case ARRAY_STRING:
                case ARRAY_BOOLEAN:
                case ARRAY_INT64:
                case ARRAY_INT32:
                case ARRAY_FLOAT:
                case ARRAY_DOUBLE:
                    RequestParamGenerator.generateArrayParameter(ctx, className, prm, getter);
                    break;
                case REF:
                    throw new UnsupportedOperationException("not implemented yet");
                case MAP_STRING:
                case MAP_BOOLEAN:
                case MAP_INT64:
                case MAP_INT32:
                case MAP_FLOAT:
                case MAP_DOUBLE:
                case MAP_OBJECT_REF:
                case MAP_ANY:
                case MAP_ARRAY_STRING:
                case MAP_ARRAY_BOOLEAN:
                case MAP_ARRAY_INT64:
                case MAP_ARRAY_INT32:
                case MAP_ARRAY_FLOAT:
                case MAP_ARRAY_DOUBLE:
                case MAP_ARRAY_OBJECT_REF:
                case MULTI_DIMENSIONAL_IMPL:
                case MAP_ARRAY_ANY:
                case MULTI_DIMENSIONAL_SET:
                case MULTI_DIMENSIONAL_MAP:
                case ARRAY_OBJECT_REF:
                case ARRAY_ANY:
                case MULTI_DIMENSIONAL_ARRAY:
                case ARRAY_MAP_STRING:
                case ARRAY_MAP_BOOLEAN:
                case ARRAY_MAP_INT64:
                case ARRAY_MAP_INT32:
                case ARRAY_MAP_FLOAT:
                case ARRAY_MAP_DOUBLE:
                case ARRAY_MAP_OBJECT_REF:
                case ARRAY_MAP_ANY:
                case ANY:
                    throw new UnsupportedOperationException("not implemented yet");
                default:
                    throw new UnsupportedOperationException("not supported " + clazz + " input parameter");
            }
            ctx.subIndent(className);
            ctx.push(className, "}");
        }

    }
}
