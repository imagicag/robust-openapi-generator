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

public class ExtensionPreProcessor {

    public static void processExtension(GenerationContext base, GenerationContext extension) {
        Set<String> compatibleModels = new HashSet<>();
        Set<String> incompatibleModels = new HashSet<>();
        outer: for (Map.Entry<String, SchemaModel> modelKv : extension.getModel().getComponents().getSchemas().entrySet()) {
            String modelName = "#/components/schemas/" + modelKv.getKey();
            if (compatibleModels.contains(modelName) || incompatibleModels.contains(modelName)) {
                continue;
            }
            SchemaModel model = modelKv.getValue();
            Set<String> dependants = Util.getContainingTypeModelNames(extension, model, modelName);
            for (String dependant : dependants) {
                if (compatibleModels.contains(dependant)) {
                    continue;
                }

                if (incompatibleModels.contains(dependant)) {
                    incompatibleModels.add(modelName);
                    continue outer;
                }

                SchemaModel baseModel = base.findSchema(dependant);
                SchemaModel extModel = extension.findSchema(dependant);
                if (baseModel == null || extModel == null) {
                    incompatibleModels.add(modelName);
                    continue outer;
                }
                if (!baseModel.equals(extModel)) {
                    incompatibleModels.add(modelName);
                    continue outer;
                }
            }

            compatibleModels.add(modelName);
        }

        System.out.println("INFO: Found " + compatibleModels.size() + " compatible models and " + incompatibleModels.size() + " incompatible models");

        for (String s : compatibleModels) {
            extension.addCompatModel(extension.qualifyModelClass(extension.modelNameToJavaClass(s)), base.qualifyModelClass(base.modelNameToJavaClass(s)));
        }

        Set<String> comptaibleResponse = new HashSet<>();
        Set<String> incomptaibleResponse = new HashSet<>();
        for (Map.Entry<String, Map<String, PathModel>> pathKv : extension.getModel().getPaths().entrySet()) {
            outer: for (Map.Entry<String, PathModel> methodKv : pathKv.getValue().entrySet()) {
                String path = pathKv.getKey();
                String method = methodKv.getKey();
                PathModel extModel = methodKv.getValue();

                String name = extModel.getOperationId();


                Map<String, ResponseModel> responses = extModel.getResponses();

                PathModel pathModel = base.getModel().getPaths().getOrDefault(path, Collections.emptyMap()).get(method);

                if (pathModel == null) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleResponse.add(name);
                    continue;
                }

                if (!extModel.getOperationId().equals(pathModel.getOperationId())) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleResponse.add(name);
                    continue;
                }

                if (!Objects.equals(pathModel.getResponses(), responses)) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleResponse.add(name);
                    continue;
                }

                for (ResponseModel response : responses.values()) {
                    ResponseModel extensionResponse = extension.findResponse(response.get$ref());
                    ResponseModel baseResponse = base.findResponse(response.get$ref());
                    if (!Objects.equals(extensionResponse, baseResponse)) {
                        extension.addExtensionOperation(extModel.getOperationId());
                        incomptaibleResponse.add(name);
                        continue outer;
                    }

                    if (extensionResponse.getHeaders() != null) {
                        for (HeaderModel h : extensionResponse.getHeaders().values()) {
                            HeaderModel extHeader = extension.findHeader(h.get$ref());
                            HeaderModel baseHeader = base.findHeader(h.get$ref());
                            if (!Objects.equals(extHeader, baseHeader)) {
                                extension.addExtensionOperation(extModel.getOperationId());
                                incomptaibleResponse.add(name);
                                continue outer;
                            }

                            Set<String> types = Util.getContainingTypeModelNames(extension, extHeader.getSchema(), null);
                            if (!compatibleModels.containsAll(types)) {
                                extension.addExtensionOperation(extModel.getOperationId());
                                incomptaibleResponse.add(name);
                                continue outer;
                            }
                        }
                    }

                    if (extensionResponse.getContent() == null) {
                        continue;
                    }

                    for (PathSchemaModel psm : extensionResponse.getContent().values()) {
                        Set<String> types = Util.getContainingTypeModelNames(extension, psm.getSchema(), null);
                        if (!compatibleModels.containsAll(types)) {
                            extension.addExtensionOperation(extModel.getOperationId());
                            incomptaibleResponse.add(name);
                            continue outer;
                        }
                    }
                }

