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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerationContext {

    private final Map<String, StringBuilder> generated = new HashMap<>();
    private final Map<String, Integer> indentMap = new HashMap<>();
    private final Map<String, Set<String>> interfacesForModelObjects = new HashMap<>();

    private RootModel model;
    private String packageName;
    private String commonPackageName;
    private String primaryInterfaceName;
    private String modelSuffix;
    private String tagSuffix;
    private String requestSuffix;
    private String responseSuffix;
    private boolean isApiExceptionRuntimeException;

    private long opIdCounter;

    private long typeCounter;

    private String extensionOperationSuffix = "";

    private File commonApiSourceRoot;
    private File commonImplSourceRoot;

    private File apiSourceRoot;
    private File implSourceRoot;

    private String operationInterfaceSuffix;

    private Set<String> extensionMethodIds = new HashSet<>();

    private Set<String> modelClassNamesMangle = new HashSet<>();
    private Map<String, String> modelClassNameMapping = new HashMap<>();
    private Map<String, Long> typeInfoMapping = new HashMap<>();

    private boolean jsr380 = false;
    private boolean gson = false;
    private boolean jackson = false;

    private Map<String, Map<Integer, String>> managledRequestParameterNames = new HashMap<>();

    public GenerationContext(RootModel model) {
        this.model = model;
    }

    public boolean isApiExceptionRuntimeException() {
        return isApiExceptionRuntimeException;
    }

    public void setApiExceptionRuntimeException(boolean apiExceptionRuntimeException) {
        isApiExceptionRuntimeException = apiExceptionRuntimeException;
    }

    public long nextOpId() {
        return opIdCounter++;
    }

    public long nextType() {
        return typeCounter++;
    }

    public File getSourceDirForClass(String clazzName) {
        File dir = Objects.requireNonNull(this.getImplSourceRoot(), "implSourceRoot must not be null");
        if (clazzName.startsWith(this.getModelPackage()) || clazzName.startsWith(this.getReqParamPackage()) || clazzName.startsWith(this.getResponsePackage()) || clazzName.startsWith(this.getTagPackage())) {
            dir = Objects.requireNonNull(this.getApiSourceRoot(), "apiSourceRoot must not be null");
        }

        if (clazzName.startsWith(this.getCommonImplPackageName())) {
            dir = Objects.requireNonNull(this.getCommonImplSourceRoot(), "commonImplSourceRoot must not be null");
        }

        if (clazzName.startsWith(this.getCommonApiPackageName())) {
            dir = Objects.requireNonNull(this.getCommonApiSourceRoot(), "commonApiSourceRoot must not be null");
        }
        return dir;
    }

    public void setJsr380(boolean jsr380) {
        this.jsr380 = jsr380;
    }

    public void setGson(boolean gson) {
        this.gson = gson;
    }

    public void setJackson(boolean jackson) {
        this.jackson = jackson;
    }

    public File getCommonApiSourceRoot() {
        return commonApiSourceRoot;
    }

    public void setCommonApiSourceRoot(File commonApiSourceRoot) {
        this.commonApiSourceRoot = commonApiSourceRoot;
    }

    public File getCommonImplSourceRoot() {
        return commonImplSourceRoot;
    }

    public void setCommonImplSourceRoot(File commonImplSourceRoot) {
        this.commonImplSourceRoot = commonImplSourceRoot;
    }

    public File getApiSourceRoot() {
        return apiSourceRoot;
    }

    public void setApiSourceRoot(File apiSourceRoot) {
        this.apiSourceRoot = apiSourceRoot;
    }

    public File getImplSourceRoot() {
        return implSourceRoot;
    }

    public void setImplSourceRoot(File implSourceRoot) {
        this.implSourceRoot = implSourceRoot;
    }

    public String getOperationInterfaceSuffix() {
        return operationInterfaceSuffix;
    }

    public void setOperationInterfaceSuffix(String operationInterfaceSuffix) {
        this.operationInterfaceSuffix = operationInterfaceSuffix;
    }

    public void push(String file, String line1, String... content) {
        StringBuilder current = generated.get(file);
        if (current == null) {
            current = new StringBuilder("//THIS FILE IS MACHINE GENERATED DO NOT EDIT\n");
            generated.put(file, current);
        }
        List<String> ar = new ArrayList<>();
        ar.add(line1);
        ar.addAll(Arrays.asList(content));
        for (String line : ar) {
            int indent = indentMap.getOrDefault(file, 0);
            while(indent > 0) {
                indent--;
                current.append("    ");
            }

            current.append(line).append("\n");
        }
    }

    private void addCommonApiFile(String name, String resource) {
        this.generated.put(qualifyCommonApiClass(name), new StringBuilder("package " + getCommonApiPackageName() +";\n" + Util.readResource("/togen/"+resource+".java")));
    }

    private void addCommonImplFile(String name, String resource) {
        this.generated.put(qualifyCommonImplClass(name), new StringBuilder("package " + getCommonImplPackageName() +";\nimport "+getCommonApiPackageName()+".*;\n" + Util.readResource("/togen/"+resource+".java")));
    }

    public void addCommonFiles() {
        if (isApiExceptionRuntimeException) {
            addCommonApiFile("ApiException", "ApiException");
        } else {
            addCommonApiFile("ApiException", "ApiException2");
        }

        addCommonApiFile("BinaryPayload", "BinaryPayload");
        addCommonApiFile("ByteArrayBinaryPayload", "ByteArrayBinaryPayload");
        addCommonApiFile("FieldName", "FieldName");
        addCommonApiFile("FileBinaryPayload", "FileBinaryPayload");
        addCommonApiFile("InputStreamBinaryPayload", "InputStreamBinaryPayload");
        addCommonApiFile("OneOf", "OneOf");
        addCommonApiFile("RequestParameters", "RequestParameters");
        addCommonApiFile("Response", "Response");
        addCommonApiFile("ToString", "ToString");
        addCommonApiFile("Visitable", "Visitable");
        addCommonApiFile("PropertyVisitor", "PropertyVisitor");

        addCommonImplFile("ApiClient", "ApiClient");
        addCommonImplFile("InputStreamBinaryPayloadWithContentLength", "InputStreamBinaryPayloadWithContentLength");
        addCommonImplFile("PrefixedInputStreamBinaryPayload", "PrefixedInputStreamBinaryPayload");
        addCommonImplFile("RequestContext", "RequestContext");
        addCommonImplFile("RequestMetadata", "RequestMetadata");
        addCommonImplFile("HttpResponseInputStream", "HttpResponseInputStream");
        addCommonImplFile("TimeoutBodyPublisher", "TimeoutBodyPublisher");
    }

    public void addIndent(String file) {
        indentMap.put(file, indentMap.getOrDefault(file, 0)+1);
    }

    public void subIndent(String file) {
        int cnt = indentMap.getOrDefault(file, 0);
        if (cnt == 0) {
            throw new IllegalStateException("subIndent of " + file + " < 0");
        }

        cnt--;
        indentMap.put(file, cnt);
    }

    public Set<String> getInterfaces(String clazz) {
        return interfacesForModelObjects.getOrDefault(clazz, Collections.emptySet());
    }

    public Set<String> getImplsForInterface(String iface) {
        Set<String> r = new TreeSet<>();
        for (Map.Entry<String, Set<String>> e : interfacesForModelObjects.entrySet()) {
            if (e.getValue().contains(iface)) {
                r.add(e.getKey());
            }
        }

        return r;
    }

    public String getModelSuffix() {
        return modelSuffix;
    }

    public void setModelSuffix(String modelSuffix) {
        this.modelSuffix = modelSuffix;
    }

    public String getPrimaryInterfaceName() {
        return primaryInterfaceName;
    }

    public void setPrimaryInterfaceName(String primaryInterfaceName) {
        this.primaryInterfaceName = primaryInterfaceName;
    }

    public String getTagSuffix() {
        return tagSuffix;
    }

    public void setTagSuffix(String tagSuffix) {
        this.tagSuffix = tagSuffix;
    }

    public String getRequestSuffix() {
        return requestSuffix;
    }

    public void setRequestSuffix(String requestSuffix) {
        this.requestSuffix = requestSuffix;
    }

    public String getResponseSuffix() {
        return responseSuffix;
    }

    public void setResponseSuffix(String responseSuffix) {
        this.responseSuffix = responseSuffix;
    }

    public void addInterface(String clazz, String iface) {
        interfacesForModelObjects.computeIfAbsent(clazz, k -> new LinkedHashSet<>()).add(iface);
    }

    public String getExtensionOperationSuffix() {
        return extensionOperationSuffix;
    }

    public boolean isExtensionOperation(String operationId) {
        return extensionMethodIds.contains(operationId);
    }

    public void addExtensionOperation(String operationId) {
        extensionMethodIds.add(operationId);
    }

    public void setExtensionOperationSuffix(String extensionOperationSuffix) {
        this.extensionOperationSuffix = extensionOperationSuffix;
    }

    public void putMangledRequestParameterName(String clazz, int index, String mangle) {
        managledRequestParameterNames.computeIfAbsent(clazz,  k -> new HashMap<>()).put(index, mangle);
    }

    public String getMangledRequestParameterName(String clazz, int index) {
        return managledRequestParameterNames.computeIfAbsent(clazz,  k -> new HashMap<>()).get(index);
    }

    public Long getTypeInfo(String type) {
        return typeInfoMapping.get(type);
    }

    public void setTypeInfo(String type, Long value) {
        typeInfoMapping.put(type, value);
    }

    private Map<String, String> compatModels = new HashMap<>();
    private Set<String> compatModelValues = new HashSet<>();

    public void addCompatModel(String s, String s1) {
        compatModels.put(s, s1);
        compatModelValues.add(s1);
    }


    public boolean isCompatModel(String s) {
        return compatModelValues.contains(s);
    }

    private Map<String, String> compatResponse = new HashMap<>();
    private Set<String> compatResponseValues = new HashSet<>();

    public void addCompatResponse(String s, String s2) {
        compatResponse.put(s, s2);
        compatResponseValues.add(s2);
    }

    public boolean isCompatResponse(String s) {
        return compatResponseValues.contains(s);
    }



    private Map<String, String> compatRequest = new HashMap<>();
    private Set<String> compatRequestValues = new HashSet<>();

    public void addCompatRequest(String s, String s2) {
        compatRequest.put(s, s2);
        compatRequestValues.add(s2);
    }

    public boolean isCompatRequest(String s) {
        return compatRequestValues.contains(s);
    }

    public String modelNameToJavaClass(String name) {
        if (name.startsWith("#/components/schemas/")) {
            name = name.substring("#/components/schemas/".length());
        }

        String s = modelClassNameMapping.get(name);
        if (s != null) {
            return s;
        }

        String mangledName = Util.capitalize(name) + getModelSuffix();
        mangledName = Util.mangleName(mangledName);

        if (modelClassNamesMangle.add(mangledName)) {
            modelClassNameMapping.put(name, mangledName);
            return mangledName;
        }

        int cnt = -1;
        while(true) {
            cnt++;
            if (modelClassNamesMangle.add(mangledName + cnt)) {
                modelClassNameMapping.put(name, mangledName + cnt);
                return mangledName + cnt;
            }
        }
    }

    public Map<String, ? extends CharSequence> getGenerated() {
        return generated;
    }

    public RootModel getModel() {
        return model;
    }

    public void setModel(RootModel model) {
        this.model = model;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getModelPackage() {
        return packageName + ".api.model";
    }

    public String getTagPackage() {
        return packageName + ".api.tags";
    }

    public String getImplPackage() {
        return packageName + ".impl";
    }

    public String getCommonImplPackageName() {
        if (commonPackageName == null) {
            return getPackageName() + ".impl";
        }
        return commonPackageName + ".impl";
    }

    public String getCommonApiPackageName() {
        if (commonPackageName == null) {
            return getPackageName() + ".api";
        }
        return commonPackageName + ".api";
    }

    public String getReqParamPackage() {
        return packageName + ".api.request";
    }

    public String getResponsePackage() {
        return packageName + ".api.response";
    }

    public String qualifyModelClass(String modelClassName) {
        String result = qualifyModelClassInner(modelClassName);
        return compatModels.getOrDefault(result, result);
    }



    public String operationIdAndMimeToRequestClass(String operationId, String mimeType) {
        return Util.capitalize(operationId) + (mimeType == null ? "" : Util.mangleContentType(Util.capitalize(mimeType))) + getRequestSuffix();
    }

    public String qualifyRequestClass(String responseClassName) {
        if (responseClassName.startsWith(getReqParamPackage())) {
            return responseClassName;
        }

        String clazz = getReqParamPackage() + "." + responseClassName;

        return compatRequest.getOrDefault(clazz, clazz);
    }

    public String operationIdToResponseClass(String operationId) {
        return Util.capitalize(operationId) + getResponseSuffix();
    }

    public String qualifyResponseClass(String responseClassName) {
        if (responseClassName.startsWith(getResponsePackage())) {
            return responseClassName;
        }

        String clazz = getResponsePackage() + "." + responseClassName;

        return compatResponse.getOrDefault(clazz, clazz);
    }

    private String qualifyModelClassInner(String modelClassName) {
        if (modelClassName.startsWith("java.")) {
            throw new IllegalArgumentException("unlikely model class" + modelClassName);
        }
        if (modelClassName.startsWith(getModelPackage())) {
            return modelClassName;
        }

        return getModelPackage() + "." + modelClassName;
    }

    public String qualifyTagInterfaceClass(String modelClassName) {
        if (modelClassName.startsWith(getTagPackage())) {
            return modelClassName;
        }

        return getTagPackage() + "." + modelClassName;
    }

    public String qualifyImplClass(String modelClassName) {
        if (modelClassName.startsWith(getImplPackage())) {
            return modelClassName;
        }

        return getImplPackage() + "." + modelClassName;
    }

    public String qualifyCommonImplClass(String modelClassName) {
        if (modelClassName.startsWith(getCommonImplPackageName())) {
            return modelClassName;
        }

        return getCommonImplPackageName() + "." + modelClassName;
    }

    public String qualifyCommonApiClass(String modelClassName) {
        if (modelClassName.startsWith(getCommonApiPackageName())) {
            return modelClassName;
        }

        return getCommonApiPackageName() + "." + modelClassName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setCommonPackageName(String commonPackage) {
        this.commonPackageName = commonPackage;
    }

    public Set<String> getModelClassNamesMangle() {
        return modelClassNamesMangle;
    }

    public void setModelClassNamesMangle(Set<String> modelClassNamesMangle) {
        this.modelClassNamesMangle = modelClassNamesMangle;
    }

    public Map<String, String> getModelClassNameMapping() {
        return modelClassNameMapping;
    }

    public void setModelClassNameMapping(Map<String, String> modelClassNameMapping) {
        this.modelClassNameMapping = modelClassNameMapping;
    }

    public ParameterModel findParameter(String name) {
        if (getModel().getComponents() == null) {
            return null;
        }

        if (getModel().getComponents().getResponses() == null) {
            return null;
        }


        if (name.startsWith("#/components/parameters/")) {
            name = name.substring("#/components/parameters/".length());
        }

        return getModel().getComponents().getParameters().get(name);
    }

    public RequestBodyModel findRequestBody(String name) {
        if (getModel().getComponents() == null) {
            return null;
        }

        if (getModel().getComponents().getResponses() == null) {
            return null;
        }


        if (name.startsWith("#/components/requestBodies/")) {
            name = name.substring("#/components/requestBodies/".length());
        }

        return getModel().getComponents().getRequestBodies().get(name);
    }

    public ResponseModel findResponse(String name) {
        if (getModel().getComponents() == null) {
            return null;
        }

        if (getModel().getComponents().getResponses() == null) {
            return null;
        }


        if (name.startsWith("#/components/responses/")) {
            name = name.substring("#/components/responses/".length());
        }

        return getModel().getComponents().getResponses().get(name);
    }

    public SchemaModel findSchema(String name) {
        if (getModel().getComponents() == null) {
            return null;
        }

        if (getModel().getComponents().getSchemas() == null) {
            return null;
        }


        if (name.startsWith("#/components/schemas/")) {
            name = name.substring("#/components/schemas/".length());
        }

        return getModel().getComponents().getSchemas().get(name);
    }

    public HeaderModel findHeader(String name) {
        if (getModel().getComponents() == null) {
            return null;
        }

        if (getModel().getComponents().getSchemas() == null) {
            return null;
        }


        if (name.startsWith("#/components/headers/")) {
            name = name.substring("#/components/headers/".length());
        }

        return getModel().getComponents().getHeaders().get(name);
    }

    public boolean jackson() {
        return jackson;
    }

    public boolean gson() {
        return gson;
    }

    public boolean jsr380() {
        return jsr380;
    }

    public void writeGeneratedFilesToDisk() throws Exception {
        for (Map.Entry<String, ? extends CharSequence> e : this.getGenerated().entrySet()) {
            String clazzName = e.getKey();
            File dir = this.getSourceDirForClass(clazzName);

            String fileName = clazzName.replace('.', File.separatorChar) + ".java";
            File target = new File(dir, fileName);
            if (target.exists()) {
                target.delete();
            }

            target.getParentFile().mkdirs();
            try(FileOutputStream faos = new FileOutputStream(target, false)) {
                faos.write(e.getValue().toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}

