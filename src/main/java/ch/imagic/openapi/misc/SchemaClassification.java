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

public enum SchemaClassification {
    STRING,
    ENUM,
    INT64,
    INT32,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    REF,
    OBJECT_IMPL,

    MAP_STRING,
    MAP_ENUM,
    MAP_BOOLEAN,
    MAP_INT64,
    MAP_INT32,
    MAP_FLOAT,
    MAP_DOUBLE,
    MAP_OBJECT_REF,
    MAP_ANY,
    MAP_IMPL,
    MAP_ARRAY_STRING,
    MAP_ARRAY_ENUM,
    MAP_ARRAY_BOOLEAN,
    MAP_ARRAY_INT64,
    MAP_ARRAY_INT32,
    MAP_ARRAY_FLOAT,
    MAP_ARRAY_DOUBLE,
    MAP_ARRAY_OBJECT_REF,
    MAP_ARRAY_ANY,


    MAP_SET_STRING,
    MAP_SET_ENUM,
    MAP_SET_BOOLEAN,
    MAP_SET_INT64,
    MAP_SET_INT32,
    MAP_SET_FLOAT,
    MAP_SET_DOUBLE,
    MAP_SET_OBJECT_REF,
    MAP_SET_ANY,

    SET_STRING,
    SET_ENUM,
    SET_INT64,
    SET_INT32,
    SET_FLOAT,
    SET_DOUBLE,
    SET_BOOLEAN,
    SET_OBJECT_REF,
    SET_ANY,
    SET_IMPL,
    SET_MAP_STRING,
    SET_MAP_ENUM,
    SET_MAP_BOOLEAN,
    SET_MAP_INT64,
    SET_MAP_INT32,
    SET_MAP_FLOAT,
    SET_MAP_DOUBLE,
    SET_MAP_OBJECT_REF,
    SET_MAP_ANY,

    ARRAY_STRING,
    ARRAY_ENUM,
    ARRAY_BOOLEAN,
    ARRAY_INT64,
    ARRAY_INT32,
    ARRAY_FLOAT,
    ARRAY_DOUBLE,
    ARRAY_OBJECT_REF,
    ARRAY_IMPL,
    ARRAY_ANY,
    ARRAY_MAP_STRING,
    ARRAY_MAP_ENUM,
    ARRAY_MAP_BOOLEAN,
    ARRAY_MAP_INT64,
    ARRAY_MAP_INT32,
    ARRAY_MAP_FLOAT,
    ARRAY_MAP_DOUBLE,
    ARRAY_MAP_OBJECT_REF,
    ARRAY_MAP_ANY,

    MULTI_DIMENSIONAL_SET,
    MULTI_DIMENSIONAL_MAP,
    MULTI_DIMENSIONAL_ARRAY,


    MULTI_DIMENSIONAL_IMPL,


    UNION,
    ANY_OF,
    ONE_OF,

    ANY,
    ;

