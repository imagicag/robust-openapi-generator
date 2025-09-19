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

import ch.imagic.openapi.model.DiscriminatorModel;
import ch.imagic.openapi.model.SchemaModel;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataModelGenerator {

    public static void generateModels(GenerationContext context) {
        for (Map.Entry<String, SchemaModel> e : context.getModel().getComponents().getSchemas().entrySet()) {
            String name = e.getKey();
            SchemaModel schema = e.getValue();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
            switch (schemaClassification) {
                case OBJECT_IMPL:
                    //LATER
                    break;
                case ANY_OF:
                    //CURRENTLY TREATED SAME AS ONE_OF!
                case ONE_OF:
                    DataModelGenerator.prepareOneOf(context, name, schema);
                    break;
                default:
                    throw new IllegalStateException("Cannot generate model for schema " + name + " classification is " + schemaClassification);
            }
        }

        for (Map.Entry<String, SchemaModel> e : context.getModel().getComponents().getSchemas().entrySet()) {
            String name = e.getKey();
            SchemaModel schema = e.getValue();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
            switch (schemaClassification) {
                case OBJECT_IMPL:
                    DataModelGenerator.generateObjectModel(context, name, schema);
                    break;
                case ANY_OF:
                    //CURRENTLY TREATED SAME AS ONE_OF!
                case ONE_OF:
                    DataModelGenerator.generateOneOf(context, name, schema);
                    break;
                default:
                    throw new IllegalStateException("Cannot generate model for schema " + name + " classification is " + schemaClassification);
            }
        }
    }

    public static void prepareOneOf(GenerationContext context, String name, SchemaModel schema) {
        SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
        if (schemaClassification != SchemaClassification.ONE_OF &&  schemaClassification != SchemaClassification.ANY_OF) {
            throw new IllegalStateException("schema is not ONE_OF! " + schemaClassification);
        }

        String classname = context.modelNameToJavaClass(name);
        String fullyQualified = context.qualifyModelClass(classname);

        SchemaModel[] oneOf = schema.getOneOf();
        if (oneOf == null) {
            //TODO
            oneOf = schema.getAnyOf();
        }

        Set<String> implementations = new LinkedHashSet<>();

        for (SchemaModel model : oneOf) {
            SchemaClassification modelClass = SchemaClassification.fromSchema(name + ".oneOf", model);
            if (modelClass != SchemaClassification.REF) {
                throw new IllegalStateException("schema oneOf/anyOf contains non ref " + modelClass);
            }

            String resolvedName = model.get$ref();
            int lidx = resolvedName.lastIndexOf("/");
            if (lidx != -1) {
                resolvedName = resolvedName.substring(lidx + 1);
            }

            String rawImpl = context.modelNameToJavaClass(resolvedName);
            String qualifiedImpl = context.qualifyModelClass(rawImpl);
            implementations.add(qualifiedImpl);
        }

        for (String impl : implementations) {
            context.addInterface(impl, fullyQualified);
        }
    }

    public static void generateOneOf(GenerationContext context, String name, SchemaModel schema) {
        SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
        if (schemaClassification != SchemaClassification.ONE_OF &&  schemaClassification != SchemaClassification.ANY_OF) {
            throw new IllegalStateException("schema is not ONE_OF! " + schemaClassification);
        }

        String classname = context.modelNameToJavaClass(name);
        String fullyQualified = context.qualifyModelClass(classname);
        if (context.isCompatModel(fullyQualified)) {
            return;
        }

        String addIface = String.join(", ", context.getInterfaces(fullyQualified));
        if (!addIface.isEmpty()) {
            addIface = ", " + addIface;
        }

        DiscriminatorModel discriminator = schema.getDiscriminator();
        if (discriminator == null) {
            SchemaModel[] oneOf = schema.getOneOf();
            if (oneOf == null) {
                oneOf = schema.getAnyOf();
            }
            for (SchemaModel child : oneOf) {
                SchemaModel childRes = context.findSchema(child.get$ref());
                if (childRes.getDiscriminator() != null) {
                    if (discriminator != null) {
                        throw new IllegalArgumentException("Ambiguous discriminator while generating " + name);
                    }
                    discriminator = childRes.getDiscriminator();
                }
            }
        }

        Set<String> impl = context.getImplsForInterface(fullyQualified);

        context.push(fullyQualified, "package " + context.getModelPackage() + ";");
        context.push(fullyQualified, "");

        if (context.jackson()) {
            if (discriminator == null) {
                if (context.jackson()) {
                    context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION)");
                    String subtypes = impl.stream().map(a -> "@com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = "+ a + ".class)").collect(Collectors.joining(",\n    ", "{\n    ", "\n}"));
                    context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonSubTypes(" + subtypes+ ")");
                }

                context.push(fullyQualified, "@"+context.qualifyCommonApiClass("OneOf")+"(classes = {" + impl.stream().map(a -> a + ".class").collect(Collectors.joining(", "))+ "})");
            } else {
                String propName = discriminator.getPropertyName();

                SchemaModel[] oneOf = schema.getOneOf();
                if (oneOf == null) {
                    oneOf = schema.getAnyOf();
                }

                Map<String, String> mapping = new TreeMap<>();
                outer:
                for (SchemaModel child : oneOf) {
                    String childName = child.get$ref();
                    if (childName.startsWith("#/components/schemas/")) {
                        childName = childName.substring("#/components/schemas/".length());
                    }

                    String clazz = context.modelNameToJavaClass(childName);
                    String qualified = context.qualifyModelClass(clazz);
                    if (discriminator.getMapping() != null) {
                        for (Map.Entry<String, String> entry : discriminator.getMapping().entrySet()) {
                            if (entry.getValue().endsWith("/" + childName)) {
                                mapping.put(qualified, entry.getKey());
                                continue outer;
                            }
                        }
                    }

                    mapping.put(qualified, childName);
                }

                if (context.jackson()) {
                    context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonTypeInfo(use=com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property=\"" + Util.escapeForSourceCode(propName) + "\")");
                    String subtypes = mapping.entrySet().stream().map(a -> "@com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = " + a.getKey() + ".class, name = \"" + a.getValue() + "\")")
                            .collect(Collectors.joining(",\n    ", "{\n    ", "\n}"));
                    context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonSubTypes(" + subtypes + ")");
                }

                String mappingValues = impl.stream().sorted().map(a -> Objects.requireNonNull(mapping.get(a))).map(a -> "\"" + Util.escapeForSourceCode(a) + "\"").collect(Collectors.joining(", "));
                context.push(fullyQualified, "@"+context.qualifyCommonApiClass("OneOf")+"(discriminatorFieldName = \""+Util.escapeForSourceCode(propName)+"\", discriminatorFieldValues = {"+mappingValues+"}, classes = {" + impl.stream().sorted().map(a -> a + ".class").collect(Collectors.joining(", "))+ "})");
            }
        }

        context.push(fullyQualified, "public interface " + classname + " extends java.io.Serializable" + addIface +" {");
        context.push(fullyQualified, "}");
    }

    public static void generateObjectModel(GenerationContext context, String name, SchemaModel schema) {
        SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
        if (schemaClassification != SchemaClassification.OBJECT_IMPL) {
            throw new IllegalStateException("schema is not object! " + schemaClassification);
        }

        String classname = context.modelNameToJavaClass(name);
        String fullyQualified = context.qualifyModelClass(classname);
        if (context.isCompatModel(fullyQualified)) {
            return;
        }

        context.push(fullyQualified, "package " + context.getModelPackage() + ";");
        context.push(fullyQualified, "");

        String addIface = String.join(", ", context.getInterfaces(fullyQualified));
        if (!addIface.isEmpty()) {
            addIface = ", " + addIface;
        }

        context.push(fullyQualified, "public class " + classname + " implements java.io.Serializable, " + context.qualifyCommonApiClass("ToString") + ", " + context.qualifyCommonApiClass("Visitable") + addIface +" {");
        context.addIndent(fullyQualified);


        Set<String> required = new HashSet<>();
        if (schema.getRequired() != null) {
            required.addAll(Arrays.asList(schema.getRequired()));
        }

        Set<String> properties = new LinkedHashSet<>();

        for (Map.Entry<String, SchemaModel> e : schema.getProperties().entrySet()) {
            String propName = e.getKey();
            String mangledName = Util.mangleName(propName);
            if (!properties.add(mangledName)) {
                int cnt = 0;
                while(!properties.add(mangledName + cnt)) {
                    cnt++;
                }
                mangledName += cnt;
            }

            SchemaModel propSchema = e.getValue();
            List<String> enumValues = Util.getEnumRecursive(propSchema);
            String enumName = null;
            if (enumValues != null) {
                enumName = Util.capitalize(mangledName);
                Util.generateEnum(context, fullyQualified, enumName, enumValues);
            }

            SchemaClassification propClassification = SchemaClassification.fromSchema(name, propSchema);

            if (context.jsr380()) {
                generateModelFieldJSR380(context, name, required, propName, fullyQualified, propClassification, propSchema);
            }

            switch (propClassification) {
                case STRING:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "String", propSchema);
                    break;
                case ENUM:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, enumName, propSchema);
                    break;
                case INT64:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "Long", propSchema);
                    break;
                case INT32:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "Integer", propSchema);
                    break;
                case FLOAT:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "Float", propSchema);
                    break;
                case DOUBLE:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "Double", propSchema);
                    break;
                case BOOLEAN:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "Boolean", propSchema);
                    break;
                case MAP_SET_STRING:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<String>", propSchema);
                case MAP_SET_ENUM:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<"+enumName+">", propSchema);
                    break;
                case MAP_SET_BOOLEAN:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<Boolean>", propSchema);
                    break;
                case MAP_SET_INT64:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<Long>", propSchema);
                    break;
                case MAP_SET_INT32:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<Integer>", propSchema);
                    break;
                case MAP_SET_FLOAT:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<Float>", propSchema);
                    break;
                case MAP_SET_DOUBLE:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<Double>", propSchema);
                    break;
                case MAP_SET_OBJECT_REF: {
                    String ref = propSchema.getAdditionalProperties().getItems().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }

                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<"+clazz+">", propSchema);
                    break;
                }
                case MAP_SET_ANY:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.Set<Object>", propSchema);
                    break;
                case SET_STRING:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "String", propSchema);
                    break;
                case SET_ENUM:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, enumName, propSchema);
                    break;
                case SET_INT64:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "Long", propSchema);
                    break;
                case SET_INT32:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "Integer", propSchema);
                    break;
                case SET_FLOAT:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "Float", propSchema);
                    break;
                case SET_DOUBLE:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "Double", propSchema);
                    break;
                case SET_BOOLEAN:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "Boolean", propSchema);
                    break;
                case SET_OBJECT_REF: {
                    String ref = propSchema.getItems().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }

                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, clazz, propSchema);
                    break;
                }
                case SET_ANY:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "Object", propSchema);
                    break;
                case SET_MAP_STRING:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, String>", propSchema);
                    break;
                case SET_MAP_ENUM:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, "+enumName+">", propSchema);
                    break;
                case SET_MAP_BOOLEAN:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Boolean>", propSchema);
                    break;
                case SET_MAP_INT64:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Long>", propSchema);
                    break;
                case SET_MAP_INT32:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Integer>", propSchema);
                    break;
                case SET_MAP_FLOAT:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Float>", propSchema);
                    break;
                case SET_MAP_DOUBLE:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Double>", propSchema);
                    break;
                case SET_MAP_OBJECT_REF: {
                    String ref = propSchema.getItems().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }

                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, "+clazz+">", propSchema);
                    break;
                }
                case SET_MAP_ANY:
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Object>", propSchema);
                    break;
                case ARRAY_STRING:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "String", propSchema);
                    break;
                case ARRAY_ENUM:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, enumName, propSchema);
                    break;
                case ARRAY_BOOLEAN:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Boolean", propSchema);
                    break;
                case ARRAY_INT32:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Integer", propSchema);
                    break;
                case ARRAY_INT64:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Long", propSchema);
                    break;
                case ARRAY_DOUBLE:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Double", propSchema);
                    break;
                case ARRAY_FLOAT:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Float", propSchema);
                    break;
                case ARRAY_OBJECT_REF: {
                    String ref = propSchema.getItems().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }

                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, clazz, propSchema);
                    break;
                }
                case MAP_STRING:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "String", propSchema);
                    break;
                case MAP_ENUM:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, enumName, propSchema);
                    break;
                case MAP_BOOLEAN:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "Boolean", propSchema);
                    break;
                case MAP_INT64:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "Long", propSchema);
                    break;
                case MAP_INT32:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "Integer", propSchema);
                    break;
                case MAP_FLOAT:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "Float", propSchema);
                    break;
                case MAP_DOUBLE:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "Double", propSchema);
                    break;
                case MAP_ANY:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "Object", propSchema);
                    break;
                case MAP_OBJECT_REF: {
                    String ref = propSchema.getAdditionalProperties().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }
                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, clazz, propSchema);
                    break;
                }
                case MAP_ARRAY_STRING:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<String>", propSchema);
                    break;
                case MAP_ARRAY_ENUM:
                    Util.generateEnum(context, fullyQualified, Util.capitalize(mangledName), propSchema.getAdditionalProperties().getItems().get$enum());
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<"+Util.capitalize(mangledName)+">", propSchema);
                    break;
                case MAP_ARRAY_BOOLEAN:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<Boolean>", propSchema);
                    break;
                case MAP_ARRAY_INT64:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<Long>", propSchema);
                    break;
                case MAP_ARRAY_INT32:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<Integer>", propSchema);
                    break;
                case MAP_ARRAY_FLOAT:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<Float>", propSchema);
                    break;
                case MAP_ARRAY_DOUBLE:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<Double>", propSchema);
                    break;
                case MAP_ARRAY_OBJECT_REF: {
                    String ref = propSchema.getAdditionalProperties().getItems().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }
                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<"+clazz+">", propSchema);
                    break;
                }
                case MAP_ARRAY_ANY:
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, "java.util.List<Object>", propSchema);
                    break;
                case MULTI_DIMENSIONAL_MAP: {
                    String typeName = Util.findRecursiveTypeName(context, propSchema.getAdditionalProperties(), enumName);
                    MemberGenerator.generateMapMember(context, fullyQualified, propName, mangledName, classname, typeName, propSchema);
                    break;
                }
                case MULTI_DIMENSIONAL_ARRAY: {
                    String typeName = Util.findRecursiveTypeName(context, propSchema.getItems(), enumName);
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, typeName, propSchema);
                    break;
                }
                case MULTI_DIMENSIONAL_SET: {
                    String typeName = Util.findRecursiveTypeName(context, propSchema.getItems(), enumName);
                    MemberGenerator.generateSetMember(context, fullyQualified, propName, mangledName, classname, typeName, propSchema);
                    break;
                }
                case ARRAY_ANY:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "List<Object>", propSchema);
                    break;
                case REF: {
                    String ref = propSchema.get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }
                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, clazz, propSchema);
                    break;
                }
                case ARRAY_MAP_STRING:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, String>", propSchema);
                    break;
                case ARRAY_MAP_ENUM:
                    Util.generateEnum(context, fullyQualified, Util.capitalize(mangledName), propSchema.getItems().getAdditionalProperties().get$enum());
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, "+Util.capitalize(mangledName)+">", propSchema);
                    break;
                case ARRAY_MAP_BOOLEAN:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Boolean>", propSchema);
                    break;
                case ARRAY_MAP_INT64:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Long>", propSchema);
                    break;
                case ARRAY_MAP_INT32:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Integer>", propSchema);
                    break;
                case ARRAY_MAP_FLOAT:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Float>", propSchema);
                    break;
                case ARRAY_MAP_DOUBLE:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "java.util.Map<String, Double>", propSchema);
                    break;
                case ARRAY_MAP_OBJECT_REF: {
                    String ref = propSchema.getItems().getAdditionalProperties().get$ref();
                    if (context.findSchema(ref) == null) {
                        throw new IllegalStateException("Reference " + ref + " not found when generating schema " + name + " field " + propName);
                    }
                    String clazz = context.qualifyModelClass(context.modelNameToJavaClass(ref));

                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Map<String, " + clazz + ">", propSchema);
                    break;
                }
                case ARRAY_MAP_ANY:
                    MemberGenerator.generateListMember(context, fullyQualified, propName, mangledName, classname, "Map<String, Object>", propSchema);
                    break;
                case ANY:
                    MemberGenerator.generateTrivialMember(context, fullyQualified, propName, mangledName, classname, "Object", propSchema);
                    break;
                case MULTI_DIMENSIONAL_IMPL:
                case SET_IMPL:
                case UNION:
                case ANY_OF:
                case ONE_OF:
                case ARRAY_IMPL:
                case MAP_IMPL:
                case OBJECT_IMPL:
                default:
                    throw new IllegalStateException("Cannot generate model property for schema " + name + " field " + propName + " classification is " + propClassification + " this is likely a bug in the generator");
            }
        }

        Util.generateHashCodeEquals(context, fullyQualified, properties);
        Util.generateToString(context, fullyQualified, classname, properties);
        Util.generateVisitor(context, fullyQualified, classname, properties);

        context.subIndent(fullyQualified);
        context.push(fullyQualified, "}");

    }



    private static void generateModelFieldJSR380(GenerationContext context, String name, Set<String> required, String propName, String fullyQualified, SchemaClassification propClassification, SchemaModel propSchema) {
        if (required.contains(propName)) {
            context.push(fullyQualified, "@jakarta.validation.constraints.NotNull");
        }

        if (propClassification == SchemaClassification.STRING) {
            int max = Integer.MAX_VALUE;
            int min = 0;
            if (propSchema.getMaxLength() != null) {
                max = propSchema.getMaxLength();
            }
            if (propSchema.getMinLength() != null) {
                min = propSchema.getMinLength();
            }

            if (min > max || min < 0) {
                throw new IllegalArgumentException("Invalid min/max for string " + name + "." + propName + " min=" + min + " max=" + max);
            }

            if (min != 0 || max != Integer.MAX_VALUE) {
                context.push(fullyQualified, "@jakarta.validation.constraints.Size(min = "+min+", max = " + max + ")");
            }

            if (propSchema.getPattern() != null) {
                String pattern = propSchema.getPattern();
                try {
                    Pattern.compile(pattern);
                    context.push(fullyQualified, "@jakarta.validation.constraints.Pattern(regexp = \""+pattern+"\")");
                } catch (Exception ignored) {
                    System.out.println("WARNING: " + name + "." + propName + " has invalid pattern " + pattern);
                }
            }
        }

        if (propClassification == SchemaClassification.INT64 || propClassification == SchemaClassification.INT32) {
            if (propSchema.getMinimum() != null) {
                if (Boolean.TRUE.equals(propSchema.getExclusiveMinimum())) {
                    context.push(fullyQualified, "@jakarta.validation.constraints.Min(" + (propSchema.getMinimum().longValue()+1) + ")");
                } else {
                    context.push(fullyQualified, "@jakarta.validation.constraints.Min(" + propSchema.getMinimum().longValue() + ")");
                }
            }

            if (propSchema.getMaximum() != null) {
                if (Boolean.TRUE.equals(propSchema.getExclusiveMaximum())) {
                    context.push(fullyQualified, "@jakarta.validation.constraints.Min(" + (propSchema.getMinimum().longValue()-1) + ")");
                } else {
                    context.push(fullyQualified, "@jakarta.validation.constraints.Max(" + propSchema.getMaximum().longValue() + ")");
                }
            }
        }

        if (propClassification == SchemaClassification.FLOAT || propClassification == SchemaClassification.DOUBLE) {
            if (propSchema.getMinimum() != null) {
                if (Boolean.TRUE.equals(propSchema.getExclusiveMinimum())) {
                    context.push(fullyQualified, "@jakarta.validation.constraints.DecimalMin(value = \"" + new BigDecimal(propSchema.getMinimum().doubleValue()) + "\", inclusive = false)");
                } else {
                    context.push(fullyQualified, "@jakarta.validation.constraints.DecimalMin(\"" + new BigDecimal(propSchema.getMinimum().doubleValue()) + "\")");
                }

            }

            if (propSchema.getMaximum() != null) {
                if (Boolean.TRUE.equals(propSchema.getExclusiveMaximum())) {
                    context.push(fullyQualified, "@jakarta.validation.constraints.DecimalMin(value = \"" + new BigDecimal(propSchema.getMinimum().doubleValue()) + "\", inclusive = false)");
                } else {
                    context.push(fullyQualified, "@jakarta.validation.constraints.DecimalMin(\"" + new BigDecimal(propSchema.getMinimum().doubleValue()) + "\")");
                }
            }
        }

        //Yes, ghetto I know.
        if (propClassification.name().startsWith("ARRAY_")) {
            context.push(fullyQualified, "@jakarta.validation.Valid");
            int max = Integer.MAX_VALUE;
            int min = 0;
            if (propSchema.getMaxItems() != null) {
                max = propSchema.getMaxItems();
            }
            if (propSchema.getMinItems() != null) {
                min = propSchema.getMinItems();
            }

            if (min > max || min < 0) {
                throw new IllegalArgumentException("Invalid min/max items for array " + name + "." + propName + " min=" + min + " max=" + max);
            }

            if (min != 0 || max != Integer.MAX_VALUE) {
                context.push(fullyQualified, "@jakarta.validation.constraints.Size(min = "+min+", max = " + max + ")");
            }
        }

        //Yes, ghetto I know.
        if (propClassification.name().startsWith("MAP_")) {
            context.push(fullyQualified, "@jakarta.validation.Valid");
            int max = Integer.MAX_VALUE;
            int min = 0;
            if (propSchema.getMaxProperties() != null) {
                max = propSchema.getMaxProperties();
            }
            if (propSchema.getMinProperties() != null) {
                min = propSchema.getMinProperties();
            }

            if (min > max || min < 0) {
                throw new IllegalArgumentException("Invalid min/max items for map " + name + "." + propName + " min=" + min + " max=" + max);
            }

            if (min != 0 || max != Integer.MAX_VALUE) {
                context.push(fullyQualified, "@jakarta.validation.constraints.Size(min = "+min+", max = " + max + ")");
            }
        }

        if(propClassification == SchemaClassification.REF) {
            context.push(fullyQualified, "@jakarta.validation.Valid");
        }
    }


}
