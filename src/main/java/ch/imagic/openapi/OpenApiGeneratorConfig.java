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

import java.io.File;

public class OpenApiGeneratorConfig {

    private File schema;
    private File implSourceTargetDir;
    private File apiSourceTargetDir;
    private File commonImplSourceTargetDir;
    private File commonApiSourceTargetDir;
    private String packageName;
    private String commonPackageName;

    private String modelSuffix = "";
    private String tagSuffix = "Api";
    private String responseSuffix = "Response";
    private String requestSuffix = "Request";
    private String interfaceSuffix = "";

    private File extensionSchema;
    private File extensionImplSource;
    private File extensionApiSource;
    private String extensionPackage;
    private String extensionModelSuffix;
    private String extensionTagSuffix;
    private String extensionResponseSuffix;
    private String extensionRequestSuffix;
    private String extensionOperationSuffix;

    private boolean jsr380 = false;
    private boolean gson = false;
    private boolean jackson = false;

    private boolean isApiExceptionRuntimeException;

    public boolean isApiExceptionRuntimeException() {
        return isApiExceptionRuntimeException;
    }

    public void setApiExceptionRuntimeException(boolean apiExceptionRuntimeException) {
        isApiExceptionRuntimeException = apiExceptionRuntimeException;
    }

    public File getSchema() {
        return schema;
    }

    public void setSchema(File schema) {
        this.schema = schema;
    }

    public File getImplSourceTargetDir() {
        return implSourceTargetDir;
    }

    public void setImplSourceTargetDir(File implSourceTargetDir) {
        this.implSourceTargetDir = implSourceTargetDir;
    }

    public File getApiSourceTargetDir() {
        return apiSourceTargetDir;
    }

    public void setApiSourceTargetDir(File apiSourceTargetDir) {
        this.apiSourceTargetDir = apiSourceTargetDir;
    }

    public File getCommonImplSourceTargetDir() {
        return commonImplSourceTargetDir;
    }

    public void setCommonImplSourceTargetDir(File commonImplSourceTargetDir) {
        this.commonImplSourceTargetDir = commonImplSourceTargetDir;
    }

    public File getCommonApiSourceTargetDir() {
        return commonApiSourceTargetDir;
    }

    public void setCommonApiSourceTargetDir(File commonApiSourceTargetDir) {
        this.commonApiSourceTargetDir = commonApiSourceTargetDir;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getCommonPackageName() {
        return commonPackageName;
    }

    public void setCommonPackageName(String commonPackageName) {
        this.commonPackageName = commonPackageName;
    }

    public String getModelSuffix() {
        return modelSuffix;
    }

    public void setModelSuffix(String modelSuffix) {
        this.modelSuffix = modelSuffix;
    }

    public String getTagSuffix() {
        return tagSuffix;
    }

    public void setTagSuffix(String tagSuffix) {
        this.tagSuffix = tagSuffix;
    }

    public String getResponseSuffix() {
        return responseSuffix;
    }

    public void setResponseSuffix(String responseSuffix) {
        this.responseSuffix = responseSuffix;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public void setRequestSuffix(String requestSuffix) {
        this.requestSuffix = requestSuffix;
    }

    public String getInterfaceSuffix() {
        return interfaceSuffix;
    }

    public void setInterfaceSuffix(String interfaceSuffix) {
        this.interfaceSuffix = interfaceSuffix;
    }

    public File getExtensionSchema() {
        return extensionSchema;
    }

    public void setExtensionSchema(File extensionSchema) {
        this.extensionSchema = extensionSchema;
    }

    public String getExtensionPackage() {
        return extensionPackage;
    }

    public void setExtensionPackage(String extensionPackage) {
        this.extensionPackage = extensionPackage;
    }

    public File getExtensionImplSource() {
        return extensionImplSource;
    }

    public void setExtensionImplSource(File extensionImplSource) {
        this.extensionImplSource = extensionImplSource;
    }

    public File getExtensionApiSource() {
        return extensionApiSource;
    }

    public void setExtensionApiSource(File extensionApiSource) {
        this.extensionApiSource = extensionApiSource;
    }

    public String getExtensionModelSuffix() {
        return extensionModelSuffix;
    }

    public void setExtensionModelSuffix(String extensionModelSuffix) {
        this.extensionModelSuffix = extensionModelSuffix;
    }

    public String getExtensionTagSuffix() {
        return extensionTagSuffix;
    }

    public void setExtensionTagSuffix(String extensionTagSuffix) {
        this.extensionTagSuffix = extensionTagSuffix;
    }

    public String getExtensionResponseSuffix() {
        return extensionResponseSuffix;
    }

    public void setExtensionResponseSuffix(String extensionResponseSuffix) {
        this.extensionResponseSuffix = extensionResponseSuffix;
    }

    public String getExtensionRequestSuffix() {
        return extensionRequestSuffix;
    }

    public void setExtensionRequestSuffix(String extensionRequestSuffix) {
        this.extensionRequestSuffix = extensionRequestSuffix;
    }

    public String getExtensionOperationSuffix() {
        return extensionOperationSuffix;
    }

    public void setExtensionOperationSuffix(String extensionOperationSuffix) {
        this.extensionOperationSuffix = extensionOperationSuffix;
    }

    public boolean isJsr380() {
        return jsr380;
    }

    public void setJsr380(boolean jsr380) {
        this.jsr380 = jsr380;
    }

    public boolean isGson() {
        return gson;
    }

    public void setGson(boolean gson) {
        this.gson = gson;
    }

    public boolean isJackson() {
        return jackson;
    }

    public void setJackson(boolean jackson) {
        this.jackson = jackson;
    }
}
