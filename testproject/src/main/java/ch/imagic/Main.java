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
package ch.imagic;


import undertest.common.impl.RequestContext;
import undertest.impl.ApiImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;

public class Main {

    public static class TestApi extends ApiImpl {

        public TestApi(String baseUrl, HttpClient.Builder builder) {
            super(baseUrl, builder);
        }

        public TestApi(String baseUrl, HttpClient client) {
            super(baseUrl, client);
        }

        @Override
        public void close() throws RuntimeException {

        }

        @Override
        protected <T> T deserializeJsonData(RequestContext context, Type desiredType, int statusCode, HttpHeaders headers, InputStream stream) throws IOException {
            throw new UnsupportedEncodingException();
        }

        @Override
        protected HttpRequest.BodyPublisher serializeJsonData(RequestContext context, Object requestBody) throws IOException {
            throw new UnsupportedEncodingException();
        }
    }

    public static void main(String[] args) {
        //EMPTY
    }
}