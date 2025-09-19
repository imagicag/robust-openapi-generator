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

import ch.imagic.openapi.model.SchemaModel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Util {

    public static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static final Set<String> BAD_OP_IDS= new HashSet<>(Arrays.asList());

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList("class", "public", "private", "synchronized", "static", "final", "interface", "enum", "protected", "goto", "strictfp", "long", "int", "char", "byte", "boolean", "float", "short", "double", "object", "sealed", "abstract", "assert", "break", "case", "switch", "continue", "do", "while", "for", "extends", "implements", "import", "package", "instanceof", "super", "this", "throws", "throw", "transient", "try", "catch", "volatile", "void", "exports", "opens", "open", "provides", "permits", "record", "requires", "to", "transitive", "var", "with", "when", "yield", "uses", "true", "false", "null", "const"));

    //TODO this list is probably incomplete, alas I cant be asked to escape all the odd unicode symbols, emojis dont compile for example, but for example japan language such as ッ compiles.
    private static final Set<Character> RESERVED_CHARACTERS = new HashSet<>(Arrays.asList(
            '-',
            '{',
            '}',
            '%',
            '&',
            '|',
            ';',
            ':',
            '.',
            '`',
            '´',
            '\'',
            '\\',
            '/',
            '*',
            '-',
            '+',
            '=',
            '?',
            '§',
            '<',
            '>',
            '!',
            '(',
            ')',
            '"',
            '#',
            '~',
            ',',
            '°',
            '^',
            ']',
            '[',
            '@',
            '²',
            '³',
            '¼',
            '½',
            '¬',
            '¸',
            '·',
            '…',
            '\uFFF0',
            '\uFFF1',
            '\uFFF2',
            '\uFFF3',
            '\uFFF4',
            '\uFFF5',
            '\uFFF6',
            '\uFFF7',
            '\uFFF8',
            '\uFFF9',
            '\uFFFA',
            '\uFFFB',
            '\uFFFC',
            '\uFFFD',
            '\uFFFE',
            '\uFFFF',
            '\u00A0',
            '\u202F',
            '\r',
            '\n',
            '\t',
            '\0',
            ' '
            ));

    public static String mangleContentType(String ct) {
        switch (ct.toLowerCase()) {
            case "application/octet-stream":
                return "Bin";
            case "application/json":
                return "Json";
            case "text/plain":
                return "Text";
            case "image/*":
                return "AnyImage";
            case "image/jpeg":
                return "Jpeg";
            case "image/png":
                return "Png";
            case "*/*":
                return "Any";
        }
        return mangleName(ct);
    }

    public static boolean canEnum(String name) {
        if (KEYWORDS.contains(name)) {
            return false;
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (RESERVED_CHARACTERS.contains(c) || c == ' ') {
                return false;
            }
        }

        return true;
    }

    public static String mangleOpId(String opId) {
        if (BAD_OP_IDS.contains(opId.toLowerCase())) {
            opId = opId + "$";
        }
        return mangleName(opId);
    }

    public static String mangleName(String name) {
        if (KEYWORDS.contains(name.toLowerCase())) {
            name = name + "$";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ' ' || c == '/') {
                if (name.length() > i+1 && Character.isAlphabetic(name.charAt(i+1))) {
                    i++;
                    c = Character.toUpperCase(name.charAt(i));
                }
            }
            if (RESERVED_CHARACTERS.contains(c)) {
                sb.append("$");
                continue;
            }

            sb.append(c);
        }
        name = sb.toString();

        if (name.equalsIgnoreCase("")) {
            name = "$";
        }

        if (Character.isDigit(name.charAt(0))) {
            name = "$"+name;
        }

        return name;
    }

    private static final Map<String, String> CHARACTERS_THAT_NEED_ESCAPING = new HashMap<>();
    static {
        CHARACTERS_THAT_NEED_ESCAPING.put("\"", "\\\"");
        CHARACTERS_THAT_NEED_ESCAPING.put("\\", "\\\\");
        CHARACTERS_THAT_NEED_ESCAPING.put("\r", "\\r");
        CHARACTERS_THAT_NEED_ESCAPING.put("\n", "\\n");
        CHARACTERS_THAT_NEED_ESCAPING.put("\t", "\\t");
        CHARACTERS_THAT_NEED_ESCAPING.put("\0", "\\0");
    }

    public static String escapeForSourceCode(String name) {
        for (Map.Entry<String, String> s : CHARACTERS_THAT_NEED_ESCAPING.entrySet()) {
            name = name.replace(s.getKey(), s.getValue());
        }

        return name;
    }

    public static String readResource(String name) {
        try (InputStream is = Util.class.getResourceAsStream(name)) {
            if (is == null) {
                throw new RuntimeException("Resource " + name + " not found");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static void pushJavaDoc(GenerationContext context, String clazz, SchemaModel schema) {
        if (schema != null) {
            pushJavaDoc(context, clazz, schema.getDescription());
        }
    }

    public static void pushJavaDoc(GenerationContext context, String clazz, String doc) {
        if (doc != null) {
            context.push(clazz, "/**");
            for (String s : doc.replace("\r\n", "\n").split("\n")) {
                context.push(clazz, " * " + s);
            }
            context.push(clazz, " */");
        }
    }

    public static List<String> getEnumRecursive(SchemaModel schema) {
        SchemaClassification schemaClassification = SchemaClassification.fromSchema("isEnumRecursive", schema);
        switch (schemaClassification) {
            case ENUM:
                return schema.get$enum();
            case SET_ENUM:
            case ARRAY_ENUM:
                return schema.getItems().get$enum();
            case MAP_ENUM:
                return schema.getAdditionalProperties().get$enum();
            case ARRAY_MAP_ENUM:
                return schema.getItems().getAdditionalProperties().get$enum();
            case MAP_ARRAY_ENUM:
                return schema.getAdditionalProperties().getItems().get$enum();
            case MULTI_DIMENSIONAL_ARRAY:
            case MULTI_DIMENSIONAL_SET:
                return getEnumRecursive(schema.getItems());
            case MULTI_DIMENSIONAL_MAP:
                return getEnumRecursive(schema.getAdditionalProperties());
            default:
                return null;
        }
    }

    public static boolean isJsonStringSpecialSchema(SchemaModel model) {
        return model != null && "object".equalsIgnoreCase(model.getType()) && "string".equalsIgnoreCase(model.getFormat());
    }

    public static String findRecursiveTypeName(GenerationContext context, SchemaModel schema, String enumName) {
        SchemaClassification schemaClassification = SchemaClassification.fromSchema("findRecursiveTypeName", schema);
        switch (schemaClassification) {
            case STRING:
                return "String";
            case ENUM:
                return Objects.requireNonNull(enumName);
            case ARRAY_ENUM:
                return "java.util.List<" + Objects.requireNonNull(enumName) + ">";
            case MAP_ENUM:
                return "java.util.Map<String, " + Objects.requireNonNull(enumName) + ">";
            case ARRAY_MAP_ENUM:
                return "java.util.List<java.util.Map<String, " + Objects.requireNonNull(enumName) + ">>";
            case MAP_ARRAY_ENUM:
                return "java.util.Map<String, java.util.List<" + Objects.requireNonNull(enumName) + ">>";
            case INT64:
                return "Long";
            case INT32:
                return "Integer";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case BOOLEAN:
                return "Boolean";
            case REF: {
                String name = schema.get$ref();
                return context.qualifyModelClass(context.modelNameToJavaClass(name));
            }
            case OBJECT_IMPL:
                throw new IllegalArgumentException("OBJECT_IMPL does not have a type name");
            case MAP_STRING:
                return "java.util.Map<String, String>";
            case MAP_BOOLEAN:
                return "java.util.Map<String, Boolean>";
            case MAP_INT64:
                return "java.util.Map<String, Long>";
            case MAP_INT32:
                return "java.util.Map<String, Integer>";
            case MAP_FLOAT:
                return "java.util.Map<String, Float>";
            case MAP_DOUBLE:
                return "java.util.Map<String, Double>";
            case MAP_OBJECT_REF: {
                String name = schema.getAdditionalProperties().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));
                return "java.util.Map<String, " + ctx + ">";
            }
            case MAP_ANY:
                return "java.util.Map<String, Object>";
            case MAP_IMPL:
                throw new IllegalArgumentException("MAP_IMPL does not have a type name");
            case MAP_ARRAY_STRING:
                return "java.util.Map<String, java.util.List<String>>";
            case MAP_ARRAY_BOOLEAN:
                return "java.util.Map<String, java.util.List<Boolean>>";
            case MAP_ARRAY_INT64:
                return "java.util.Map<String, java.util.List<Long>>";
            case MAP_ARRAY_INT32:
                return "java.util.Map<String, java.util.List<Integer>>";
            case MAP_ARRAY_FLOAT:
                return "java.util.Map<String, java.util.List<Float>>";
            case MAP_ARRAY_DOUBLE:
                return "java.util.Map<String, java.util.List<Double>>";
            case MAP_ARRAY_OBJECT_REF: {
                String name = schema.getAdditionalProperties().getItems().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));

                return "java.util.Map<String, java.util.List<" + ctx + ">>";
            }
            case MULTI_DIMENSIONAL_IMPL:
                throw new IllegalArgumentException("MULTI_DIMENSIONAL_IMPL does not have a type name");
            case MAP_ARRAY_ANY:
                return "java.util.Map<String, java.util.List<Object>>";
            case MAP_SET_STRING:
                return "java.util.Map<String, java.util.Set<String>>";
            case MAP_SET_ENUM:
                return "java.util.Map<String, java.util.Set<"+Objects.requireNonNull(enumName)+">>";
            case MAP_SET_BOOLEAN:
                return "java.util.Map<String, java.util.Set<Boolean>>";
            case MAP_SET_INT64:
                return "java.util.Map<String, java.util.Set<Long>>";
            case MAP_SET_INT32:
                return "java.util.Map<String, java.util.Set<Integer>>";
            case MAP_SET_FLOAT:
                return "java.util.Map<String, java.util.Set<Float>>";
            case MAP_SET_DOUBLE:
                return "java.util.Map<String, java.util.Set<Double>>";
            case MAP_SET_OBJECT_REF: {
                String name = schema.getAdditionalProperties().getItems().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));
                return "java.util.Map<String, java.util.Set<" + ctx + ">>";
            }
            case MAP_SET_ANY:
                return "java.util.Map<String, java.util.Set<Object>>";
            case SET_STRING:
                return "java.util.Set<String>";
            case SET_ENUM:
                return "java.util.Set<"+Objects.requireNonNull(enumName)+">";
            case SET_INT64:
                return "java.util.Set<Long>";
            case SET_INT32:
                return "java.util.Set<Integer>";
            case SET_FLOAT:
                return "java.util.Set<Float>";
            case SET_DOUBLE:
                return "java.util.Set<Double>";
            case SET_BOOLEAN:
                return "java.util.Set<Boolean>";
            case SET_OBJECT_REF: {
                String name = schema.getItems().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));
                return "java.util.Set<" + ctx + ">";
            }
            case SET_ANY:
                return "java.util.Set<Object>";
            case SET_IMPL:
                throw new IllegalArgumentException("SET_IMPL does not have a type name");
            case SET_MAP_STRING:
                return "java.util.Set<java.util.Map<String, String>>";
            case SET_MAP_ENUM:
                return "java.util.Set<java.util.Map<String, "+Objects.requireNonNull(enumName)+">>";
            case SET_MAP_BOOLEAN:
                return "java.util.Set<java.util.Map<String, Boolean>>";
            case SET_MAP_INT64:
                return "java.util.Set<java.util.Map<String, Long>>";
            case SET_MAP_INT32:
                return "java.util.Set<java.util.Map<String, Integer>>";
            case SET_MAP_FLOAT:
                return "java.util.Set<java.util.Map<String, Float>>";
            case SET_MAP_DOUBLE:
                return "java.util.Set<java.util.Map<String, Double>>";
            case SET_MAP_OBJECT_REF: {
                String name = schema.getItems().getAdditionalProperties().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));
                return "java.util.Set<java.util.Map<String, " + ctx + ">>";
            }
            case SET_MAP_ANY:
                return "java.util.Set<java.util.Map<String, Object>>";
            case ARRAY_STRING:
                return "java.util.List<String>";
            case ARRAY_BOOLEAN:
                return "java.util.List<Boolean>";
            case ARRAY_INT64:
                return "java.util.List<Long>";
            case ARRAY_INT32:
                return "java.util.List<Integer>";
            case ARRAY_FLOAT:
                return "java.util.List<Float>";
            case ARRAY_DOUBLE:
                return "java.util.List<Double>";
            case ARRAY_OBJECT_REF: {
                String name = schema.getItems().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));
                return "java.util.List<" + ctx + ">";
            }
            case ARRAY_IMPL:
                throw new IllegalArgumentException("ARRAY_IMPL does not have a type name");
            case ARRAY_ANY:
                return "java.util.List<Object>";
            case ARRAY_MAP_STRING:
                return "java.util.List<java.util.Map<String, String>>";
            case ARRAY_MAP_BOOLEAN:
                return "java.util.List<java.util.Map<String, Boolean>>";
            case ARRAY_MAP_INT64:
                return "java.util.List<java.util.Map<String, Long>>";
            case ARRAY_MAP_INT32:
                return "java.util.List<java.util.Map<String, Integer>>";
            case ARRAY_MAP_FLOAT:
                return "java.util.List<java.util.Map<String, Float>>";
            case ARRAY_MAP_DOUBLE:
                return "java.util.List<java.util.Map<String, Double>>";
            case ARRAY_MAP_OBJECT_REF: {
                String name = schema.getItems().getAdditionalProperties().get$ref();
                String ctx = context.qualifyModelClass(context.modelNameToJavaClass(name));
                return "java.util.List<java.util.Map<String, " + ctx + ">";
            }
            case ARRAY_MAP_ANY:
                return "java.util.List<java.util.Map<String, Object>>";
            case UNION:
                throw new IllegalArgumentException("UNION does not have a type name");
            case ANY_OF:
                throw new IllegalArgumentException("ANY_OF does not have a type name");
            case ONE_OF:
                throw new IllegalArgumentException("ONE_OF does not have a type name");
            case ANY:
                return "Object";
            case MULTI_DIMENSIONAL_SET:
                return "java.util.Set<"+findRecursiveTypeName(context, schema.getItems(), enumName)+">";
            case MULTI_DIMENSIONAL_MAP:
                return "java.util.Map<String, "+findRecursiveTypeName(context, schema.getItems(), enumName)+">";
            case MULTI_DIMENSIONAL_ARRAY:
                return "java.util.List<"+findRecursiveTypeName(context, schema.getItems(), enumName)+">";
            default:
                throw new UnsupportedOperationException("not implemented yet " + schemaClassification);
        }
    }

    public static boolean isStatusCode(String key) {
        if (key.length() != 3) {
            return false;
        }

        for (int i = 0; i < key.length(); i++) {
            if (!Character.isDigit(key.charAt(i))) {
                return false;
            }
        }

        return Long.parseLong(key) >= 100;
    }

    public static String contentTypeToEnumPrefix(String ct) {
        if (ct.equals("*/*")) {
            return "ANY";
        }
        return ct.replace("/", "_").replace("-", "_").replace('+', '_').replace("*", "$").toUpperCase();
    }

    public static Set<String> getContainingTypeModelNames(GenerationContext ctx, SchemaModel schema, String name) {
        Set<String> elements = new TreeSet<>();
        if (name != null) {
            elements.add(name);
            name = "";
        }
        getContainingTypeModelNames(ctx, schema, name, elements);
        return elements;
    }

    private static void getContainingTypeModelNames(GenerationContext ctx, SchemaModel schema, String name, Set<String> schemas) {
        SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
        String ref = null;
        switch (schemaClassification) {
            case REF:
                ref = schema.get$ref();
                break;
            case ARRAY_OBJECT_REF:
                ref = schema.getItems().get$ref();
                break;
            case MAP_OBJECT_REF:
                ref = schema.getAdditionalProperties().get$ref();
                break;
        }

        if (ref != null) {
            if (schemas.add(ref)) {
                SchemaModel schem = ctx.findSchema(ref);
                getContainingTypeModelNames(ctx, schem, ref, schemas);
            }
        }

        switch (schemaClassification) {
            case OBJECT_IMPL: {
                schema.getProperties().forEach((key, value) -> getContainingTypeModelNames(ctx, value, name + "." + key, schemas));
                break;
            }
            case ONE_OF:
                Arrays.stream(schema.getOneOf()).forEach(child -> getContainingTypeModelNames(ctx, child,name + ".oneOf", schemas));
                break;
            case ANY_OF:
                Arrays.stream(schema.getAnyOf()).forEach(child -> getContainingTypeModelNames(ctx, child, name + ".anyOf", schemas));
                break;
            case MULTI_DIMENSIONAL_IMPL:
                if (schema.getItems() != null) {
                    getContainingTypeModelNames(ctx, schema.getItems(), name + ".items", schemas);
                }
                if (schema.getAdditionalProperties() != null) {
                    getContainingTypeModelNames(ctx, schema.getItems(), name + ".items", schemas);
                }
                break;
        }
    }

    public static void generateEnum(GenerationContext context, String containingClass, String enumName, List<String> variants) {
        context.push(containingClass, "");
        context.push(containingClass, "public enum " + enumName + " {");
        context.addIndent(containingClass);
        Set<String> generatedVariants = new HashSet<>();
        for (String v : variants) {
            if (!Util.canEnum(v)) {
                System.out.println("WARNING enum name " + v + " for class " + containingClass + " enum " + enumName + " cannot be generated as it would not compile, will simply skip this variant.");
                continue;
            }
            if (!generatedVariants.add(v)) {
                continue;
            }
            context.push(containingClass, v + ",");
        }

        context.subIndent(containingClass);
        context.push(containingClass, "}");
    }

    public static void generateHashCodeEquals(GenerationContext context,  String fullyQualifiedClazzName, Iterable<String> members) {
        context.push(fullyQualifiedClazzName ,"");
        context.push(fullyQualifiedClazzName, "@Override");
        context.push(fullyQualifiedClazzName, "public boolean equals(Object o) {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "if (o == null || getClass() != o.getClass()) {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "return false;");
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");

        context.push(fullyQualifiedClazzName, fullyQualifiedClazzName + " other = (" + fullyQualifiedClazzName + ") o;");

        context.push(fullyQualifiedClazzName, "return true");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "//");
        for (String prop : members) {
            context.push(fullyQualifiedClazzName, "&& java.util.Objects.equals(this." + Util.escapeForSourceCode(prop) + ", other." + Util.escapeForSourceCode(prop) + ")" );
        }
        context.push(fullyQualifiedClazzName, "//");
        context.push(fullyQualifiedClazzName, ";");
        context.subIndent(fullyQualifiedClazzName);
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");

        context.push(fullyQualifiedClazzName ,"");
        context.push(fullyQualifiedClazzName, "@Override");
        context.push(fullyQualifiedClazzName, "public int hashCode() {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "return java.util.Objects.hash(");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "//");
        Iterator<String> iter = members.iterator();
        while(iter.hasNext()) {
            String prop = iter.next();
            if (iter.hasNext()) {
                context.push(fullyQualifiedClazzName, "this." + Util.escapeForSourceCode(prop) + ", ");
                continue;
            }
            context.push(fullyQualifiedClazzName, "this." + Util.escapeForSourceCode(prop));
        }
        context.push(fullyQualifiedClazzName, "//");
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, ");");
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");
    }

    public static void generateVisitor(GenerationContext context, String fullyQualifiedClazzName, String simpleClassName, Iterable<String> members) {
        context.push(fullyQualifiedClazzName ,"");
        context.push(fullyQualifiedClazzName, "@Override");
        context.push(fullyQualifiedClazzName, "public <E extends Throwable> void visit("+ context.qualifyCommonApiClass("PropertyVisitor")+"<E> visitor) throws E {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, context.qualifyCommonApiClass("Visitable")+".visitObject(this, visitor");
        context.addIndent(fullyQualifiedClazzName);
        for (String prop : members) {
            context.push(fullyQualifiedClazzName, ", \""+ Util.escapeForSourceCode(prop) + "\", this." + Util.escapeForSourceCode(prop));
        }
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, ");");

        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");
        context.push(fullyQualifiedClazzName, "");
    }

    public static void generateToString(GenerationContext context, String fullyQualifiedClazzName, String simpleClassName, Iterable<String> members) {
        context.push(fullyQualifiedClazzName ,"");
        context.push(fullyQualifiedClazzName, "@Override");
        context.push(fullyQualifiedClazzName, "public String toString() {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "return this.toString(0);");
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");
        context.push(fullyQualifiedClazzName, "");

        context.push(fullyQualifiedClazzName, "@Override");
        context.push(fullyQualifiedClazzName, "public String toString(int level) {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "level+=1;");
        context.push(fullyQualifiedClazzName, "StringBuilder sb = new StringBuilder(\"" + simpleClassName + "{\\n\");");
        for (String prop : members) {
            context.push(fullyQualifiedClazzName, "for (int i = 0; i < level; i++) {");
            context.addIndent(fullyQualifiedClazzName);
            context.push(fullyQualifiedClazzName, "sb.append(\"  \");");
            context.subIndent(fullyQualifiedClazzName);
            context.push(fullyQualifiedClazzName, "}");

            context.push(fullyQualifiedClazzName, "sb.append(\"" + prop + "=\");");
            context.push(fullyQualifiedClazzName, "sb.append(" + context.qualifyCommonApiClass("ToString") +".toString(" + Util.escapeForSourceCode(prop) + ", level));");
            context.push(fullyQualifiedClazzName, "sb.append('\\n');");
        }
        context.push(fullyQualifiedClazzName, "for (int i = 0; i < level-1; i++) {");
        context.addIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "sb.append(\"  \");");
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");
        context.push(fullyQualifiedClazzName, "sb.append('}');");
        context.push(fullyQualifiedClazzName, "return sb.toString();");
        context.subIndent(fullyQualifiedClazzName);
        context.push(fullyQualifiedClazzName, "}");


    }
}
