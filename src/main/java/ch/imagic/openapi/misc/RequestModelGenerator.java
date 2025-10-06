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

import ch.imagic.openapi.model.ParameterModel;
import ch.imagic.openapi.model.PathModel;
import ch.imagic.openapi.model.PathSchemaModel;
import ch.imagic.openapi.model.RequestBodyModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestModelGenerator {

    public static String generateRequestModel(GenerationContext ctx, PathModel model, String path, String mimeType) {

        String name = ctx.operationIdAndMimeToRequestClass(model.getOperationId(), mimeType);
        String clazz = ctx.qualifyRequestClass(name);

        if (!ctx.isCompatRequest(clazz) && ctx.getGenerated().containsKey(clazz)) {
            //TODO is this needed?, probably doesnt work either...
            int cnt = 0;
            while (ctx.getGenerated().containsKey(clazz + cnt)) {
                cnt++;
            }
            clazz = clazz + cnt;
            name = name + cnt;
        }

        Set<String> mangledNames = new LinkedHashSet<>();
        mangledNames.add("requestBody");
        mangledNames.add("requestTimeout");
        mangledNames.add("responseBodyReadTimeout");
        mangledNames.add("responseBodyTotalTimeout");
        mangledNames.add("additionalHeaderParameter");
        mangledNames.add("additionalQueryParameter");

        if (model.getParameters() != null) {
            int idx = -1;
            for (ParameterModel parameter : model.getParameters()) {
                idx++;
                ParameterModel pm = ctx.findParameter(parameter.get$ref());
                String pname = pm.getName();
                String mngl = Util.mangleName(pname);
                if (!mangledNames.add(mngl)) {
                    int cnt = 0;
                    while (!mangledNames.add(mngl + cnt)) {
                        cnt++;
                    }
                    mngl = mngl + cnt;
                }

                ctx.putMangledRequestParameterName(clazz, idx, mngl);
            }
        }

        if (ctx.isCompatRequest(clazz)) {
            return clazz;
        }

        ctx.push(clazz, "package " + ctx.getReqParamPackage() + ";");
        ctx.push(clazz, "public class " + name + " implements java.io.Serializable, " + ctx.qualifyCommonApiClass("ToString") + ", " + ctx.qualifyCommonApiClass("RequestParameters") + " {");
        ctx.addIndent(clazz);

        if (model.getParameters() != null) {
            int idx = -1;
            for (ParameterModel parameter : model.getParameters()) {
                idx++;
                ParameterModel pm = ctx.findParameter(parameter.get$ref());

                SchemaClassification paramClazz = SchemaClassification.fromSchema("parameter " + pm.getName() + " for op " + model.getOperationId(), pm.getSchema());
                String paramType = getParameterMemberTypeName(ctx, model, pm, paramClazz);
                String mngl = ctx.getMangledRequestParameterName(clazz, idx);

                boolean required = pm.isRequired();

                switch (String.valueOf(pm.getIn()).toLowerCase()) {
                    case "path":
                        if (ctx.jsr380()) {
                            if (paramClazz == SchemaClassification.STRING) {
                                //Theres other case where we have to add NotEmpty, but there are very hard to detect and rare (mostly list/map parameter)
                                //This is a cheap win
                                if (!".*".equals(pm.getSchema().getPattern()) || !path.endsWith("{"+pm.getName()+"}")) {
                                    ctx.push(clazz, "@jakarta.validation.constraints.NotEmpty");
                                }
                            }
                        }
                        required = true;
                        break;
                    case "query":
                    case "header":
                        break;
                    default:
                        continue;
                }

                if (ctx.jsr380()) {
                    if (required) {
                        ctx.push(clazz, "@jakarta.validation.constraints.NotNull");
                    }
                }
                MemberGenerator.generateTrivialMember(ctx, clazz, mngl, mngl, name, paramType, pm.getSchema());
            }
        }

        if (mimeType != null) {
            if (mimeType.equals("application/json")) {
                RequestBodyModel requestBody = ctx.findRequestBody(model.getRequestBody().get$ref());
                PathSchemaModel pathSchemaModel = requestBody.getContent().get(mimeType);
                SchemaClassification childClass = SchemaClassification.fromSchema("request body " + model.getOperationId(), pathSchemaModel.getSchema());
                String propertyType;
                switch (childClass) {
                    case ARRAY_STRING: {
                        propertyType = "java.util.List<String>";
                        break;
                    }
                    case ARRAY_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getItems().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getItems().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = "java.util.List<" + ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getItems().get$ref())) + ">";
                        break;
                    }
                    case STRING: {
                        propertyType = "String";
                        break;
                    }
                    case ENUM:
                        //TODO support enum here
                        propertyType = "String";
                        break;
                    case INT64:
                        propertyType = "Long";
                        break;
                    case INT32:
                        propertyType = "Integer";
                        break;
                    case FLOAT:
                        propertyType = "Float";
                        break;
                    case DOUBLE:
                        propertyType = "Double";
                        break;
                    case BOOLEAN:
                        propertyType = "Boolean";
                        break;
                    case REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().get$ref()));
                        break;
                    }
                    case OBJECT_IMPL:
                    case MAP_IMPL:
                    case SET_IMPL:
                    case ARRAY_IMPL:
                    case MULTI_DIMENSIONAL_IMPL:
                    case ONE_OF:
                    case ANY_OF:
                    case UNION:
                        throw new IllegalStateException("generateRequestModel json body " + childClass);
                    case MAP_STRING:
                        propertyType = "java.util.Map<String, String>";
                        break;
                    case MAP_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.Map<String, String>";
                        break;
                    case MAP_BOOLEAN:
                        propertyType = "java.util.Map<String, Boolean>";
                        break;
                    case MAP_INT64:
                        propertyType = "java.util.Map<String, Long>";
                        break;
                    case MAP_INT32:
                        propertyType = "java.util.Map<String, Integer>";
                        break;
                    case MAP_FLOAT:
                        propertyType = "java.util.Map<String, Float>";
                        break;
                    case MAP_DOUBLE:
                        propertyType = "java.util.Map<String, Double>";
                        break;
                    case MAP_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getAdditionalProperties().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getAdditionalProperties().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        String componentType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getAdditionalProperties().get$ref()));
                        propertyType = "java.util.Map<String, "+componentType+">";
                        break;
                    }
                    case MAP_ANY:

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = "java.util.Map<String, Object>";
                        break;
                    case MAP_ARRAY_STRING:
                        propertyType = "java.util.Map<String, java.util.List<String>>";
                        break;
                    case MAP_ARRAY_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.Map<String, java.util.List<String>>";
                        break;
                    case MAP_ARRAY_BOOLEAN:
                        propertyType = "java.util.Map<String, java.util.List<Boolean>>";
                        break;
                    case MAP_ARRAY_INT64:
                        propertyType = "java.util.Map<String, java.util.List<Long>>";
                        break;
                    case MAP_ARRAY_INT32:
                        propertyType = "java.util.Map<String, java.util.List<Integer>>";
                        break;
                    case MAP_ARRAY_FLOAT:
                        propertyType = "java.util.Map<String, java.util.List<Float>>";
                        break;
                    case MAP_ARRAY_DOUBLE:
                        propertyType = "java.util.Map<String, java.util.List<Double>>";
                        break;
                    case MAP_ARRAY_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getAdditionalProperties().getItems().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getAdditionalProperties().getItems().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        String componentType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getAdditionalProperties().getItems().get$ref()));
                        propertyType = "java.util.Map<String, java.util.List<"+componentType+">>";
                        break;
                    }
                    case MAP_ARRAY_ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = "java.util.Map<String, java.util.List<Object>>";
                        break;
                    case MAP_SET_STRING:
                        propertyType = "java.util.Map<String, java.util.Set<String>>";
                        break;
                    case MAP_SET_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.Map<String, java.util.Set<String>>";
                        break;
                    case MAP_SET_BOOLEAN:
                        propertyType = "java.util.Map<String, java.util.Set<Boolean>>";
                        break;
                    case MAP_SET_INT64:
                        propertyType = "java.util.Map<String, java.util.Set<Long>>";
                        break;
                    case MAP_SET_INT32:
                        propertyType = "java.util.Map<String, java.util.Set<Integer>>";
                        break;
                    case MAP_SET_FLOAT:
                        propertyType = "java.util.Map<String, java.util.Set<Float>>";
                        break;
                    case MAP_SET_DOUBLE:
                        propertyType = "java.util.Map<String, java.util.Set<Double>>";
                        break;
                    case MAP_SET_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getAdditionalProperties().getItems().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getAdditionalProperties().getItems().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        String componentType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getAdditionalProperties().getItems().get$ref()));
                        propertyType = "java.util.Map<String, java.util.Set<"+componentType+">>";
                        break;
                    }
                    case MAP_SET_ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = "java.util.Map<String, java.util.Set<Object>>";
                        break;
                    case SET_STRING:
                        propertyType = "java.util.Set<String>";
                        break;
                    case SET_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.Set<String>";
                        break;
                    case SET_INT64:
                        propertyType = "java.util.Set<Long>";
                        break;
                    case SET_INT32:
                        propertyType = "java.util.Set<Integer>";
                        break;
                    case SET_FLOAT:
                        propertyType = "java.util.Set<Float>";
                        break;
                    case SET_DOUBLE:
                        propertyType = "java.util.Set<Double>";
                        break;
                    case SET_BOOLEAN:
                        propertyType = "java.util.Set<Boolean>";
                        break;
                    case SET_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getItems().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getItems().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        String componentType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getItems().get$ref()));
                        propertyType = "java.util.Set<"+componentType+">";
                        break;
                    }
                    case SET_ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }
                        propertyType = "java.util.Set<Object>";
                        break;
                    case SET_MAP_STRING:
                        propertyType = "java.util.Set<java.util.Map<String, String>>";
                        break;
                    case SET_MAP_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.Set<java.util.Map<String, String>>";
                        break;
                    case SET_MAP_BOOLEAN:
                        propertyType = "java.util.Set<java.util.Map<String, Boolean>>";
                        break;
                    case SET_MAP_INT64:
                        propertyType = "java.util.Set<java.util.Map<String, Long>>";
                        break;
                    case SET_MAP_INT32:
                        propertyType = "java.util.Set<java.util.Map<String, Integer>>";
                        break;
                    case SET_MAP_FLOAT:
                        propertyType = "java.util.Set<java.util.Map<String, Float>>";
                        break;
                    case SET_MAP_DOUBLE:
                        propertyType = "java.util.Set<java.util.Map<String, Double>>";
                        break;
                    case SET_MAP_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getItems().getAdditionalProperties().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getItems().getAdditionalProperties().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        String componentType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getItems().getAdditionalProperties().get$ref()));
                        propertyType = "java.util.Set<java.util.Map<String, "+componentType+">>";
                        break;
                    }
                    case SET_MAP_ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = "java.util.Set<java.util.Map<String, Object>>";
                        break;
                    case ARRAY_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.List<String>";
                        break;
                    case ARRAY_BOOLEAN:
                        //TODO support enum here
                        propertyType = "java.util.List<Boolean>";
                        break;
                    case ARRAY_INT64:
                        propertyType = "java.util.List<Long>";
                        break;
                    case ARRAY_INT32:
                        propertyType = "java.util.List<Integer>";
                        break;
                    case ARRAY_FLOAT:
                        propertyType = "java.util.List<Float>";
                        break;
                    case ARRAY_DOUBLE:
                        propertyType = "java.util.List<Double>";
                        break;
                    case ARRAY_ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        propertyType = "java.util.List<Object>";
                        break;
                    case ARRAY_MAP_STRING:
                        propertyType = "java.util.List<java.util.Map<String, String>>";
                        break;
                    case ARRAY_MAP_ENUM:
                        //TODO support enum here
                        propertyType = "java.util.List<java.util.Map<String, String>>";
                        break;
                    case ARRAY_MAP_BOOLEAN:
                        propertyType = "java.util.List<java.util.Map<String, Boolean>>";
                        break;
                    case ARRAY_MAP_INT64:
                        propertyType = "java.util.List<java.util.Map<String, Long>>";
                        break;
                    case ARRAY_MAP_INT32:
                        propertyType = "java.util.List<java.util.Map<String, Integer>>";
                        break;
                    case ARRAY_MAP_FLOAT:
                        propertyType = "java.util.List<java.util.Map<String, Float>>";
                        break;
                    case ARRAY_MAP_DOUBLE:
                        propertyType = "java.util.List<java.util.Map<String, Double>>";
                        break;
                    case ARRAY_MAP_OBJECT_REF: {
                        if (ctx.findSchema(pathSchemaModel.getSchema().getItems().getAdditionalProperties().get$ref()) == null) {
                            throw new IllegalStateException("model not found " + pathSchemaModel.getSchema().getItems().getAdditionalProperties().get$ref());
                        }

                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }

                        String componentType = ctx.qualifyModelClass(ctx.modelNameToJavaClass(pathSchemaModel.getSchema().getItems().getAdditionalProperties().get$ref()));
                        propertyType = "java.util.List<java.util.Map<String, "+componentType+">>";
                        break;
                    }
                    case ARRAY_MAP_ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }
                        propertyType = "java.util.List<java.util.Map<String, Object>>";
                        break;
                    case ANY:
                        if (ctx.jsr380()) {
                            ctx.push(clazz, "@jakarta.validation.Valid");
                        }
                        propertyType = "Object";
                        break;
                    case MULTI_DIMENSIONAL_SET:
                        //TODO
                    case MULTI_DIMENSIONAL_MAP:
                        //TODO
                    case MULTI_DIMENSIONAL_ARRAY:
                        //TODO
                    default: {
                        //TODO
                        throw new IllegalStateException("Not yet implemented " + childClass);
                    }
                }

                if (ctx.jsr380()) {
                    ctx.push(clazz, "@jakarta.validation.constraints.NotNull");
                }

                MemberGenerator.generateTrivialMember(ctx, clazz, "requestBody", "requestBody", name, propertyType, pathSchemaModel.getSchema());
            } else if (mimeType.equals("text/plain")) {
                if (ctx.jsr380()) {
                    ctx.push(clazz, "@jakarta.validation.constraints.NotNull");
                }
                MemberGenerator.generateTrivialMember(ctx, clazz, "requestBody", "requestBody", name, "String", null);
            } else {
                if (ctx.jsr380()) {
                    ctx.push(clazz, "@jakarta.validation.Valid");
                    ctx.push(clazz, "@jakarta.validation.constraints.NotNull");
                }
                MemberGenerator.generateTrivialMember(ctx, clazz, "requestBody", "requestBody", name, ctx.qualifyCommonApiClass("BinaryPayload"), null);


                ctx.push(clazz, "public " + clazz + " withRequestBody(java.io.InputStream value) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "this.requestBody = new " + ctx.qualifyCommonApiClass("InputStreamBinaryPayload") + "(value);");
                ctx.push(clazz, "return this;");
                ctx.subIndent(clazz);
                ctx.push(clazz,"}");
                ctx.push(clazz, "");

                ctx.push(clazz, "public " + clazz + " withRequestBody(String value) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "this.requestBody = new " + ctx.qualifyCommonApiClass("ByteArrayBinaryPayload") + "(value);");
                ctx.push(clazz, "return this;");
                ctx.subIndent(clazz);
                ctx.push(clazz,"}");
                ctx.push(clazz, "");

                ctx.push(clazz, "public " + clazz + " withRequestBody(java.io.File value) throws java.io.FileNotFoundException {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "this.requestBody = new " + ctx.qualifyCommonApiClass("FileBinaryPayload") + "(value);");
                ctx.push(clazz, "return this;");
                ctx.subIndent(clazz);
                ctx.push(clazz,"}");
                ctx.push(clazz, "");

                ctx.push(clazz, "public " + clazz + " withRequestBody(byte[] value, int off, int len) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "this.requestBody = new " + ctx.qualifyCommonApiClass("ByteArrayBinaryPayload") + "(value, off, len);");
                ctx.push(clazz, "return this;");
                ctx.subIndent(clazz);
                ctx.push(clazz,"}");
                ctx.push(clazz, "");

                ctx.push(clazz, "public " + clazz + " withRequestBody(byte[] value) {");
                ctx.addIndent(clazz);
                ctx.push(clazz, "return this.withRequestBody(value, 0, value.length);");
                ctx.subIndent(clazz);
                ctx.push(clazz,"}");
                ctx.push(clazz, "");
            }
        }

        MemberGenerator.generateTrivialMember(ctx, clazz, "requestTimeout", "requestTimeout", name, "java.time.Duration", null);
        MemberGenerator.generateTrivialMember(ctx, clazz, "responseBodyReadTimeout", "responseBodyReadTimeout", name, "java.time.Duration", null);
        MemberGenerator.generateTrivialMember(ctx, clazz, "responseBodyTotalTimeout", "responseBodyTotalTimeout", name, "java.time.Duration", null);

        MemberGenerator.generateMapMember(ctx, clazz, "additionalHeaderParameter", "additionalHeaderParameter", name, "java.util.List<String>", null);
        MemberGenerator.generateMapMember(ctx, clazz, "additionalQueryParameter", "additionalQueryParameter", name, "java.util.List<String>", null);

        List<String> toStringMembers = mangledNames.stream().filter(a -> !"requestBody".equals(a) || mimeType != null).collect(Collectors.toList());

        Util.generateHashCodeEquals(ctx, clazz, toStringMembers);
        Util.generateToString(ctx, clazz, name, toStringMembers);

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");

        return clazz;
    }

    public static String getParameterMemberTypeName(GenerationContext ctx, PathModel model, ParameterModel pm, SchemaClassification paramClazz) {
        if (Util.getEnumRecursive(pm.getSchema()) != null) {
            throw new IllegalStateException("Not implemented yet enum for path/query/header parameter");
        }

        switch (paramClazz) {
            case ARRAY_MAP_OBJECT_REF:
                //TODO
            case ARRAY_OBJECT_REF:
                //TODO
            case MAP_OBJECT_REF:
                //TODO
            case MAP_ARRAY_OBJECT_REF:
                //TODO
            case REF:
                //TODO
                throw new UnsupportedOperationException("NOT YET SUPPORTED!");
        }

        return Util.findRecursiveTypeName(ctx, pm.getSchema(), null);
    }
}