    public static SchemaClassification fromSchema(String name, SchemaModel value) {
        if (value.get$ref() != null) {
            return SchemaClassification.REF;
        }

        if (value.getAllOf() != null) {
            return SchemaClassification.UNION;
        }

        if (value.getAnyOf() != null) {
            return SchemaClassification.ANY_OF;
        }

        if (value.getOneOf() != null) {
            return SchemaClassification.ONE_OF;
        }

        if (value.getType() == null) {
            value.setType("object"); //This appears to be implict...
        }

        switch (value.getType().toLowerCase()) {
            case "string":
                if (value.get$enum() != null) {
                    return SchemaClassification.ENUM;
                }
                return SchemaClassification.STRING;
            case "boolean":
                return SchemaClassification.BOOLEAN;
            case "number":
            case "integer":
                if (value.getFormat() == null) {
                    return SchemaClassification.INT32;
                }
                switch (value.getFormat().toLowerCase()) {
                    case "int64":
                        return SchemaClassification.INT64;
                    case "int32":
                        return SchemaClassification.INT32;
                    case "float":
                        return SchemaClassification.FLOAT;
                    case "double":
                        return SchemaClassification.DOUBLE;
                    default:
                        throw new IllegalArgumentException("Schema " + name +" unsupported integer format " + value.getFormat());
                }
            case "array":
                if (value.getItems() == null) {
                    throw new IllegalArgumentException("Schema " + name + " items is null when type is array");
                }

                SchemaClassification itemClass = fromSchema(name + ".items", value.getItems());

                if (value.isUniqueItems()) {
                    switch (itemClass) {
                        case STRING:
                            return SchemaClassification.SET_STRING;
                        case ENUM:
                            return SchemaClassification.SET_ENUM;
                        case INT64:
                            return SchemaClassification.SET_INT64;
                        case INT32:
                            return SchemaClassification.SET_INT32;
                        case FLOAT:
                            return SchemaClassification.SET_FLOAT;
                        case DOUBLE:
                            return SchemaClassification.SET_DOUBLE;
                        case BOOLEAN:
                            return SchemaClassification.SET_BOOLEAN;
                        case REF:
                            return SchemaClassification.SET_OBJECT_REF;
                        case OBJECT_IMPL:
                            return SchemaClassification.SET_IMPL;
                        case MAP_STRING:
                            return SchemaClassification.SET_MAP_STRING;
                        case MAP_ENUM:
                            return SchemaClassification.SET_MAP_ENUM;
                        case MAP_BOOLEAN:
                            return SchemaClassification.SET_MAP_BOOLEAN;
                        case MAP_INT64:
                            return SchemaClassification.SET_MAP_INT64;
                        case MAP_INT32:
                            return SchemaClassification.SET_MAP_INT32;
                        case MAP_FLOAT:
                            return SchemaClassification.SET_MAP_FLOAT;
                        case MAP_DOUBLE:
                            return SchemaClassification.SET_MAP_DOUBLE;
                        case MAP_OBJECT_REF:
                            return SchemaClassification.SET_MAP_OBJECT_REF;
                        case MAP_ANY:
                            return SchemaClassification.SET_MAP_ANY;
                        case MAP_ARRAY_STRING:
                        case MAP_ARRAY_OBJECT_REF:
                        case MAP_SET_ANY:
                        case MAP_SET_OBJECT_REF:
                        case MAP_SET_DOUBLE:
                        case MAP_SET_FLOAT:
                        case MAP_SET_INT32:
                        case MAP_SET_INT64:
                        case MAP_SET_BOOLEAN:
                        case MAP_SET_ENUM:
                        case MAP_SET_STRING:
                        case MAP_ARRAY_ANY:
                        case MAP_ARRAY_DOUBLE:
                        case MAP_ARRAY_FLOAT:
                        case MAP_ARRAY_INT32:
                        case MAP_ARRAY_INT64:
                        case MAP_ARRAY_BOOLEAN:
                        case MAP_ARRAY_ENUM:
                        case SET_STRING:
                        case ARRAY_MAP_ANY:
                        case ARRAY_MAP_OBJECT_REF:
                        case ARRAY_MAP_DOUBLE:
                        case ARRAY_MAP_FLOAT:
                        case ARRAY_MAP_INT32:
                        case ARRAY_MAP_INT64:
                        case ARRAY_MAP_BOOLEAN:
                        case ARRAY_MAP_ENUM:
                        case ARRAY_MAP_STRING:
                        case ARRAY_ANY:
                        case ARRAY_OBJECT_REF:
                        case ARRAY_DOUBLE:
                        case ARRAY_FLOAT:
                        case ARRAY_INT32:
                        case ARRAY_INT64:
                        case ARRAY_BOOLEAN:
                        case ARRAY_ENUM:
                        case ARRAY_STRING:
                        case SET_MAP_ANY:
                        case SET_MAP_OBJECT_REF:
                        case SET_MAP_DOUBLE:
                        case SET_MAP_FLOAT:
                        case SET_MAP_INT32:
                        case SET_MAP_INT64:
                        case SET_MAP_BOOLEAN:
                        case SET_MAP_ENUM:
                        case SET_MAP_STRING:
                        case MULTI_DIMENSIONAL_SET:
                        case MULTI_DIMENSIONAL_ARRAY:
                        case MULTI_DIMENSIONAL_MAP:
                        case SET_ANY:
                        case SET_OBJECT_REF:
                        case SET_BOOLEAN:
                        case SET_DOUBLE:
                        case SET_FLOAT:
                        case SET_INT32:
                        case SET_INT64:
                        case SET_ENUM:
                            return SchemaClassification.MULTI_DIMENSIONAL_SET;
                        case SET_IMPL:
                        case ARRAY_IMPL:
                        case MAP_IMPL:
                        case MULTI_DIMENSIONAL_IMPL:
                            return SchemaClassification.MULTI_DIMENSIONAL_IMPL;
                        case UNION:
                        case ONE_OF:
                        case ANY_OF:
                            return ARRAY_IMPL;
                        case ANY:
                            return SET_ANY;
                    }

                    throw new IllegalStateException("itemClass" + itemClass + " not yet implemented");
                }

                switch (itemClass) {
                    case STRING:
                        return SchemaClassification.ARRAY_STRING;
                    case ENUM:
                        return SchemaClassification.ARRAY_ENUM;
                    case INT64:
                        return SchemaClassification.ARRAY_INT64;
                    case INT32:
                        return SchemaClassification.ARRAY_INT32;
                    case FLOAT:
                        return SchemaClassification.ARRAY_FLOAT;
                    case DOUBLE:
                        return SchemaClassification.ARRAY_DOUBLE;
                    case BOOLEAN:
                        return SchemaClassification.ARRAY_BOOLEAN;
                    case REF:
                        return SchemaClassification.ARRAY_OBJECT_REF;
                    case ANY:
                        return SchemaClassification.ARRAY_ANY;
                    case UNION:
                    case ANY_OF:
                    case ONE_OF:
                    case OBJECT_IMPL:
                        return SchemaClassification.ARRAY_IMPL;
                    case MAP_STRING:
                        return SchemaClassification.ARRAY_MAP_STRING;
                    case MAP_ENUM:
                        return SchemaClassification.ARRAY_MAP_ENUM;
                    case MAP_BOOLEAN:
                        return SchemaClassification.ARRAY_MAP_BOOLEAN;
                    case MAP_INT64:
                        return SchemaClassification.ARRAY_MAP_INT64;
                    case MAP_INT32:
                        return SchemaClassification.ARRAY_MAP_INT32;
                    case MAP_FLOAT:
                        return SchemaClassification.ARRAY_MAP_FLOAT;
                    case MAP_DOUBLE:
                        return SchemaClassification.ARRAY_MAP_DOUBLE;
                    case MAP_OBJECT_REF:
                        return SchemaClassification.ARRAY_MAP_OBJECT_REF;
                    case MAP_IMPL:
                    case ARRAY_IMPL:
                    case SET_IMPL:
                    case MULTI_DIMENSIONAL_IMPL:
                        return SchemaClassification.MULTI_DIMENSIONAL_IMPL;
                    case ARRAY_STRING:
                    case ARRAY_INT64:
                    case ARRAY_INT32:
                    case ARRAY_FLOAT:
                    case ARRAY_DOUBLE:
                    case ARRAY_OBJECT_REF:
                    case ARRAY_MAP_STRING:
                    case ARRAY_MAP_BOOLEAN:
                    case ARRAY_MAP_INT64:
                    case ARRAY_MAP_INT32:
                    case ARRAY_MAP_FLOAT:
                    case ARRAY_MAP_DOUBLE:
                    case ARRAY_MAP_OBJECT_REF:
                    case ARRAY_MAP_ANY:
                    case SET_STRING:
                    case SET_INT64:
                    case SET_INT32:
                    case SET_FLOAT:
                    case SET_DOUBLE:
                    case SET_OBJECT_REF:
                    case MULTI_DIMENSIONAL_SET:
                    case MULTI_DIMENSIONAL_ARRAY:
                    case MULTI_DIMENSIONAL_MAP:
                    case SET_MAP_STRING:
                    case SET_MAP_BOOLEAN:
                    case SET_MAP_INT64:
                    case SET_MAP_INT32:
                    case SET_MAP_FLOAT:
                    case SET_MAP_DOUBLE:
                    case SET_MAP_OBJECT_REF:
                    case SET_MAP_ANY:
                        return SchemaClassification.MULTI_DIMENSIONAL_ARRAY;
                }

                throw new IllegalStateException("itemClass" + itemClass + " not yet implemented");
            case "object":
                if (value.getAdditionalProperties() != null) {
                    SchemaClassification addPropClass = fromSchema(name + ".additionalProperties", value.getAdditionalProperties());
                    switch (addPropClass) {
                        case STRING:
                            return SchemaClassification.MAP_STRING;
                        case ENUM:
                            return SchemaClassification.MAP_ENUM;
                        case INT64:
                            return SchemaClassification.MAP_INT64;
                        case INT32:
                            return SchemaClassification.MAP_INT32;
                        case FLOAT:
                            return SchemaClassification.MAP_FLOAT;
                        case DOUBLE:
                            return SchemaClassification.MAP_DOUBLE;
                        case BOOLEAN:
                            return SchemaClassification.MAP_BOOLEAN;
                        case REF:
                            return SchemaClassification.MAP_OBJECT_REF;
                        case ANY:
                            return SchemaClassification.MAP_ANY;
                        case MAP_ENUM:
                        case MAP_STRING:
                        case MAP_BOOLEAN:
                        case MAP_INT64:
                        case MAP_INT32:
                        case MAP_FLOAT:
                        case MAP_DOUBLE:
                        case MAP_OBJECT_REF:
                        case MAP_IMPL:
                        case MAP_ARRAY_STRING:
                        case MAP_ARRAY_ENUM:
                        case MAP_ARRAY_BOOLEAN:
                        case MAP_ARRAY_INT64:
                        case MAP_ARRAY_INT32:
                        case MAP_ARRAY_FLOAT:
                        case MAP_ARRAY_DOUBLE:
                        case MAP_ARRAY_OBJECT_REF:
                        case MAP_ARRAY_ANY:
                        case MULTI_DIMENSIONAL_SET:
                        case MULTI_DIMENSIONAL_ARRAY:
                        case MULTI_DIMENSIONAL_MAP:
                        case MAP_ANY:
                        case SET_MAP_STRING:
                        case SET_MAP_ENUM:
                        case SET_MAP_BOOLEAN:
                        case SET_MAP_INT64:
                        case SET_MAP_INT32:
                        case SET_MAP_FLOAT:
                        case SET_MAP_DOUBLE:
                        case SET_MAP_OBJECT_REF:
                        case SET_MAP_ANY:
                        case ARRAY_MAP_STRING:
                        case ARRAY_MAP_ENUM:
                        case ARRAY_MAP_BOOLEAN:
                        case ARRAY_MAP_INT64:
                        case ARRAY_MAP_INT32:
                        case ARRAY_MAP_FLOAT:
                        case ARRAY_MAP_DOUBLE:
                        case ARRAY_MAP_OBJECT_REF:
                        case MAP_SET_BOOLEAN:
                        case MAP_SET_ENUM:
                        case MAP_SET_STRING:
                        case MAP_SET_INT64:
                        case MAP_SET_INT32:
                        case MAP_SET_FLOAT:
                        case MAP_SET_DOUBLE:
                        case MAP_SET_ANY:
                        case MAP_SET_OBJECT_REF:
                        case ARRAY_MAP_ANY:
                            return MULTI_DIMENSIONAL_MAP;
                        case SET_STRING:
                            return SchemaClassification.MAP_SET_STRING;
                        case SET_ENUM:
                            return SchemaClassification.MAP_SET_ENUM;
                        case SET_INT64:
                            return SchemaClassification.MAP_SET_INT64;
                        case SET_INT32:
                            return SchemaClassification.MAP_SET_INT32;
                        case SET_FLOAT:
                            return SchemaClassification.MAP_SET_FLOAT;
                        case SET_DOUBLE:
                            return SchemaClassification.MAP_SET_DOUBLE;
                        case SET_BOOLEAN:
                            return SchemaClassification.MAP_SET_BOOLEAN;
                        case SET_OBJECT_REF:
                            return SchemaClassification.MAP_SET_OBJECT_REF;
                        case SET_ANY:
                            return SchemaClassification.MAP_SET_ANY;
                        case ARRAY_STRING:
                            return SchemaClassification.MAP_ARRAY_STRING;
                        case ARRAY_ENUM:
                            return SchemaClassification.MAP_ARRAY_ENUM;
                        case ARRAY_BOOLEAN:
                            return SchemaClassification.MAP_ARRAY_BOOLEAN;
                        case ARRAY_INT64:
                            return SchemaClassification.MAP_ARRAY_INT64;
                        case ARRAY_INT32:
                            return SchemaClassification.MAP_ARRAY_INT32;
                        case ARRAY_FLOAT:
                            return SchemaClassification.MAP_ARRAY_FLOAT;
                        case ARRAY_DOUBLE:
                            return SchemaClassification.MAP_ARRAY_DOUBLE;
                        case ARRAY_OBJECT_REF:
                            return SchemaClassification.MAP_ARRAY_OBJECT_REF;
                        case ARRAY_IMPL:
                            return SchemaClassification.MULTI_DIMENSIONAL_IMPL;
                        case ARRAY_ANY:
                            return SchemaClassification.MAP_ARRAY_ANY;
                        case MULTI_DIMENSIONAL_IMPL:
                            return SchemaClassification.MULTI_DIMENSIONAL_IMPL;
                        case UNION:
                        case ANY_OF:
                        case ONE_OF:
                        case OBJECT_IMPL:
                            return SchemaClassification.MAP_IMPL;
                        default:
                            throw new IllegalStateException("Not implemented yet map additionalProperties " + addPropClass);
                    }
                }

                if (value.getProperties() == null) {
                    return SchemaClassification.ANY;
                }
                return SchemaClassification.OBJECT_IMPL;
            default:
                throw new IllegalArgumentException("Schema " + name +" unsupported type " + value.getType());
        }
    }
}
