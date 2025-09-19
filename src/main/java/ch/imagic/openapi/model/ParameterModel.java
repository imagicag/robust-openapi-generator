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

import java.util.Objects;

public class ParameterModel {
    private String $ref;
    private String name;
    private String in;
    private String description;
    private boolean required;
    private boolean deprecated;
    private String style;
    private boolean explode;
    private SchemaModel schema;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStyle() {
        return style;
    }



    public void setStyle(String style) {
        this.style = style;
    }

    public boolean isExplode() {
        return explode;
    }

    public void setExplode(boolean explode) {
        this.explode = explode;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }

    public SchemaModel getSchema() {
        return schema;
    }

    public void setSchema(SchemaModel schema) {
        this.schema = schema;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ParameterModel that = (ParameterModel) o;
        return required == that.required && deprecated == that.deprecated && explode == that.explode && Objects.equals($ref, that.$ref) && Objects.equals(name, that.name) && Objects.equals(in, that.in) && Objects.equals(description, that.description) && Objects.equals(style, that.style) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode($ref);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(in);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Boolean.hashCode(required);
        result = 31 * result + Boolean.hashCode(deprecated);
        result = 31 * result + Objects.hashCode(style);
        result = 31 * result + Boolean.hashCode(explode);
        result = 31 * result + Objects.hashCode(schema);
        return result;
    }
}
