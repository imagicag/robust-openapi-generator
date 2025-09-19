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

import ch.imagic.openapi.model.ParameterModel;

public class RequestParamGenerator {

    public static void generatePrimitiveParameter(GenerationContext ctx, String apiClassName, ParameterModel prm,  String parameterGetterName) {
        String rawParamName = Util.escapeForSourceCode(prm.getName());
        switch (prm.getIn()) {
            case "query":
                switch (String.valueOf(prm.getStyle())) {
                    case "null":
                    case "form":
                        ctx.push(apiClassName, "context.addQueryParam(\""+ rawParamName +"\", String.valueOf(param."+parameterGetterName+"()));");
                        return;
                    case "spaceDelimited":
                        throw new IllegalArgumentException("spaceDelimited primitive query parameters are undefined");
                    case "pipeDelimited":
                        throw new IllegalArgumentException("pipeDelimited primitive query parameters are undefined");
                    default:
                        throw new IllegalArgumentException("Unsupported query parameter style " + prm.getStyle() + " for primitive query parameter");
                }
            case "path":
                switch (String.valueOf(prm.getStyle())) {
                    case "matrix":
                        ctx.push(apiClassName, "context.addPathParam(\""+ rawParamName +"\", \";"+ rawParamName +"=\" + String.valueOf(param."+parameterGetterName+"()));");
                        return;
                    case "label":
                        //TODO what about numbers with dots?
                        ctx.push(apiClassName, "context.addPathParam(\""+ rawParamName +"\", \".\" + String.valueOf(param."+parameterGetterName+"()));");
                        return;
                    case "null":
                    case "simple":
                        ctx.push(apiClassName, "context.addPathParam(\""+ rawParamName +"\", String.valueOf(param."+parameterGetterName+"()));");
                        return;
                    default:
                        throw new IllegalArgumentException("Unsupported path parameter style " + prm.getStyle() + " for  primitive path parameter");
                }
            case "header":
                if (prm.getStyle() != null && !"simple".equals(prm.getStyle())) {
                    throw new IllegalArgumentException("Unsupported header parameter style " + prm.getStyle() + " for primitive header parameter");
                }
                ctx.push(apiClassName, "context.addHeaderParam(\""+ rawParamName +"\", String.valueOf(param."+parameterGetterName+"()));");
                return;
        }
    }

    public static void generateArrayParameter(GenerationContext ctx, String apiClassName, ParameterModel prm, String parameterGetterName) {
        switch (prm.getIn()) {
            case "query":
                switch (prm.getStyle()) {
                    case "form":
                        if (!prm.isExplode()) {
                            ctx.push(apiClassName, "context.addQueryParam(\""+Util.escapeForSourceCode(prm.getName())+"\", String.join(\",\", param."+ parameterGetterName +"()));");
                            return;
                        }
                        ctx.push(apiClassName, "for (Object qparam: param."+ parameterGetterName +"()) {");
                        ctx.addIndent(apiClassName);
                        ctx.push(apiClassName, "context.addQueryParam(\""+Util.escapeForSourceCode(prm.getName())+"\", String.valueOf(qparam));");
                        ctx.subIndent(apiClassName);
                        ctx.push(apiClassName, "}");

                        return;
                    case "spaceDelimited":
                        if (prm.isExplode()) {
                            throw new IllegalArgumentException("explosion of spaceDelimited array query parameters is undefined");
                        }
                        ctx.push(apiClassName, "context.addQueryParam(\""+Util.escapeForSourceCode(prm.getName())+"\", String.join(\" \", param."+ parameterGetterName +"()));");
                        return;
                    case "pipeDelimited":
                        if (prm.isExplode()) {
                            throw new IllegalArgumentException("explosion of pipeDelimited array query parameters is undefined");
                        }
                        ctx.push(apiClassName, "context.addQueryParam(\""+Util.escapeForSourceCode(prm.getName())+"\", String.join(\"|\", param."+ parameterGetterName +"()));");
                        return;
                    default:
                        throw new IllegalArgumentException("Unsupported query parameter style " + prm.getStyle() + " for string array query parameter");
                }
            case "path":
                switch (prm.getStyle()) {
                    case "matrix":
                        if (!prm.isExplode()) {
                            ctx.push(apiClassName, "context.addPathParam(\""+Util.escapeForSourceCode(prm.getName())+"\", \";"+Util.escapeForSourceCode(prm.getName())+"=\" + String.join(\",\", param."+ parameterGetterName +"()));");
                            return;
                        }
                        ctx.push(apiClassName, "context.addPathParam(\""+Util.escapeForSourceCode(prm.getName())+"\", " +
                                "param."+ parameterGetterName +"().stream().map(a -> \";"+Util.escapeForSourceCode(prm.getName())+"=\" + String.valueOf(a)).collect(Collectors.joining()));");
                        return;
                    case "label":
                        throw new UnsupportedOperationException("not yet implemented");
                    case "simple":
                        throw new UnsupportedOperationException("not yet implemented");
                    default:
                        throw new IllegalArgumentException("Unsupported path parameter style " + prm.getStyle() + " for string array path parameter");
                }
            case "header":
                switch (prm.getStyle()) {
                    case "simple":
                        throw new UnsupportedOperationException("not yet implemented");
                    default:
                        throw new IllegalArgumentException("Unsupported path parameter style " + prm.getStyle() + " for string array header parameter");
                }
        }
    }
}
