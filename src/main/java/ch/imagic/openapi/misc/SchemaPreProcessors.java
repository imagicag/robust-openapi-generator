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

import java.util.*;

public class SchemaPreProcessors {

    public static void preProcess(GenerationContext context) {
        ensureComponentsObjectIsPopulated(context);
        ensureOpIds(context);
        moveRequestBodies(context);
        moveResponseBodies(context);
        moveParameters(context);
        moveHeader(context);
        moveHeaderSchemas(context);

        boolean working;
        do {
            working = false;
            working |= bustObjects(context);
            working |= bustArrays(context);
            working |= bustPoly(context);
            working |= bustUnions(context);
        } while (working);

        bustRefs(context);
    }

    public static void ensureComponentsObjectIsPopulated(GenerationContext context) {
        if (context.getModel().getComponents() == null) {
            context.getModel().setComponents(new ComponentsModel());
        }

        ComponentsModel components = context.getModel().getComponents();
        if (components.getSchemas() == null) {
            components.setSchemas(new HashMap<>());
        }

        if (components.getHeaders() == null) {
            components.setHeaders(new HashMap<>());
        }

        if (components.getResponses() == null) {
            components.setResponses(new HashMap<>());
        }

        if (components.getRequestBodies() == null) {
            components.setRequestBodies(new HashMap<>());
        }

        if (components.getParameters() == null) {
            components.setParameters(new HashMap<>());
        }
    }

    public static void ensureOpIds(GenerationContext context) {
        Set<String> opIds = new HashSet<>();

        for (Map.Entry<String, Map<String, PathModel>> e : context.getModel().getPaths().entrySet()) {
            for (Map.Entry<String, PathModel> p : e.getValue().entrySet()) {
                String opId = p.getValue().getOperationId();
                if (opId == null) {
                    continue;
                }

                String s = Util.mangleOpId(opId);
                if (!s.equals(opId)) {
                    System.out.println("WARNING operation id '" + opId + "' contains symbols that cannot be used in java identifiers, or is otherwise unsuitable, will mangle it to '" + s+ "' ");
                    p.getValue().setOperationId(s);
                }

                int cnt = 0;
                String theOp =  p.getValue().getOperationId();
                if (!opIds.add(theOp)) {
                    while(!opIds.add(theOp + cnt)) {
                        cnt++;
                    }

                    System.out.println("WARNING operation id '" + theOp + "' is duplicated, will mangle it to '" + theOp + cnt + "' ");
                    p.getValue().setOperationId(theOp + cnt);
                }
            }
        }


        for (Map.Entry<String, Map<String, PathModel>> e : context.getModel().getPaths().entrySet()) {
            for (Map.Entry<String, PathModel> p : e.getValue().entrySet()) {
                if (p.getValue().getOperationId() == null || p.getValue().getOperationId().trim().isEmpty()) {
                    String nid = "operation" + (context.nextOpId());
                    while(opIds.contains(nid)) {
                        nid = "operation" + (context.nextOpId());
                    }
                    System.out.println("WARNING path " + e.getKey() + "->" + p.getKey() + " has no operation id, will assign it operation id '" + nid + "' " +
                            "the assignment is dependant on the order of the json file! " +
                            "This may cause unexpected problems in the future! " +
                            "Please assign every operation a id.");
                    p.getValue().setOperationId(nid);
                    opIds.add(nid);
                }
            }
        }
    }

