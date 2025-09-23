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

import ch.imagic.openapi.model.HeaderModel;
import ch.imagic.openapi.model.PathModel;
import ch.imagic.openapi.model.PathSchemaModel;
import ch.imagic.openapi.model.ResponseModel;

import java.util.*;

public class ResponseModelGenerator {

    public static String generateResponseModel(GenerationContext ctx, PathModel model) {
        String name = ctx.operationIdToResponseClass(model.getOperationId());
        String clazz = ctx.qualifyResponseClass(name);
        if (ctx.isCompatResponse(clazz)) {
            return clazz;
        }

        if (ctx.getGenerated().containsKey(clazz)) {
            int cnt = 0;
            while (ctx.getGenerated().containsKey(clazz + cnt)) {
                cnt++;
            }
            clazz = clazz + cnt;
            name = name + cnt;
        }

        Set<String> mangledNames = new LinkedHashSet<>();
        mangledNames.add("variant");
        mangledNames.add("body");
        mangledNames.add("statusCode");
        mangledNames.add("headers");
        mangledNames.add("contentType");
        mangledNames.add("contentLength");

        ctx.push(clazz, "package " + ctx.getResponsePackage() + ";");
        ctx.push(clazz, "");
        ctx.push(clazz, "public class " + name + " implements java.io.Serializable, " + ctx.qualifyCommonApiClass("ToString") + ", " + ctx.qualifyCommonApiClass("Response") +" {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "");

        ctx.push(clazz, "public enum Variant {");
        ctx.addIndent(clazz);

        Map<String, Map<String, HeaderModel>> headerModelPerVariant = new LinkedHashMap<>();
        Map<String, String> variantNameToStatusCodePrefix = new LinkedHashMap<>();
        for (Map.Entry<String, ResponseModel> e1 : model.getResponses().entrySet()) {
            String statusCode = Util.capitalize(e1.getKey());
            if (statusCode.isEmpty()) {
                throw new IllegalStateException("status code is blank");
            }
            if (Character.isDigit(statusCode.charAt(0))) {
                statusCode = "S" + statusCode;
            }
            ResponseModel responseModel = ctx.findResponse(e1.getValue().get$ref());
            Map<String, HeaderModel> headers = responseModel.getHeaders();
            if (responseModel.getContent() == null || responseModel.getContent().isEmpty()) {
                String variantName = Util.mangleName(statusCode.toUpperCase());
                ctx.push(clazz, variantName +",");
                headerModelPerVariant.put(variantName, headers);
                variantNameToStatusCodePrefix.put(variantName, statusCode);
                continue;
            }
            for (Map.Entry<String, PathSchemaModel> e2 : responseModel.getContent().entrySet()) {
                String contentType = e2.getKey();
                String variantName = statusCode.toUpperCase() + "_" + Util.contentTypeToEnumPrefix(contentType);
                if (!Util.mangleName(variantName).equals(variantName)) {
                    throw new IllegalStateException("invalid status code or content type status: '" + statusCode + "' contentType: '" + contentType + "'");
                }
                ctx.push(clazz, variantName +",");
                headerModelPerVariant.put(variantName, headers);
                variantNameToStatusCodePrefix.put(variantName, statusCode);
            }
        }

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        boolean hasTrivial404 = false;
        if (variantNameToStatusCodePrefix.containsKey("S404")) {
            hasTrivial404 = true;
        }

        ctx.push(clazz, "private final Variant variant;");
        ctx.push(clazz, "");
        ctx.push(clazz, "private final int statusCode;");
        ctx.push(clazz, "");
        ctx.push(clazz, "private final java.util.Map<String, java.util.List<String>> headers;");
        ctx.push(clazz, "");

        if (ctx.jsr380()) {
            ctx.push(clazz, "@jakarta.validation.Valid");
        }
        ctx.push(clazz, "private final Object body;");
        ctx.push(clazz, "");

        Map<String, String> headerFieldNamesToTypes = new HashMap<>();
        Map<String, String> headerFieldNamesToEnumName = new HashMap<>();
        for (Map.Entry<String, Map<String, HeaderModel>> e : headerModelPerVariant.entrySet()) {
            String variantName = e.getKey();
            Map<String, HeaderModel> hdr = e.getValue();
            if (hdr == null) {
                continue;
            }
            for (Map.Entry<String, HeaderModel> e2 : hdr.entrySet()) {
                String headerName =  e2.getKey();
                String prefix = variantNameToStatusCodePrefix.get(variantName);
                String fieldName = Util.mangleName(prefix + Util.capitalize(headerName));
                mangledNames.add(fieldName);

                HeaderModel hm = ctx.findHeader(e2.getValue().get$ref());
                SchemaClassification.fromSchema("response header " + model.getOperationId() + " " + variantName + " " + headerName, hm.getSchema());
                List<String> enumRecursive = Util.getEnumRecursive(hm.getSchema());
                String enumName = null;
                if (enumRecursive != null) {
                    enumName = Util.capitalize(fieldName) + "Enum";
                }

                String typeName = Util.findRecursiveTypeName(ctx, hm.getSchema(), enumName);
                if (headerFieldNamesToTypes.put(fieldName, typeName) != null) {
                    continue;
                }
                if (enumName != null) {
                    Util.generateEnum(ctx, clazz, enumName, enumRecursive);
                    headerFieldNamesToEnumName.put(fieldName, enumName);
                }
                ctx.push(clazz, "private " + typeName + " "+ fieldName  +";");
            }
        }

        ctx.push(clazz, "");

        ctx.push(clazz, "public " + name + "(Variant variant, int statusCode, java.util.Map<String, java.util.List<String>> headers, Object body) throws " +ctx.qualifyCommonApiClass("ApiException") + "{");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this.variant = variant;");
        ctx.push(clazz, "this.body = body;");
        ctx.push(clazz, "this.headers = headers;");
        ctx.push(clazz, "this.statusCode = statusCode;");
        ctx.push(clazz, "");
        for (Map.Entry<String, Map<String, HeaderModel>> e : headerModelPerVariant.entrySet()) {
            String variantName = e.getKey();
            Map<String, HeaderModel> hdr = e.getValue();
            if (hdr == null) {
                continue;
            }
            for (Map.Entry<String, HeaderModel> e2 : hdr.entrySet()) {
                String headerName =  e2.getKey();
                String prefix = variantNameToStatusCodePrefix.get(variantName);
                String fieldName = Util.mangleName(prefix + Util.capitalize(headerName));
                ctx.push(clazz, "if (variant == Variant." + variantName + ") {");
                ctx.addIndent(clazz);
                //CODE that parses header here
                HeaderModel hm = ctx.findHeader(e2.getValue().get$ref());
                SchemaClassification hdrClazz = SchemaClassification.fromSchema("response header " + model.getOperationId() + " " + variantName + " " + headerName, hm.getSchema());
                ctx.push(clazz, "java.util.List<String> theHdr = headers.getOrDefault(\"" + Util.escapeForSourceCode(headerName)+ "\", java.util.Collections.emptyList());");

                ctx.push(clazz, "if (theHdr.isEmpty()) {");
                ctx.addIndent(clazz);
                if (hm.isRequired()) {
                    ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is required and was not found in the response headers\");");
                } else {
                    ctx.push(clazz, "this." + fieldName + " = null;");
                }

                ctx.subIndent(clazz);
                ctx.push(clazz, "} else if (theHdr.size() > 1) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is present more than once in the response\");");
                ctx.subIndent(clazz);
                ctx.push(clazz, "} else {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "String rawHdr = theHdr.get(0);");

                switch (hdrClazz) {
                    case ENUM: {
                        String enumName = headerFieldNamesToEnumName.get(fieldName);
                        ResponseParserGenerator.generateParseEnumHeader(ctx, model, clazz, fieldName, headerName, enumName);
                        break;
                    }
                    case ARRAY_ENUM: {
                        String enumName = headerFieldNamesToEnumName.get(fieldName);
                        ResponseParserGenerator.generateParseEnumArrayHeader(ctx, model, clazz, fieldName, headerName, enumName);
                        break;
                    }
                    case MAP_ENUM: {
                        String enumName = headerFieldNamesToEnumName.get(fieldName);
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseEnumMapHeaderExplode(ctx, model, clazz, fieldName, headerName, enumName);
                        } else {
                            ResponseParserGenerator.generateParseEnumMapHeader(ctx, model, clazz, fieldName, headerName, enumName);
                        }
                        break;
                    }
                    case ANY:
                        //LATER
                    case STRING:
                        ctx.push(clazz, "this." + fieldName + " = rawHdr;");
                        break;
                    case INT64:
                        ResponseParserGenerator.generateParseInt64Header(ctx, model, clazz, fieldName, headerName);
                        break;
                    case INT32:
                        ResponseParserGenerator.generateParseInt32Header(ctx, model, clazz, fieldName, headerName);
                        break;
                    case FLOAT:
                        ResponseParserGenerator.generateParseFloatHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case DOUBLE:
                        ResponseParserGenerator.generateParseDoubleHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case BOOLEAN:
                        ResponseParserGenerator.generateParseBooleanHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case ARRAY_ANY:
                        //LATER
                    case ARRAY_STRING:
                        ResponseParserGenerator.generateParseStringArrayHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case ARRAY_BOOLEAN:
                        ResponseParserGenerator.generateParseBooleanArrayHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case ARRAY_INT64:
                        ResponseParserGenerator.generateParseInt64ArrayHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case ARRAY_INT32:
                        ResponseParserGenerator.generateParseInt32ArrayHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case ARRAY_FLOAT:
                        ResponseParserGenerator.generateParseFloatArrayHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case ARRAY_DOUBLE:
                        ResponseParserGenerator.generateParseDoubleArrayHeader(ctx, model, clazz, fieldName, headerName);
                        break;
                    case MAP_ANY:
                        //LATER
                    case MAP_STRING:
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseStringMapHeaderExplode(ctx, model, clazz, fieldName, headerName);
                        } else {
                            ResponseParserGenerator.generateParseStringMapHeader(ctx, model, clazz, fieldName, headerName);
                        }
                        break;
                    case MAP_BOOLEAN:
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseBooleanMapHeaderExplode(ctx, model, clazz, fieldName, headerName);
                        } else {
                            ResponseParserGenerator.generateParseBooleanMapHeader(ctx, model, clazz, fieldName, headerName);
                        }
                        break;
                    case MAP_INT64:
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseInt64MapHeaderExplode(ctx, model, clazz, fieldName, headerName);
                        } else {
                            ResponseParserGenerator.generateParseInt64MapHeader(ctx, model, clazz, fieldName, headerName);
                        }
                        break;
                    case MAP_INT32:
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseInt32MapHeaderExplode(ctx, model, clazz, fieldName, headerName);
                        } else {
                            ResponseParserGenerator.generateParseInt32MapHeader(ctx, model, clazz, fieldName, headerName);
                        }
                        break;
                    case MAP_FLOAT:
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseFloatMapHeaderExplode(ctx, model, clazz, fieldName, headerName);
                        } else {
                            ResponseParserGenerator.generateParseFloatMapHeader(ctx, model, clazz, fieldName, headerName);
                        }
                        break;
                    case MAP_DOUBLE:
                        if (hm.isExplode()) {
                            ResponseParserGenerator.generateParseDoubleMapHeaderExplode(ctx, model, clazz, fieldName, headerName);
                        } else {
                            ResponseParserGenerator.generateParseDoubleMapHeader(ctx, model, clazz, fieldName, headerName);
                        }
                        break;

                    case REF:
                        //LATER
                        throw new UnsupportedOperationException("Not implemented yet, header parsing for " + hdrClazz);
                    case ARRAY_OBJECT_REF:
                    case MAP_OBJECT_REF:
                    case MAP_ARRAY_STRING:
                    case MAP_ARRAY_BOOLEAN:
                    case MAP_ARRAY_INT64:
                    case MAP_ARRAY_INT32:
                    case MAP_ARRAY_FLOAT:
                    case MAP_ARRAY_DOUBLE:
                    case MAP_ARRAY_OBJECT_REF:
                    case MAP_ARRAY_ANY:
                    case MULTI_DIMENSIONAL_ARRAY:
                    case MULTI_DIMENSIONAL_SET:
                    case MULTI_DIMENSIONAL_MAP:
                    case ARRAY_MAP_STRING:
                    case ARRAY_MAP_BOOLEAN:
                    case ARRAY_MAP_INT64:
                    case ARRAY_MAP_INT32:
                    case ARRAY_MAP_FLOAT:
                    case ARRAY_MAP_DOUBLE:
                    case ARRAY_MAP_OBJECT_REF:
                    case ARRAY_MAP_ANY:
                        throw new IllegalStateException("Object is too complex for header parsing. " + hdrClazz);
                    default:
                        throw new IllegalStateException("HDR clazz " + hdrClazz);
                }
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");

                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
            }
        }

        generateConstructorTypeVerifier(ctx, model, clazz);

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        ctx.push(clazz, "public Object getRawBody() {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "return this.body;");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        ctx.push(clazz, "public Variant getVariant() {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "return this.variant;");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        ctx.push(clazz, "public int getStatusCode() {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "return this.statusCode;");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        ctx.push(clazz, "public java.util.Map<String, java.util.List<String>> getRawHeaders() {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "return this.headers;");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        for (Map.Entry<String, String> e : headerFieldNamesToTypes.entrySet()) {

            String fieldName = e.getKey();
            String fieldType = e.getValue();

            //TODO check mangles, probably incorrect
            ctx.push(clazz, "public " + fieldType +  " get" + Util.capitalize(fieldName) + "() {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return this." + fieldName + ";");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");
        }

        ctx.push(clazz, "public <T, E extends Throwable> T map("+ctx.qualifyCommonApiClass("Response")+".ResponseMapper<"+clazz+", T, E> mapper) throws E {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "return mapper.map(this);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "");

        for (Map.Entry<String, ResponseModel> e1 : model.getResponses().entrySet()) {
            ResponseModel resp = ctx.findResponse(e1.getValue().get$ref());

            if (resp.getContent() == null) {
                continue;
            }

            String statusCode = Util.capitalize(e1.getKey());
            if (!"DEFAULT".equalsIgnoreCase(statusCode) && !Util.isStatusCode(statusCode)) {
                System.out.println("WARNING operation " + model.getOperationId() + " has invalid status code '" + statusCode + "' ");
                continue;
            }
            if (Character.isDigit(statusCode.charAt(0))) {
                statusCode = "S" + statusCode;
            }
            String variantName = Util.mangleName(statusCode.toUpperCase() + "_APPLICATION_JSON").toUpperCase();
            if (resp.getContent() == null) {
                continue;
            }
            PathSchemaModel psm = resp.getContent().get("application/json");
            if (psm == null) {
                //NO JSON BODY
                continue;
            }

            List<String> enumValues = Util.getEnumRecursive(psm.getSchema());
            String enumName = null;
            if (enumValues != null) {
                enumName = statusCode + "ResponseBodyEnum";
                Util.generateEnum(ctx, clazz, enumName, enumValues);
            }

            String jsonBodyTypeName = Util.findRecursiveTypeName(ctx, psm.getSchema(), enumName);

            if (Util.isJsonStringSpecialSchema(psm.getSchema())) {
                jsonBodyTypeName = "String";
            }

            ctx.push(clazz, "public boolean is" + statusCode + "Json() {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return variant == Variant." + variantName + ";");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "public void assert" + statusCode + "Json() throws " +  ctx.qualifyCommonApiClass("ApiException")+ " {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "if (variant == Variant." + variantName + ") {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return;");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Unexpected variant received: \" + this.variant);");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "public " + jsonBodyTypeName +  " getResponseBody" + statusCode + "Json() throws " + ctx.qualifyCommonApiClass("ApiException") + " {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Wrong body for variant \" + this.variant);");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "return (" + jsonBodyTypeName+ ")this.body;");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            if (hasTrivial404) {
                ctx.push(clazz, "public java.util.Optional<" + jsonBodyTypeName + "> getResponseBody" + statusCode + "JsonOr404() throws " + ctx.qualifyCommonApiClass("ApiException") + " {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant != Variant.S404) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Wrong body for variant \" + this.variant);");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "return java.util.Optional.empty();");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");

                ctx.push(clazz, "return java.util.Optional.ofNullable((" + jsonBodyTypeName+ ") this.body);");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");
            }

            ctx.push(clazz, "public java.util.Optional<" + jsonBodyTypeName +  "> tryGetResponseBody" + statusCode + "Json() {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return java.util.Optional.empty();");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "return java.util.Optional.ofNullable((" + jsonBodyTypeName+ ") this.body);");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");
        }

        for (Map.Entry<String, ResponseModel> e1 : model.getResponses().entrySet()) {
            ResponseModel resp = ctx.findResponse(e1.getValue().get$ref());

            if (resp.getContent() == null) {
                continue;
            }

            String statusCode = Util.capitalize(e1.getKey());
            if (!"DEFAULT".equalsIgnoreCase(statusCode) && !Util.isStatusCode(statusCode)) {
                System.out.println("WARNING operation " + model.getOperationId() + " has invalid status code '" + statusCode + "' ");
                continue;
            }

            if (Character.isDigit(statusCode.charAt(0))) {
                statusCode = "S" + statusCode;
            }
            String variantName = Util.mangleName(statusCode.toUpperCase() + "_TEXT_PLAIN").toUpperCase();
            if (resp.getContent() == null) {
                continue;
            }
            PathSchemaModel psm = resp.getContent().get("text/plain");
            if (psm == null) {
                //NO PLAIN TEXT BODY
                continue;
            }

            ctx.push(clazz, "public boolean is" + statusCode + "String() {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return variant == Variant." + variantName + ";");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "public void assert" + statusCode + "String() throws " +  ctx.qualifyCommonApiClass("ApiException")+ " {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "if (variant == Variant." + variantName + ") {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return;");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Unexpected variant received: \" + this.variant);");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "public String getResponseBody" + statusCode + "String() throws " + ctx.qualifyCommonApiClass("ApiException") + "{");
            ctx.addIndent(clazz);
            ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Wrong body for variant \" + this.variant);");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "return this.body == null ? null : String.valueOf(this.body);");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            if (hasTrivial404) {
                ctx.push(clazz, "public java.util.Optional<String> getResponseBody" + statusCode + "StringOr404() throws " + ctx.qualifyCommonApiClass("ApiException") + " {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant != Variant.S404) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Wrong body for variant \" + this.variant);");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "return java.util.Optional.empty();");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");

                ctx.push(clazz, "return java.util.Optional.ofNullable(this.body == null ? null : String.valueOf(this.body));");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");
            }

            ctx.push(clazz, "public java.util.Optional<String> tryGetResponseBody" + statusCode + "String() {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
            ctx.addIndent(clazz);
            ctx.push(clazz, "return java.util.Optional.empty();");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");

            ctx.push(clazz, "return  java.util.Optional.ofNullable(String.valueOf(this.body));");
            ctx.subIndent(clazz);
            ctx.push(clazz, "}");
            ctx.push(clazz, "");
        }

        for (Map.Entry<String, ResponseModel> e1 : model.getResponses().entrySet()) {
            ResponseModel resp = ctx.findResponse(e1.getValue().get$ref());

            String statusCode = Util.capitalize(e1.getKey());

            if (!"DEFAULT".equalsIgnoreCase(statusCode) && !Util.isStatusCode(statusCode)) {
                System.out.println("WARNING operation " + model.getOperationId() + " has invalid status code '" + statusCode + "' ");
                continue;
            }

            if (Character.isDigit(statusCode.charAt(0))) {
                statusCode = "S" + statusCode;
            }

            if (resp.getContent() == null) {
                ctx.push(clazz, "public boolean is" + statusCode +"() {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "return variant == Variant." + statusCode + ";");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");

                ctx.push(clazz, "public void assert" + statusCode + "() throws " +  ctx.qualifyCommonApiClass("ApiException")+ " {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant == Variant." + statusCode + ") {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "return;");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Unexpected variant received: \" + this.variant);");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");

                continue;
            }

            for (Map.Entry<String, PathSchemaModel> e2 : resp.getContent().entrySet()) {
                String contentType = e2.getKey();
                if (contentType.equals("application/json") || contentType.equals("text/plain")) {
                    continue;
                }

                String variantName = Util.mangleName(statusCode.toUpperCase() + "_" + Util.contentTypeToEnumPrefix(contentType));

                String mangledSuffix = statusCode + Util.capitalize(Util.mangleContentType(e2.getKey()));

                ctx.push(clazz, "public boolean is" + mangledSuffix + "() {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "return variant == Variant." + variantName + ";");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");

                ctx.push(clazz, "public void assert" + mangledSuffix + "() throws " +  ctx.qualifyCommonApiClass("ApiException")+ " {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant == Variant." + variantName + ") {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "return;");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Unexpected variant received: \" + this.variant);");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");

                if (hasTrivial404) {
                    ctx.push(clazz, "public java.util.Optional<"+ctx.qualifyCommonApiClass("BinaryPayload")+"> getResponseBody" + mangledSuffix + "Or404() throws " + ctx.qualifyCommonApiClass("ApiException") + " {");
                    ctx.addIndent(clazz);
                    ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
                    ctx.addIndent(clazz);
                    ctx.push(clazz, "if (variant != Variant.S404) {");
                    ctx.addIndent(clazz);
                    ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Wrong body for variant \" + this.variant);");
                    ctx.subIndent(clazz);
                    ctx.push(clazz, "}");
                    ctx.push(clazz, "return java.util.Optional.empty();");
                    ctx.subIndent(clazz);
                    ctx.push(clazz, "}");
                    ctx.push(clazz, "");

                    ctx.push(clazz, "return java.util.Optional.ofNullable(("+ ctx.qualifyCommonApiClass("BinaryPayload")+") this.body);");
                    ctx.subIndent(clazz);
                    ctx.push(clazz, "}");
                    ctx.push(clazz, "");
                }

                ctx.push(clazz, "public " + ctx.qualifyCommonApiClass("BinaryPayload") + " getResponseBody" + mangledSuffix + "() throws " + ctx.qualifyCommonApiClass("ApiException") + " {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "if (variant != Variant." + variantName + ") {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Wrong body for variant \" + this.variant);");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "return (" + ctx.qualifyCommonApiClass("BinaryPayload")+ ") this.body;");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
                ctx.push(clazz, "");


            }

        }

        List<String> members = new ArrayList<>(mangledNames);
        members.remove("contentType"); //only getter from interface
        members.remove("contentLength"); //only getter from interface


        ctx.push(clazz, "/**");
        ctx.push(clazz, " * This method always throws the response as an exception. It doesnt return, it always throws.");
        ctx.push(clazz,
                    " * If this response contains binary data that is not yet received, ",
                 " * then that data may be truncated by this call and the binary stream will always be closed.");
        ctx.push(clazz, " */");
        ctx.push(clazz, "public "+ ctx.qualifyCommonApiClass("ApiException") +" throwIt() throws " + ctx.qualifyCommonApiClass("ApiException") + " {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");

        Util.generateHashCodeEquals(ctx, clazz, members);
        Util.generateToString(ctx, clazz, name, members);

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");

        return clazz;
    }

    private static void generateConstructorTypeVerifier(GenerationContext ctx, PathModel model, String clazz) {
        for (Map.Entry<String, ResponseModel> e1 : model.getResponses().entrySet()) {
            ResponseModel resp = ctx.findResponse(e1.getValue().get$ref());

            if (resp.getContent() == null) {
                continue;
            }

            String statusCode = Util.capitalize(e1.getKey());
            if (!"DEFAULT".equalsIgnoreCase(statusCode) && !Util.isStatusCode(statusCode)) {
                System.out.println("WARNING operation " + model.getOperationId() + " has invalid status code '" + statusCode + "' ");
                continue;
            }
            if (Character.isDigit(statusCode.charAt(0))) {
                statusCode = "S" + statusCode;
            }

            if (resp.getContent() == null) {
                continue;
            }
            PathSchemaModel tp = resp.getContent().get("text/plain");
            if (tp != null) {
                String variantName = Util.mangleName(statusCode.toUpperCase() + "_TEXT_PLAIN").toUpperCase();
                ctx.push(clazz, "if (variant == Variant." + variantName + " && !(this.body instanceof String)) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Response body type mismatch when constructing response object for variant '" + variantName + "'\");");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
            }

            PathSchemaModel aj = resp.getContent().get("application/json");
            if (aj != null) {
                String variantName = Util.mangleName(statusCode.toUpperCase() + "_APPLICATION_JSON").toUpperCase();

                List<String> enumValues = Util.getEnumRecursive(aj.getSchema());
                String enumName = null;
                if (enumValues != null) {
                    enumName = statusCode + "ResponseBodyEnum";
                }

                String jsonBodyTypeName = Util.findRecursiveTypeName(ctx, aj.getSchema(), enumName);

                if (Util.isJsonStringSpecialSchema(aj.getSchema())) {
                    jsonBodyTypeName = "String";
                }

                //This is quite pragmatic, but I cant think of anything better currently.
                if (jsonBodyTypeName.startsWith("java.util.List<")) {
                    jsonBodyTypeName = "java.util.List";
                }

                if (jsonBodyTypeName.startsWith("java.util.Map<")) {
                    jsonBodyTypeName = "java.util.Map";
                }

                if (jsonBodyTypeName.contains("<")) {
                    jsonBodyTypeName = "java.lang.Object";
                }

                ctx.push(clazz, "if (variant == Variant." + variantName + " && !(this.body instanceof " + jsonBodyTypeName + ")) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Response body type mismatch when constructing response object for variant '" + variantName + "'\");");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
            }

            for (Map.Entry<String, PathSchemaModel> e2 : resp.getContent().entrySet()) {
                String contentType = e2.getKey();
                if (contentType.equals("application/json") || contentType.equals("text/plain")) {
                    continue;
                }

                String variantName = Util.mangleName(statusCode.toUpperCase() + "_" + Util.contentTypeToEnumPrefix(contentType));
                String binaryPayload = ctx.qualifyCommonApiClass("BinaryPayload");

                ctx.push(clazz, "if (variant == Variant." + variantName + " && !(this.body instanceof " + binaryPayload + ")) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Response body type mismatch when constructing response object for variant '" + variantName + "'\");");
                ctx.subIndent(clazz);
                ctx.push(clazz, "}");
            }
        }
    }
}
