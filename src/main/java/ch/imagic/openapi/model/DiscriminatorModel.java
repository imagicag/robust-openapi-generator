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

public class DiscriminatorModel {
    private String propertyName;
    private Map<String, String> mapping;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        DiscriminatorModel that = (DiscriminatorModel) o;
        return Objects.equals(propertyName, that.propertyName) && Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(propertyName);
        result = 31 * result + Objects.hashCode(mapping);
        return result;
    }
}