                comptaibleResponse.add(name);

            }
        }

        for (String s : comptaibleResponse) {
            extension.addCompatResponse(extension.qualifyResponseClass(extension.operationIdToResponseClass(s)), base.qualifyResponseClass(base.operationIdToResponseClass(s)));
        }

        System.out.println("INFO: Found " + comptaibleResponse.size() + " compatible responses and " + incomptaibleResponse.size() + " incompatible responses");

        Map<String, Set<String>> comptaibleRequests = new HashMap<>();
        Set<String> incomptaibleRequests = new HashSet<>();
        for (Map.Entry<String, Map<String, PathModel>> pathKv : extension.getModel().getPaths().entrySet()) {
            outer: for (Map.Entry<String, PathModel> methodKv : pathKv.getValue().entrySet()) {
                String path = pathKv.getKey();
                String method = methodKv.getKey();
                PathModel extModel = methodKv.getValue();

                String name = extModel.getOperationId();

                PathModel baseModel = base.getModel().getPaths().getOrDefault(path, Collections.emptyMap()).get(method);

                if (baseModel == null) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleRequests.add(name);
                    continue;
                }

                if (!extModel.getOperationId().equals(baseModel.getOperationId())) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleRequests.add(name);
                    continue;
                }

                if (!Arrays.equals(extModel.getParameters(), baseModel.getParameters())) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleRequests.add(name);
                    continue;
                }

                if (extModel.getParameters() != null) {
                    for (ParameterModel p : extModel.getParameters()) {
                        ParameterModel extParam = extension.findParameter(p.get$ref());
                        ParameterModel baseParam = base.findParameter(p.get$ref());
                        if (!Objects.equals(extParam, baseParam)) {
                            extension.addExtensionOperation(extModel.getOperationId());
                            incomptaibleRequests.add(name);
                            continue outer;
                        }

                        Set<String> types = Util.getContainingTypeModelNames(extension, extParam.getSchema(), null);
                        if (!compatibleModels.containsAll(types)) {
                            extension.addExtensionOperation(extModel.getOperationId());
                            incomptaibleRequests.add(name);
                            continue outer;
                        }
                    }
                }

                if (!Objects.equals(extModel.getRequestBody(), baseModel.getRequestBody())) {
                    extension.addExtensionOperation(extModel.getOperationId());
                    incomptaibleRequests.add(name);
                    continue;
                }

                Set<String> mimeTypes = new HashSet<>();

                if (extModel.getRequestBody() != null) {
                    RequestBodyModel baseRequestBody = base.findRequestBody(extModel.getRequestBody().get$ref());
                    RequestBodyModel extRequestBody = extension.findRequestBody(extModel.getRequestBody().get$ref());
                    if (!Objects.equals(baseRequestBody, extRequestBody)) {
                        extension.addExtensionOperation(extModel.getOperationId());
                        incomptaibleRequests.add(name);
                        continue;
                    }

                    for (Map.Entry<String, PathSchemaModel> bodyKv : extRequestBody.getContent().entrySet()) {
                        String contentType = bodyKv.getKey();
                        mimeTypes.add(contentType);
                        PathSchemaModel extBodySchema = bodyKv.getValue();
                        if (!contentType.equals("application/json")) {
                            continue;
                        }
                        Set<String> containers = Util.getContainingTypeModelNames(extension, extBodySchema.getSchema(), null);
                        if (!compatibleModels.containsAll(containers)) {
                            extension.addExtensionOperation(extModel.getOperationId());
                            incomptaibleRequests.add(name);
                            continue outer;
                        }
                    }
                }

                comptaibleRequests.put(name, mimeTypes);
            }
        }

        for (Map.Entry<String, Set<String>> opIdToMime : comptaibleRequests.entrySet()) {
            if (opIdToMime.getValue().isEmpty()) {
                extension.addCompatRequest(extension.qualifyRequestClass(extension.operationIdAndMimeToRequestClass(opIdToMime.getKey(), null)), base.qualifyRequestClass(base.operationIdAndMimeToRequestClass(opIdToMime.getKey(), null)));
                continue;
            }

            for (String mime : opIdToMime.getValue()) {
                extension.addCompatRequest(extension.qualifyRequestClass(extension.operationIdAndMimeToRequestClass(opIdToMime.getKey(), mime)), base.qualifyRequestClass(base.operationIdAndMimeToRequestClass(opIdToMime.getKey(), mime)));
            }
        }

        System.out.println("INFO: Found " + comptaibleRequests.size() + " compatible requests and " + incomptaibleRequests.size() + " incompatible requests");
    }
}