    public static void moveRequestBodies(GenerationContext context) {
        if (context.getModel().getPaths() == null) {
            return;
        }

        for (Map.Entry<String, Map<String, PathModel>> e : context.getModel().getPaths().entrySet()) {
            for (Map.Entry<String, PathModel> p : e.getValue().entrySet()) {
                String method = p.getKey();
                PathModel model = p.getValue();

                RequestBodyModel requestBody = model.getRequestBody();
                if (requestBody == null) {
                    continue;
                }

                if (requestBody.get$ref() != null) {
                    if (context.findRequestBody(requestBody.get$ref()) == null) {
                        throw new IllegalArgumentException("Invalid RequestBody schema reference " + requestBody.get$ref());
                    }
                    continue;
                }
                Map<String, RequestBodyModel> responses = context.getModel().getComponents().getRequestBodies();


                String id = Util.capitalize(model.getOperationId()) + Util.capitalize(method) + "RequestBody";
                if (responses.containsKey(id)) {
                    int cnt = 0;
                    while (responses.containsKey(id + cnt)) {
                        cnt++;
                    }
                    id = id + cnt;
                }

                responses.put(id, requestBody);
                RequestBodyModel link = new RequestBodyModel();
                link.set$ref("#/components/requestBodies/" + id);
                model.setRequestBody(link);
            }
        }

        for (Map.Entry<String, RequestBodyModel> e : context.getModel().getComponents().getRequestBodies().entrySet()) {
            String id = e.getKey();
            RequestBodyModel requestBody = e.getValue();
            if (requestBody.getContent() == null) {
                continue;
            }

            PathSchemaModel requestBodySchemaModel = requestBody.getContent().get("application/json");
            if (requestBodySchemaModel == null) {
                continue;
            }


            SchemaModel actualSchema = requestBodySchemaModel.getSchema();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(id + "->application/json->requestBody", actualSchema);
            switch (schemaClassification) {
                case STRING:
                case INT64:
                case INT32:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case MAP_STRING:
                case MAP_BOOLEAN:
                case MAP_INT64:
                case MAP_INT32:
                case MAP_FLOAT:
                case MAP_DOUBLE:
                case MAP_OBJECT_REF:
                case MAP_IMPL:
                case REF:
                case ARRAY_STRING:
                case ARRAY_BOOLEAN:
                case ARRAY_INT64:
                case ARRAY_INT32:
                case ARRAY_FLOAT:
                case ARRAY_DOUBLE:
                case ARRAY_OBJECT_REF:
                    continue;
                case UNION:
                case OBJECT_IMPL: {
                    String itemName = id;

                    SchemaModel refModel = new SchemaModel();

                    if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                        int cnt = 0;
                        while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                            cnt++;
                        }
                        itemName += cnt;
                    }

                    refModel.set$ref("#/components/schemas/" + itemName);
                    context.getModel().getComponents().getSchemas().put(itemName, actualSchema);
                    requestBodySchemaModel.setSchema(refModel);
                    break;
                }
                case ARRAY_IMPL: {
                    String itemName = id + "Item";

                    SchemaModel refModel = new SchemaModel();

                    if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                        int cnt = 0;
                        while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                            cnt++;
                        }
                        itemName += cnt;
                    }

