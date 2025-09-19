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

import ch.imagic.openapi.model.PathModel;

public class ResponseParserGenerator {

    public static void generateParseEnumHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName, String enumName) {
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = " + enumName + ".valueOf(rawHdr);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch (Exception e) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " contains an illegal variant that is not part of the enum\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseEnumArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName, String enumName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);

        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(" + enumName + ".valueOf(rawHdr));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch (Exception e) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " contains an illegal variant that is not part of the enum\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseEnumMapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName, String enumName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Enum Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");

        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".put(a.substring(0, idx), " + enumName + ".valueOf(a.substring(idx + 1)));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch (Exception e) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " contains an illegal variant that is not part of the enum\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);

        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseEnumMapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName, String enumName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Enum Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".put(rawSplit[i], " + enumName + ".valueOf(rawSplit[i + 1]));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(Exception e){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " contains an illegal variant that is not part of the enum\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }


    public static void generateParseDoubleHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = Double.parseDouble(rawHdr);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Double\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseFloatHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = Float.parseFloat(rawHdr);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt32Header(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = Integer.parseInt(rawHdr);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Integer\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt64Header(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = Long.parseLong(rawHdr);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseBooleanHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "if (\"true\".equalsIgnoreCase(rawHdr)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = true;");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else if (\"false\".equalsIgnoreCase(rawHdr)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = false;");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Boolean\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseStringArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "if (rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+"= java.util.Arrays.stream(rawHdr.split(\",\").collect(java.util.stream.Collectors.toList());");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseBooleanArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "if (\"true\".equalsIgnoreCase(a)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(true);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else if (\"false\".equalsIgnoreCase(a)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(false);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Boolean Array\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt64ArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(Long.parseLong(a));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Array\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt32ArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(Integer.parseInt(a));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Array\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseFloatArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(Float.parseFloat(a));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Array\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseDoubleArrayHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.ArrayList<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this." + fieldName + ".add(Double.parseDouble(a));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Array\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseStringMapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid String Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], rawSplit[i + 1]);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseStringMapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid String Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), a.substring(idx + 1));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseBooleanMapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Boolean Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String value = rawSplit[i + 1];");
        ctx.push(clazz, "if (\"true\".equalsIgnoreCase(value)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], Boolean.TRUE);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else if (\"false\".equalsIgnoreCase(value)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], Boolean.FALSE);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Boolean Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseBooleanMapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Boolean Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "String value = a.substring(idx + 1);");
        ctx.push(clazz, "if (\"true\".equalsIgnoreCase(value)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), Boolean.TRUE);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else if (\"false\".equalsIgnoreCase(value)) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), Boolean.FALSE);");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} else {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Boolean Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt32MapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Integer Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], Integer.parseInt(rawSplit[i + 1]));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Integer Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt32MapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Integer Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), Integer.parseInt(a.substring(idx + 1)));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Integer Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt64MapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], Integer.parseInt(rawSplit[i + 1]));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseInt64MapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, ".forEach(a -> {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), Long.parseLong(a.substring(idx + 1)));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Long Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseFloatMapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], Float.parseFloat(rawSplit[i + 1]));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseFloatMapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), Float.parseFloat(a.substring(idx + 1)));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseDoubleMapHeader(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "String[] rawSplit = rawHdr.split(\",\");");
        ctx.push(clazz, "if ((rawSplit.length & 1) == 1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "for (int i = 0; i < rawSplit.length; i += 2) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(rawSplit[i], Double.parseDouble(rawSplit[i + 1]));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Float Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }

    public static void generateParseDoubleMapHeaderExplode(GenerationContext ctx, PathModel model, String clazz, String fieldName, String headerName) {
        ctx.push(clazz, "this." + fieldName + " = new java.util.LinkedHashMap<>();");
        ctx.push(clazz, "if (!rawHdr.equals(\"\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "for (String a : rawHdr.split(\",\")) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "int idx = a.indexOf(\"=\");");
        ctx.push(clazz, "if (idx == -1) {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Double Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.push(clazz, "try {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "this."+fieldName+".put(a.substring(0, idx), Double.parseDouble(a.substring(idx + 1)));");
        ctx.subIndent(clazz);
        ctx.push(clazz, "} catch(java.lang.NumberFormatException nfe){ {");
        ctx.addIndent(clazz);
        ctx.push(clazz, "throw new " + ctx.qualifyCommonApiClass("ApiException") + "(\"" + model.getOperationId() + "\", statusCode, headers, body, \"Header " + Util.escapeForSourceCode(headerName) + " is not a valid Double Map\");");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
        ctx.subIndent(clazz);
        ctx.push(clazz, "}");
    }
}
