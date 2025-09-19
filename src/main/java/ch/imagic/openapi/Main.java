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
import ch.imagic.openapi.model.*;

import java.io.File;
import java.util.*;

public class Main  {

    private static String getMandatoryVariable(String name) {
        String val = System.getProperty(name);
        if (val == null || val.isEmpty()) {
            val = System.getenv(name);
        }

        if (val == null || val.isEmpty()) {
            System.err.println("Missing mandatory env variable " + name);
            printUsageAndExit();
        }

        return val;
    }


    private static String getOptionalVariable(String name, String defaultValue) {
        String val = System.getProperty(name);
        if (val == null || val.isEmpty()) {
            val = System.getenv(name);
        }

        if (val == null || val.isEmpty()) {
            return defaultValue;
        }

        return val;
    }

    private static void printUsageAndExit() {
        System.out.println();
        System.out.println("Mandatory env variables: ");
        System.out.println("\tSCHEMA: the path to the openapi schema file");
        System.out.println("\tPACKAGE: the java package name for the generated sources");
        System.out.println();
        System.out.println("Output env variables, one combination is mandatory: ");
        System.out.println("\tSOURCE_TARGET_DIR: the path to the directory where the generated sources will be written");
        System.out.println("If SOURCE_TARGET_DIR is not set, the following variables become mandatory, otherwise they will fallback to SOURCE_TARGET_DIR.");
        System.out.println("\tIMPL_SOURCE_TARGET_DIR: the path to the directory where the generated implementation sources will be written");
        System.out.println("\tAPI_SOURCE_TARGET_DIR: the path to the directory where the generated api sources will be written");
        System.out.println("\tCOMMON_IMPL_SOURCE_TARGET_DIR: the path to the directory where the generated common implementation sources will be written");
        System.out.println("\tCOMMON_API_SOURCE_TARGET_DIR: the path to the directory where the generated common implementation sources will be written");
        System.out.println();
        System.out.println("Optional env variables: ");
        System.out.println("\tCOMMON_PACKAGE: the java package name for the generated common sources.\n\t\tThese sources do not depend on your schema and can be reused in multiple generations, falls back to PACKAGE_NAME if not set.");
        System.out.println("\tMODEL_SUFFIX: Suffix for generated model classes, defaults to empty string.");
        System.out.println("\tTAG_SUFFIX: Suffix for interfaces generated for each tag, defaults to 'Api'.");
        System.out.println("\tRESPONSE_SUFFIX: Suffix for generated response classes, defaults to 'Response'.");
        System.out.println("\tREQUEST_SUFFIX: Suffix for generated request classes, defaults to 'Request'.");
        System.out.println("\tINTERFACE_OPERATION_DEFINITION_SUFFIX: operation method suffix between ')' and ; in the interface definition.\n\t\tAllows for adding 'throws MyCustomException', useful for java reflection proxys. Defaults to empty string");
        System.out.println("\tGSON: defaults to true, if set to false no gson annotations will be generated.");
        System.out.println("\tJACKSON: defaults to true, if set to false no jackson annotations will be generated.");
        System.out.println("\tJSR380: defaults to true, if set to false no jakarta.validation annotations will be generated.");
        System.out.println();
        System.out.println("Extension env variables (optional): ");
        System.out.println("\tEXTENSION_SCHEMA: path to the extension schema file. This file is a full openapi schema.");
        System.out.println("if EXTENSION_SCHEMA is not set the following variables are ignored:");
        System.out.println("\tEXTENSION_PACKAGE: package name for the extension schema. Mandatory if EXTENSION_SCHEMA is set.");
        System.out.println("\tEXTENSION_IMPL_SOURCE_TARGET_DIR: directory for the extension implementation sources. Defaults to IMPL_SOURCE_TARGET_DIR or SOURCE_TARGET_DIR.");
        System.out.println("\tEXTENSION_API_SOURCE_TARGET_DIR: directory for the extension api sources. Defaults to API_SOURCE_TARGET_DIR or SOURCE_TARGET_DIR.");
        System.out.println("\tEXTENSION_MODEL_SUFFIX: Suffix for generated model classes, defaults to empty string.");
        System.out.println("\tEXTENSION_TAG_SUFFIX: Suffix for interfaces generated for each tag, defaults to 'Api'.");
        System.out.println("\tEXTENSION_RESPONSE_SUFFIX: Suffix for generated response classes, defaults to 'Response'.");
        System.out.println("\tEXTENSION_REQUEST_SUFFIX: Suffix for generated response classes, defaults to 'Request'.");
        System.out.println("\tEXTENSION_OPERATION_SUFFIX: Suffix for extended operations, defaults to 'Extended'.");
        System.out.println();
        System.out.println("Simple Example: ");
        System.out.println("export SCHEMA=petstore.json");
        System.out.println("export PACKAGE=org.example.petstore");
        System.out.println("export SOURCE_TARGET_DIR=src/main/java");
        System.out.println("java -jar robust-openapi-generator.jar");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        String allSources = getOptionalVariable("SOURCE_TARGET_DIR", null);
        String implSource = allSources != null ?
                getOptionalVariable("IMPL_SOURCE_TARGET_DIR", allSources) : getMandatoryVariable("IMPL_SOURCE_TARGET_DIR");
        String apiSource = allSources != null ?
                getOptionalVariable("API_SOURCE_TARGET_DIR", allSources) : getMandatoryVariable("API_SOURCE_TARGET_DIR");
        String commonImplSource = allSources != null ?
                getOptionalVariable("COMMON_IMPL_SOURCE_TARGET_DIR", allSources) : getMandatoryVariable("COMMON_IMPL_SOURCE_TARGET_DIR");
        String commonApiSource = allSources != null ?
                getOptionalVariable("COMMON_API_SOURCE_TARGET_DIR", allSources) : getMandatoryVariable("COMMON_API_SOURCE_TARGET_DIR");

        String schema = getMandatoryVariable("SCHEMA");
        String packageName = getMandatoryVariable("PACKAGE");
        String commonPackageName = getOptionalVariable("COMMON_PACKAGE", packageName + ".common");

        String modelSuffix = getOptionalVariable("MODEL_SUFFIX", "");
        String tagSuffix = getOptionalVariable("TAG_SUFFIX", "Api");
        String responseSuffix = getOptionalVariable("RESPONSE_SUFFIX", "Response");
        String requestSuffix = getOptionalVariable("REQUEST_SUFFIX", "Request");
        String interfaceSuffix = getOptionalVariable("INTERFACE_OPERATION_DEFINITION_SUFFIX", "");

        boolean jsr380 = !"false".equalsIgnoreCase(getOptionalVariable("JSR380", "true"));
        boolean gson = !"false".equalsIgnoreCase(getOptionalVariable("GSON", "true"));
        boolean jackson = !"false".equalsIgnoreCase(getOptionalVariable("JACKSON", "true"));

        String extensionSchema = getOptionalVariable("EXTENSION_SCHEMA", null);
        String extensionPackage = getOptionalVariable("EXTENSION_PACKAGE", null);
        if (extensionPackage == null) {
            System.err.println("EXTENSION_PACKAGE must be set if EXTENSION_SCHEMA is set!");
            printUsageAndExit();
        }
        String extensionImplSource = getOptionalVariable("EXTENSION_IMPL_SOURCE_TARGET_DIR", implSource);
        String extensionApiSource = getOptionalVariable("EXTENSION_API_SOURCE_TARGET_DIR", apiSource);
        String extensionModelSuffix = getOptionalVariable("EXTENSION_MODEL_SUFFIX", "");
        String extensionTagSuffix = getOptionalVariable("EXTENSION_TAG_SUFFIX", "Api");
        String extensionResponseSuffix = getOptionalVariable("EXTENSION_RESPONSE_SUFFIX", "Response");
        String extensionRequestSuffix = getOptionalVariable("EXTENSION_REQUEST_SUFFIX", "Request");
        String extensionOperationSuffix = getOptionalVariable("EXTENSION_OPERATION_SUFFIX", "Extended");

        OpenApiGeneratorConfig config = new OpenApiGeneratorConfig();
        config.setSchema(new File(schema));
        config.setImplSourceTargetDir(new File(implSource));
        config.setApiSourceTargetDir(new File(apiSource));
        config.setCommonImplSourceTargetDir(new File(commonImplSource));
        config.setCommonApiSourceTargetDir(new File(commonApiSource));
        config.setPackageName(packageName);
        config.setCommonPackageName(commonPackageName);
        config.setModelSuffix(modelSuffix);
        config.setTagSuffix(tagSuffix);
        config.setRequestSuffix(requestSuffix);
        config.setResponseSuffix(responseSuffix);
        config.setInterfaceSuffix(interfaceSuffix);
        config.setJsr380(jsr380);
        config.setGson(gson);
        config.setJackson(jackson);

        if (extensionSchema != null) {
            config.setExtensionSchema(new File(extensionSchema));
            config.setExtensionPackage(extensionPackage);
            config.setExtensionImplSource(new File(extensionImplSource));
            config.setExtensionApiSource(new File(extensionApiSource));
            config.setExtensionModelSuffix(extensionModelSuffix);
            config.setExtensionTagSuffix(extensionTagSuffix);
            config.setExtensionRequestSuffix(extensionRequestSuffix);
            config.setExtensionResponseSuffix(extensionResponseSuffix);
            config.setExtensionOperationSuffix(extensionOperationSuffix);
        }

        OpenApiGenerator.generate(config);
    }








}