                    refModel.set$ref("#/components/schemas/" + itemName);
                    context.getModel().getComponents().getSchemas().put(itemName, actualSchema.getItems());
                    actualSchema.setItems(refModel);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("not yet implemented" + schemaClassification);
            }
        }
    }

    public static void moveResponseBodies(GenerationContext context) {
        if (context.getModel().getPaths() == null) {
            return;
        }

        for (Map.Entry<String, Map<String, PathModel>> e : context.getModel().getPaths().entrySet()) {
            for (Map.Entry<String, PathModel> p : e.getValue().entrySet()) {
                String method = p.getKey();
                PathModel model = p.getValue();

                if (model.getResponses() == null) {
                    continue;
                }

                for (Map.Entry<String, ResponseModel> r : model.getResponses().entrySet()) {
                    String statusCode = r.getKey();
                    if (r.getValue().get$ref() != null) {
                        if (context.findResponse(r.getValue().get$ref()) == null) {
                            throw new IllegalArgumentException("Invalid response schema reference " + r.getValue().get$ref());
                        }
                        continue;
                    }
                    Map<String, ResponseModel> responses = context.getModel().getComponents().getResponses();


                    String id = Util.capitalize(model.getOperationId()) + Util.capitalize(method) + "Http" + statusCode + "Response";
                    if (responses.containsKey(id)) {
                        int cnt = 0;
                        while (responses.containsKey(id + cnt)) {
                            cnt++;
                        }
                        id = id + cnt;
                    }

                    responses.put(id, r.getValue());
                    ResponseModel link = new ResponseModel();
                    link.set$ref("#/components/responses/" + id);
                    r.setValue(link);
                }
            }
        }

        for (Map.Entry<String, ResponseModel> r : context.getModel().getComponents().getResponses().entrySet()) {
            String id = r.getKey();
            ResponseModel responseBody = r.getValue();
            if (responseBody == null || responseBody.getContent() == null) {
                continue;
            }

            PathSchemaModel responseBodySchema = responseBody.getContent().get("application/json");
            if (responseBodySchema == null) {
                continue;
            }


            SchemaModel actualSchema = responseBodySchema.getSchema();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema("response " + id + "->application/json->responseBody", actualSchema);
            switch (schemaClassification) {
                case STRING:
                case ENUM:
                case INT64:
                case INT32:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case MAP_STRING:
                case MAP_BOOLEAN:
                case MAP_INT64:
                case MAP_INT32:
                case MAP_FLOAT:
                case MAP_DOUBLE:
                case MAP_OBJECT_REF:
                case MAP_IMPL:
                case REF:
                case ARRAY_STRING:
                case ARRAY_BOOLEAN:
                case ARRAY_INT64:
                case ARRAY_INT32:
                case ARRAY_FLOAT:
                case ARRAY_DOUBLE:
                case ARRAY_OBJECT_REF:
                case ANY:
                    continue;
                case UNION:
                case OBJECT_IMPL: {
                    String itemName = id +"Body";

                    SchemaModel refModel = new SchemaModel();

                    if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                        int cnt = 0;
                        while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                            cnt++;
                        }
                        itemName += cnt;
                    }

                    refModel.set$ref("#/components/schemas/" + itemName);
                    context.getModel().getComponents().getSchemas().put(itemName, actualSchema);
                    responseBodySchema.setSchema(refModel);
                    break;
                }
                case ARRAY_IMPL: {
                    String itemName = id + "BodyItem";

                    SchemaModel refModel = new SchemaModel();

                    if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                        int cnt = 0;
                        while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                            cnt++;
                        }
                        itemName += cnt;
                    }

                    refModel.set$ref("#/components/schemas/" + itemName);
                    context.getModel().getComponents().getSchemas().put(itemName, actualSchema.getItems());
                    actualSchema.setItems(refModel);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("not yet implemented" + schemaClassification);
            }
        }
    }

    public static boolean bustPoly(GenerationContext context) {
        if (context.getModel().getComponents() == null) {
            return false;
        }

        if (context.getModel().getComponents().getSchemas() == null) {
            return false;
        }

        for (Map.Entry<String, SchemaModel> e : context.getModel().getComponents().getSchemas().entrySet()) {
            String name = e.getKey();
            SchemaModel schema = e.getValue();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
            if (schemaClassification != SchemaClassification.ONE_OF  && schemaClassification != SchemaClassification.ANY_OF) {
                continue;
            }

            SchemaModel[] content = schema.getOneOf();
            if (content == null) {
                content = schema.getAnyOf();
            }

            for (int i = 0; i < content.length; i++) {
                SchemaClassification innerClass = SchemaClassification.fromSchema(name, content[i]);
                switch (innerClass) {
                    case REF:
                        break;
                    case OBJECT_IMPL:
                    case UNION:
                    case ANY_OF:
                    case ONE_OF:
                        SchemaModel refModel = new SchemaModel();

                        String itemName = name + "Poly";

                        if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                            int cnt = 0;
                            while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                                cnt++;
                            }
                            itemName += cnt;
                        }

                        refModel.set$ref("#/components/schemas/" + itemName);
                        context.getModel().getComponents().getSchemas().put(itemName, content[i]);
                        content[i] = refModel;
                        return true;
                    default:
                        throw new IllegalArgumentException("Cannot poly with " + innerClass);
                }
            }
        }

        return false;
    }

    public static boolean bustObjects(GenerationContext context) {
        for (Map.Entry<String, SchemaModel> e : context.getModel().getComponents().getSchemas().entrySet()) {
            String name = e.getKey();
            SchemaModel schema = e.getValue();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
            if (schemaClassification != SchemaClassification.OBJECT_IMPL) {
                continue;
            }

            Map<String, SchemaModel> properties = schema.getProperties();
            for (Map.Entry<String, SchemaModel> p : properties.entrySet()) {
                String propName = p.getKey();
                SchemaModel propSchema = p.getValue();
                SchemaClassification propClass = SchemaClassification.fromSchema(name + "." + propName, propSchema);
                if (propClass != SchemaClassification.UNION && propClass != SchemaClassification.OBJECT_IMPL) {
                    continue;
                }
                SchemaModel refModel = new SchemaModel();

                String itemName = name + Util.capitalize(propName) + "Property";

                if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                    int cnt = 0;
                    while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                        cnt++;
                    }
                    itemName += cnt;
                }

                refModel.set$ref("#/components/schemas/" + itemName);
                context.getModel().getComponents().getSchemas().put(itemName, propSchema);
                properties.put(propName, refModel);
                return true;
            }

            for (Map.Entry<String, SchemaModel> p : properties.entrySet()) {
                String propName = p.getKey();
                SchemaModel propSchema = p.getValue();
                SchemaClassification propClass = SchemaClassification.fromSchema(name + "." + propName, propSchema);
                if (propClass != SchemaClassification.ARRAY_IMPL) {
                    continue;
                }
                SchemaModel refModel = new SchemaModel();

                String itemName = name + Util.capitalize(propName) + "Item";

                if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                    int cnt = 0;
                    while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                        cnt++;
                    }
                    itemName += cnt;
                }

                refModel.set$ref("#/components/schemas/" + itemName);
                context.getModel().getComponents().getSchemas().put(itemName, propSchema.getItems());
                propSchema.setItems(refModel);
                return true;
            }
        }

        return false;
    }

    public static boolean bustArrays(GenerationContext context) {
        if (context.getModel().getComponents() == null) {
            return false;
        }

        if (context.getModel().getComponents().getSchemas() == null) {
            return false;
        }

        for (Map.Entry<String, SchemaModel> e : context.getModel().getComponents().getSchemas().entrySet()) {
            String name = e.getKey();
            SchemaModel schema = e.getValue();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
            if (schemaClassification != SchemaClassification.ARRAY_IMPL) {
                continue;
            }

            SchemaModel refModel = new SchemaModel();

            String itemName = name + "Item";

            if (context.getModel().getComponents().getSchemas().containsKey(itemName)) {
                int cnt = 0;
                while(context.getModel().getComponents().getSchemas().containsKey(itemName + cnt)) {
                    cnt++;
                }
                itemName += cnt;
            }

            refModel.set$ref("#/components/schemas/" + itemName);
            context.getModel().getComponents().getSchemas().put(itemName, schema.getItems());
            schema.setItems(refModel);
            return true;
        }

        return false;
    }

    public static boolean bustRefs(GenerationContext context) {
        if (context.getModel().getComponents() == null) {
            return false;
        }

        if (context.getModel().getComponents().getSchemas() == null) {
            return false;
        }

        boolean foundAny = false;
        int count = 0xFF;

        while(count > 0) {
            count--;
            boolean found = false;
            for (String name : new ArrayList<>(context.getModel().getComponents().getSchemas().keySet())) {
                SchemaModel schema = context.getModel().getComponents().getSchemas().get(name);
                SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
                if (schemaClassification != SchemaClassification.REF) {
                    continue;
                }
                found = true;
                foundAny = true;

                String ref = schema.get$ref();
                SchemaModel model = context.findSchema(ref);
                if (model == null) {
                    throw new IllegalArgumentException("Referenced model not found " + ref);
                }
                context.getModel().getComponents().getSchemas().put(name, model);
            }

            if (!found) {
                return foundAny;
            }
        }

        throw new IllegalStateException("Too many references! circular reference?");
    }

    public static boolean bustUnions(GenerationContext context) {
        boolean containsUnionOfUnion = false;

        if (context.getModel().getComponents() == null) {
            return false;
        }

        if (context.getModel().getComponents().getSchemas() == null) {
            return false;
        }

        for (Map.Entry<String, SchemaModel> e : context.getModel().getComponents().getSchemas().entrySet()) {
            String name = e.getKey();
            SchemaModel schema = e.getValue();
            SchemaClassification schemaClassification = SchemaClassification.fromSchema(name, schema);
            if (schemaClassification != SchemaClassification.UNION) {
                continue;
            }

            SchemaModel[] allOf = schema.getAllOf();


            for (int i = 0; i < allOf.length; i++) {
                SchemaClassification unionClassification = SchemaClassification.fromSchema(name + ".union." + i, allOf[i]);
                switch (unionClassification) {
                    case REF:
                        //We will bust this union later!
                        break;
                    case ANY_OF:
                    case OBJECT_IMPL:
                    case UNION:
                        //We can make progress towards busting the union by further dividing it
                        SchemaModel refModel = new SchemaModel();

                        String unionName = name + "Union";

                        if (context.getModel().getComponents().getSchemas().containsKey(unionName)) {
                            int cnt = 0;
                            while(context.getModel().getComponents().getSchemas().containsKey(unionName + cnt)) {
                                cnt++;
                            }
                            unionName += cnt;
                        }

                        refModel.set$ref("#/components/schemas/" + unionName);
                        context.getModel().getComponents().getSchemas().put(unionName, allOf[i]);
                        allOf[i] = refModel;
                        return true;
                    default:
                        throw new IllegalArgumentException("Cannot union with type " + unionClassification + " in union " + name);
                }
            }

            List<SchemaModel> unionCompoents = new ArrayList<>();

            for (int i = 0; i < allOf.length; i++) {
                SchemaClassification unionClassification = SchemaClassification.fromSchema(name + ".union." + i, allOf[i]);
                if (unionClassification != SchemaClassification.REF) {
                    throw new IllegalStateException("Can not bust union because type is not ref at this point. Is " + unionClassification + " in union " + name);
                }

                String ref = allOf[i].get$ref();
                SchemaModel model = context.findSchema(ref);
                if (model == null) {
                    throw new IllegalArgumentException("Cannot find referenced model " + ref + " when busting union " + name);
                }
                SchemaClassification targetClass = SchemaClassification.fromSchema(name + ".union." + i + ".target", model);
                if (targetClass == SchemaClassification.UNION) {
                    containsUnionOfUnion = true;
                    break;
                }
                if (targetClass != SchemaClassification.OBJECT_IMPL) {
                    throw new IllegalArgumentException("Cannot union with type " + unionClassification + " in union " + name);
                }

                unionCompoents.add(model);
            }

            if (containsUnionOfUnion) {
                continue;
            }

            Map<String, SchemaModel> properties = new LinkedHashMap<>();
            Set<String> required = new LinkedHashSet<>();

            for (SchemaModel model : unionCompoents) {
                if (model.getProperties() != null) {
                    for (Map.Entry<String, SchemaModel> newProp : model.getProperties().entrySet()) {
                        SchemaModel eprop = properties.get(newProp.getKey());
                        if (eprop == null) {
                            continue;
                        }

                        SchemaClassification newPropClass = SchemaClassification.fromSchema(name + "." + newProp.getKey(), newProp.getValue());

                        if (SchemaClassification.fromSchema(name + "." + newProp.getKey(), eprop) != newPropClass) {
                            throw new IllegalArgumentException("Property is clashing between two union members " + newProp.getKey() + " in union " + name);
                        }

                        if (newPropClass == SchemaClassification.REF && !Objects.equals(newProp.getValue().get$ref(), eprop.get$ref())) {
                            throw new IllegalArgumentException("Property is clashing between two union members " + newProp.getKey() + " in union " + name + " they refer to two different schemas");
                        }
                    }

                    properties.putAll(model.getProperties());
                }
                if (model.getRequired() != null) {
                    required.addAll(Arrays.asList(model.getRequired()));
                }
            }

            SchemaModel bustedModel = new SchemaModel();
            bustedModel.setType("object");
            bustedModel.setProperties(properties);
            bustedModel.setRequired(required.toArray(new String[required.size()]));
            context.getModel().getComponents().getSchemas().put(name, bustedModel);
            return true;
        }

        if (containsUnionOfUnion) {
            throw new IllegalStateException("Cyclic union detected");
        }

        return false;
    }


    public static void moveHeader(GenerationContext context) {
        for (Map.Entry<String, ResponseModel> e : context.getModel().getComponents().getResponses().entrySet()) {
            if (e.getValue().getHeaders() == null) {
                continue;
            }

            for (Map.Entry<String, HeaderModel> p : e.getValue().getHeaders().entrySet()) {
                String headerName = p.getKey();
                HeaderModel model = p.getValue();

                if (model.get$ref() != null) {
                    if (context.findParameter(model.get$ref()) == null) {
                        throw new IllegalArgumentException("Invalid header reference " + model.get$ref());
                    }
                    continue;
                }

                Map<String, HeaderModel> headers = context.getModel().getComponents().getHeaders();


                String id = e.getKey() + Util.capitalize(headerName);
                if (headers.containsKey(id)) {
                    int cnt = 0;
                    while (headers.containsKey(id + cnt)) {
                        cnt++;
                    }
                    id = id + cnt;
                }

                headers.put(id, model);
                HeaderModel link = new HeaderModel();
                link.set$ref("#/components/headers/" + id);
                p.setValue(link);
            }
        }
    }

    public static void moveHeaderSchemas(GenerationContext context) {
        for (Map.Entry<String, HeaderModel> e : context.getModel().getComponents().getHeaders().entrySet()) {
            String headerName = e.getKey();
            HeaderModel model = e.getValue();
            SchemaClassification headerClass = SchemaClassification.fromSchema("header " + headerName, model.getSchema());
            switch (headerClass) {
                case OBJECT_IMPL:
                case ARRAY_IMPL:
                case MAP_IMPL:
                case UNION:
                case ONE_OF:
                case ANY_OF:
                    break;
                default:
                    continue;
            }

            Map<String, SchemaModel> responses = context.getModel().getComponents().getSchemas();


            String id = Util.capitalize(headerName) + "Header";
            if (responses.containsKey(id)) {
                int cnt = 0;
                while (responses.containsKey(id + cnt)) {
                    cnt++;
                }
                id = id + cnt;
            }

            responses.put(id, model.getSchema());
            SchemaModel link = new SchemaModel();
            link.set$ref("#/components/responses/" + id);
            model.setSchema(link);
        }
    }

    public static void moveParameters(GenerationContext context) {
        for (Map.Entry<String, Map<String, PathModel>> e : context.getModel().getPaths().entrySet()) {
            for (Map.Entry<String, PathModel> p : e.getValue().entrySet()) {
                String method = p.getKey();
                PathModel model = p.getValue();

                ParameterModel[] parameters = model.getParameters();
                if (parameters == null || parameters.length == 0) {
                    continue;
                }

                for (int i = 0; i < parameters.length; i++) {
                    ParameterModel parameter = parameters[i];
                    if (parameter.get$ref() != null) {
                        if (context.findParameter(parameter.get$ref()) == null) {
                            throw new IllegalArgumentException("Invalid parameter schema reference " + parameter.get$ref());
                        }
                        continue;
                    }
                    Map<String, ParameterModel> params = context.getModel().getComponents().getParameters();


                    String id = Util.capitalize(model.getOperationId()) + Util.capitalize(method);
                    if (params.containsKey(id)) {
                        int cnt = 0;
                        while (params.containsKey(id + cnt)) {
                            cnt++;
                        }
                        id = id + cnt;
                    }

                    params.put(id, parameter);
                    ParameterModel link = new ParameterModel();
                    link.set$ref("#/components/parameters/" + id);
                    parameters[i] = link;
                }
            }
        }

    }
}
