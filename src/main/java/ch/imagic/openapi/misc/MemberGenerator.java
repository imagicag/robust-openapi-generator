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

public class MemberGenerator {

    public static void generateTrivialMember(GenerationContext context, String fullyQualified, String propName, String mangledName, String classname, String memberType, SchemaModel propSchema) {

        //Util.pushJavaDoc(context, fullyQualified, propSchema);

        if (!propName.equals(mangledName)) {
            if (context.jackson()) {
                context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonProperty(\"" +  Util.escapeForSourceCode(propName) +  "\")");
            }
            if (context.gson()) {
                context.push(fullyQualified, "@com.google.gson.annotations.SerializedName(\"" + Util.escapeForSourceCode(propName) + "\")");
            }
        }

        context.push(fullyQualified, "private "+memberType+" " + mangledName + ";");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public "+memberType+" get" + Util.capitalize(mangledName) + "() {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return this." + mangledName + ";");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public void set" + Util.capitalize(mangledName) + "("+memberType+" value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " with" + Util.capitalize(mangledName) + "("+memberType+" value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");
    }

    public static void generateSetMember(GenerationContext context, String fullyQualified, String propName, String mangledName, String classname, String memberType, SchemaModel propSchema) {
        if (!propName.equals(mangledName)) {
            if (context.jackson()) {
                context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonProperty(\"" +  Util.escapeForSourceCode(propName) +  "\")");
            }
        }

        //Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified, "private java.util.Set<"+memberType+"> " + mangledName + ";");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public java.util.Set<"+memberType+"> get" + Util.capitalize(mangledName) + "() {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return this." + mangledName + ";");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public void set" + Util.capitalize(mangledName) + "(java.util.Set<"+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " with" + Util.capitalize(mangledName) + "(java.util.Set<"+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " with" + Util.capitalize(mangledName) + "("+memberType+"... value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (value == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.LinkedHashSet<>();");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + " = new java.util.LinkedHashSet<>(java.util.Arrays.asList(value));");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " add" + Util.capitalize(mangledName) + "("+memberType+" value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.LinkedHashSet<>();");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + ".add(value);");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public boolean contains" + Util.capitalize(mangledName) + "("+memberType+" value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return false;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "return this." + mangledName + ".contains(value);");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " addAll" + Util.capitalize(mangledName) + "(java.util.Collection<"+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.LinkedHashSet<>();");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "if ("+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + ".addAll(value);");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");
    }

    public static void generateListMember(GenerationContext context, String fullyQualified, String propName, String mangledName, String classname, String memberType, SchemaModel propSchema) {
        if (!propName.equals(mangledName)) {
            if (context.jackson()) {
                context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonProperty(\"" +  Util.escapeForSourceCode(propName) +  "\")");
            }
        }

        //Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified, "private java.util.List<"+memberType+"> " + mangledName + ";");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public java.util.List<"+memberType+"> get" + Util.capitalize(mangledName) + "() {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return this." + mangledName + ";");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public void set" + Util.capitalize(mangledName) + "(java.util.List<"+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " with" + Util.capitalize(mangledName) + "(java.util.List<"+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " with" + Util.capitalize(mangledName) + "("+memberType+"... value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (value == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.ArrayList<>();");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + " = new java.util.ArrayList<>(java.util.Arrays.asList(value));");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " add" + Util.capitalize(mangledName) + "("+memberType+" value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.ArrayList<>();");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + ".add(value);");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " addAll" + Util.capitalize(mangledName) + "(java.util.Collection<"+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.ArrayList<>();");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "if ("+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + ".addAll(value);");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");
    }

    public static void generateMapMember(GenerationContext context, String fullyQualified, String propName, String mangledName, String classname, String memberType, SchemaModel propSchema) {
        if (!propName.equals(mangledName)) {
            if (context.jackson()) {
                context.push(fullyQualified, "@com.fasterxml.jackson.annotation.JsonProperty(\"" +  Util.escapeForSourceCode(propName) +  "\")");
            }
        }

        //Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified, "private java.util.Map<String, "+memberType+"> " + mangledName + ";");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public java.util.Map<String, "+memberType+"> get" + Util.capitalize(mangledName) + "() {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return this." + mangledName + ";");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public void set" + Util.capitalize(mangledName) + "(java.util.Map<String, "+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " with" + Util.capitalize(mangledName) + "(java.util.Map<String, "+memberType+"> value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this." + mangledName + " = value;");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");


        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + classname + " put" + Util.capitalize(mangledName) + "(String key, "+memberType+" value) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "this."+mangledName+" = new java.util.LinkedHashMap<>();");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "this." + mangledName + ".put(key, value);");
        context.push(fullyQualified, "return this;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");

        Util.pushJavaDoc(context, fullyQualified, propSchema);
        context.push(fullyQualified,"public " + memberType + " get" + Util.capitalize(mangledName) + "(String key) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "if (this."+mangledName + " == null) {");
        context.addIndent(fullyQualified);
        context.push(fullyQualified, "return null;");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "return this." + mangledName + ".get(key);");
        context.subIndent(fullyQualified);
        context.push(fullyQualified,"}");
        context.push(fullyQualified, "");
    }
}
