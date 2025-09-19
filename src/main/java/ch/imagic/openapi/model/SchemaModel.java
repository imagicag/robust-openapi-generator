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

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SchemaModel {

    private String $ref;

    private DiscriminatorModel discriminator;

    private SchemaModel[] allOf;

    private SchemaModel[] anyOf;

    private SchemaModel[] oneOf;

    private SchemaModel items;

    private Number maximum;

    private Boolean exclusiveMaximum;

    private Number minimum;

    private Boolean exclusiveMinimum;

    private Integer maxLength;

    private Integer minLength;

    private Integer maxItems;

    private Integer minItems;

    private Integer maxProperties;

    private Integer minProperties;

    private String pattern;

    private String title;

    @SerializedName("enum")
    private List<String> $enum;

    private String type;

    private String format;

    private String description;

    private String[] required;

    private boolean uniqueItems;

    private SchemaModel additionalProperties;

    private Map<String, SchemaModel> properties;

    public String get$ref() {
        return $ref;
    }

    public void set$ref(String $ref) {
        this.$ref = $ref;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getRequired() {
        return required;
    }

    public void setRequired(String[] required) {
        this.required = required;
    }

    public Map<String, SchemaModel> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, SchemaModel> properties) {
        this.properties = properties;
    }

    public SchemaModel[] getAllOf() {
        return allOf;
    }

    public SchemaModel[] getAnyOf() {
        return anyOf;
    }

    public void setAnyOf(SchemaModel[] anyOf) {
        this.anyOf = anyOf;
    }

    public void setAllOf(SchemaModel[] allOf) {
        this.allOf = allOf;
    }

    public SchemaModel getItems() {
        return items;
    }

    public DiscriminatorModel getDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(DiscriminatorModel discriminator) {
        this.discriminator = discriminator;
    }

    public SchemaModel[] getOneOf() {
        return oneOf;
    }

    public void setOneOf(SchemaModel[] oneOf) {
        this.oneOf = oneOf;
    }

    public void setItems(SchemaModel items) {
        this.items = items;
    }

    public SchemaModel getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(SchemaModel additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public boolean isUniqueItems() {
        return uniqueItems;
    }

    public void setUniqueItems(boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    public List<String> get$enum() {
        return $enum;
    }

    public void set$enum(List<String> $enum) {
        this.$enum = $enum;
    }

    public Number getMaximum() {
        return maximum;
    }

    public void setMaximum(Number maximum) {
        this.maximum = maximum;
    }

    public Boolean getExclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void setExclusiveMaximum(Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public Number getMinimum() {
        return minimum;
    }

    public void setMinimum(Number minimum) {
        this.minimum = minimum;
    }

    public Boolean getExclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void setExclusiveMinimum(Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(Integer maxItems) {
        this.maxItems = maxItems;
    }

    public Integer getMinItems() {
        return minItems;
    }

    public void setMinItems(Integer minItems) {
        this.minItems = minItems;
    }

    public Integer getMaxProperties() {
        return maxProperties;
    }

    public void setMaxProperties(Integer maxProperties) {
        this.maxProperties = maxProperties;
    }

    public Integer getMinProperties() {
        return minProperties;
    }

    public void setMinProperties(Integer minProperties) {
        this.minProperties = minProperties;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SchemaModel that = (SchemaModel) o;
        return uniqueItems == that.uniqueItems && Objects.equals($ref, that.$ref) && Objects.equals(discriminator, that.discriminator) && Arrays.equals(allOf, that.allOf) && Arrays.equals(anyOf, that.anyOf) && Arrays.equals(oneOf, that.oneOf) && Objects.equals(items, that.items) && Objects.equals(maximum, that.maximum) && Objects.equals(exclusiveMaximum, that.exclusiveMaximum) && Objects.equals(minimum, that.minimum) && Objects.equals(exclusiveMinimum, that.exclusiveMinimum) && Objects.equals(maxLength, that.maxLength) && Objects.equals(minLength, that.minLength) && Objects.equals(maxItems, that.maxItems) && Objects.equals(minItems, that.minItems) && Objects.equals(maxProperties, that.maxProperties) && Objects.equals(minProperties, that.minProperties) && Objects.equals(pattern, that.pattern) && Objects.equals(title, that.title) && Objects.equals($enum, that.$enum) && Objects.equals(type, that.type) && Objects.equals(format, that.format) && Objects.equals(description, that.description) && Arrays.equals(required, that.required) && Objects.equals(additionalProperties, that.additionalProperties) && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode($ref);
        result = 31 * result + Objects.hashCode(discriminator);
        result = 31 * result + Arrays.hashCode(allOf);
        result = 31 * result + Arrays.hashCode(anyOf);
        result = 31 * result + Arrays.hashCode(oneOf);
        result = 31 * result + Objects.hashCode(items);
        result = 31 * result + Objects.hashCode(maximum);
        result = 31 * result + Objects.hashCode(exclusiveMaximum);
        result = 31 * result + Objects.hashCode(minimum);
        result = 31 * result + Objects.hashCode(exclusiveMinimum);
        result = 31 * result + Objects.hashCode(maxLength);
        result = 31 * result + Objects.hashCode(minLength);
        result = 31 * result + Objects.hashCode(maxItems);
        result = 31 * result + Objects.hashCode(minItems);
        result = 31 * result + Objects.hashCode(maxProperties);
        result = 31 * result + Objects.hashCode(minProperties);
        result = 31 * result + Objects.hashCode(pattern);
        result = 31 * result + Objects.hashCode(title);
        result = 31 * result + Objects.hashCode($enum);
        result = 31 * result + Objects.hashCode(type);
        result = 31 * result + Objects.hashCode(format);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Arrays.hashCode(required);
        result = 31 * result + Boolean.hashCode(uniqueItems);
        result = 31 * result + Objects.hashCode(additionalProperties);
        result = 31 * result + Objects.hashCode(properties);
        return result;
    }
}
