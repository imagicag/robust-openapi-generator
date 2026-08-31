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

import java.util.Map;

public class FieldTypesGenerator {

    public static void generate(GenerationContext context) {
        String fieldTypesClass = context.getPackageName() + ".api.FieldTypes";
        context.push(fieldTypesClass, "package " + context.getPackageName() + ".api;");
        context.push(fieldTypesClass, "");
        context.push(fieldTypesClass, "/** Generic type information for fields in generated API models. */");
        context.push(fieldTypesClass, "public final class FieldTypes {");
        context.addIndent(fieldTypesClass);
        context.push(fieldTypesClass, "private FieldTypes() {");
        context.push(fieldTypesClass, "}");
        context.push(fieldTypesClass, "");
        context.push(fieldTypesClass, "private static abstract class TypeInfo<T> {");
        context.addIndent(fieldTypesClass);
        context.push(fieldTypesClass, "public java.lang.reflect.Type getType() {");
        context.addIndent(fieldTypesClass);
        context.push(fieldTypesClass, "return ((java.lang.reflect.ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];");
        context.subIndent(fieldTypesClass);
        context.push(fieldTypesClass, "}");
        context.subIndent(fieldTypesClass);
        context.push(fieldTypesClass, "}");

        for (Map.Entry<String, Long> entry : context.getFieldTypeInfoMapping().entrySet()) {
            context.push(fieldTypesClass, "");
            context.push(fieldTypesClass, "/** Generic type for " + entry.getKey().replace("<", "&lt;").replace(">", "&gt;") + ". */");
            context.push(fieldTypesClass, "public static final java.lang.reflect.Type TYPE" + entry.getValue() + " = new TypeInfo<" + entry.getKey() + ">() {}.getType();");
        }

        context.subIndent(fieldTypesClass);
        context.push(fieldTypesClass, "}");
    }
}
