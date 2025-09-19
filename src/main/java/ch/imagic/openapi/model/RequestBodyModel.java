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

import java.util.Map;
import java.util.Objects;

public class RequestBodyModel {
    private String $ref;
    private Map<String, PathSchemaModel> content;

    public Map<String, PathSchemaModel> getContent() {
        return content;
    }

    public void setContent(Map<String, PathSchemaModel> content) {
        this.content = content;
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        RequestBodyModel that = (RequestBodyModel) o;
        return Objects.equals($ref, that.$ref) && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode($ref);
        result = 31 * result + Objects.hashCode(content);
        return result;
    }
}
