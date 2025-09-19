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
package ch.imagic.openapi;

import ch.imagic.openapi.misc.*;
import ch.imagic.openapi.model.RootModel;
import com.google.gson.Gson;

import java.nio.file.Files;

public class OpenApiGenerator {

    private static void validateConfig(OpenApiGeneratorConfig cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("config must not be null");
        }

        if (cfg.getSchema() == null) {
            throw new IllegalArgumentException("schema must not be null");
        }

        if (cfg.getApiSourceTargetDir() == null) {
            throw new IllegalArgumentException("apiSourceTargetDir must not be null");
        }

        if (cfg.getImplSourceTargetDir() == null) {
            throw new IllegalArgumentException("implSourceTargetDir must not be null");
        }

        if (cfg.getCommonApiSourceTargetDir() == null) {
            throw new IllegalArgumentException("commonApiSourceTargetDir must not be null");
        }

        if (cfg.getCommonImplSourceTargetDir() == null) {
            throw new IllegalArgumentException("commonImplSourceTargetDir must not be null");
        }

        if (cfg.getPackageName() == null) {
            throw new IllegalArgumentException("packageName must not be null");
        }

        if (cfg.getCommonPackageName() == null) {
            throw new IllegalArgumentException("commonPackageName must not be null");
        }

        if (cfg.getInterfaceSuffix() == null) {
            throw new IllegalArgumentException("interfaceSuffix must not be null");
        }

        if (cfg.getExtensionSchema() != null) {
            if (cfg.getExtensionApiSource() == null) {
                throw new IllegalArgumentException("extensionApiSource must not be null");
            }

            if (cfg.getExtensionImplSource() == null) {
                throw new IllegalArgumentException("extensionImplSource must not be null");
            }

            if (cfg.getExtensionPackage() == null) {
                throw new IllegalArgumentException("extensionPackage must not be null");
            }

            if (cfg.getExtensionOperationSuffix() == null) {
                throw new IllegalArgumentException("extensionOperationSuffix must not be null");
            }
        }
    }

    public static void generate(OpenApiGeneratorConfig config) throws Exception {
        validateConfig(config);
        Gson gson = new Gson();
        String base = new String(Files.readAllBytes(config.getSchema().toPath()));
        RootModel rootModel = gson.fromJson(base, RootModel.class);

        GenerationContext context = new GenerationContext(rootModel);

        context.setJackson(config.isJackson());
        context.setJsr380(config.isJsr380());
        context.setGson(config.isGson());

        context.setImplSourceRoot(config.getImplSourceTargetDir());
        context.setApiSourceRoot(config.getApiSourceTargetDir());
        context.setCommonApiSourceRoot(config.getCommonApiSourceTargetDir());
        context.setCommonImplSourceRoot(config.getCommonImplSourceTargetDir());
        context.setOperationInterfaceSuffix(config.getInterfaceSuffix());
        context.setModelSuffix(config.getModelSuffix());
        context.setTagSuffix(config.getTagSuffix());
        context.setResponseSuffix(config.getResponseSuffix());
        context.setRequestSuffix(config.getRequestSuffix());

        context.setPackageName(config.getPackageName());
        context.setCommonPackageName(config.getCommonPackageName());

        context.addCommonFiles();

        SchemaPreProcessors.preProcess(context);
        DataModelGenerator.generateModels(context);
        OperationGenerator.generateApis(context);

        if (config.getExtensionSchema() != null) {
            String ext = new String(Files.readAllBytes(config.getExtensionSchema().toPath()));
            GenerationContext extContext = new GenerationContext(gson.fromJson(ext, RootModel.class));
            extContext.setImplSourceRoot(config.getExtensionImplSource());
            extContext.setApiSourceRoot(config.getExtensionApiSource());
            extContext.setCommonApiSourceRoot(config.getCommonApiSourceTargetDir());
            extContext.setCommonImplSourceRoot(config.getCommonImplSourceTargetDir());

            extContext.setPackageName(config.getExtensionPackage());
            extContext.setCommonPackageName(config.getCommonPackageName());
            extContext.setModelSuffix(config.getExtensionModelSuffix());
            extContext.setTagSuffix(config.getExtensionTagSuffix());
            extContext.setResponseSuffix(config.getExtensionResponseSuffix());
            extContext.setRequestSuffix(config.getExtensionRequestSuffix());
            extContext.setExtensionOperationSuffix(config.getExtensionOperationSuffix());
            extContext.setOperationInterfaceSuffix(config.getInterfaceSuffix());

            extContext.addCommonFiles();

            SchemaPreProcessors.preProcess(extContext);

            ExtensionPreProcessor.processExtension(context, extContext);

            DataModelGenerator.generateModels(extContext);
            OperationGenerator.generateApis(extContext);

            extContext.writeGeneratedFilesToDisk();
        }

        context.writeGeneratedFilesToDisk();
    }
}
