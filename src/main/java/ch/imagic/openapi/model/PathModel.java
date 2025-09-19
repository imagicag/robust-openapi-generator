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
package ch.imagic.openapi.model;

import java.util.List;
import java.util.Map;

public class PathModel {
    private String operationId;

    private RequestBodyModel requestBody;

    private ParameterModel[] parameters;

    private String summary;

    private List<String> tags;

    private Map<String, ResponseModel> responses;

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public RequestBodyModel getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(RequestBodyModel requestBody) {
        this.requestBody = requestBody;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, ResponseModel> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, ResponseModel> responses) {
        this.responses = responses;
    }

    public ParameterModel[] getParameters() {
        return parameters;
    }

    public void setParameters(ParameterModel[] parameters) {
        this.parameters = parameters;
    }


}
