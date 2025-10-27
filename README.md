# Robust OpenAPI Generator
OpenAPI 3.X Client Generator for the Java Programming Language using minimal external dependencies.

## Key Features of this Implementation
1. Minimal Dependencies
    * Only a JSON framework is required, which you can choose freely. (Annotations for GSON and JACKSON are generated)
2. Endpoints that return a huge amount of binary data.
    * Streaming data via an InputStream like interface is possible.
3. Different JSON entities depending on the status code of an endpoint.
4. Different content types with the same status code of an endpoint.
5. Structured JSON data can be returned as error's (non 2XX status codes)
6. Generated boilerplate code can be re-used between multiple schemas.
    * You can re-use the same ApiException type for example, for every client you generate with this schema.
7. Easy evaluation of known response headers.
    * The response object contains parsed java objects for the response headers with are present in the OpenAPI schema.
8. Meta-information about endpoints is available at runtime.
    * List of data entity classes generated.
    * List of endpoints and their parameters.
9. Fluent/Builder like interface for calling endpoints.
    * Better for endpoints with many parameters
10. Support for generating a single client that supports multiple versions of the same schema.
    * A client may need to communicate with multiple versions of the same backend.
    * See EXTENSION.md for more details
11. Precise Timeout mechanics.
    * All timeouts can be controlled for individual endpoints and for the entire client.
    * You can precisely control the following timeouts:
        - Total Request->Response timeout
        - Request Body publishing write timeout
        - Response Body read timeout
        - Response Body reading total timeout

## Usage
### Example usage with the Gson JSON framework.

```bash
# The generator is entirely configured via environment variables.
# This example uses absolute paths, you can of course use relative paths too.

# Where is the OpenAPI schema file?
export SCHEMA=/home/user/myproject/myschema.json

# Where should the generator put implementation source files?
export IMPL_SOURCE_TARGET_DIR=/home/user/myproject/src/main/generated

# Where should the generator put api source files?
export API_SOURCE_TARGET_DIR=/home/user/myproject/src/main/generated

# Where should the generator put implementation source files?
# Common files can be reused by multiple different api's.
export COMMON_IMPL_SOURCE_TARGET_DIR=/home/user/myproject/src/main/generated

# Where should the generator put api source files?
# Common files can be reused by multiple different api's.
export COMMON_API_SOURCE_TARGET_DIR=/home/user/myproject/src/main/generated

# Java package names that will be in the generated source code
export PACKAGE=com.example.myproject
export COMMON_PACKAGE=com.example.openapi.common

# Optional parameters, that are useful for some corner cases ##########################
# Comment all these variables for default use-case

# These values disable generation for certain frameworks.
export JACKSON=false
export GSON=true
export JSR380=false

# These values control the naming of the generated classes.
export MODEL_SUFFIX=MySchemaModel # Defaults to empty string.
export TAG_SUFFIX=MySchemaApi # Defaults to Api
export RESPONSE_SUFFIX=MySchemaResponse # Defaults to Response
export REQUEST_SUFFIX=MySchemaRequest # Defaults to Request

# Needed if you wish to use the generated interfaces with a Java proxy that itself throws typed exceptions.
# This gets appended to each interface method definition after the closing () and before the ;
# For example public GetUserResponse getUser(GetUserRequest request);
# becomes public GetUserResponse getUser(GetUserRequest request) throws MySpecialException;
export INTERFACE_OPERATION_DEFINITION_SUFFIX=throws MySpecialException # Defaults to empty string.

# See EXTENSION.md for details, these pretty much mirror the options above and do the same thing for an extension schema.
# It probably makes sense to set all of these options to be able to tell apart the extension schema from the main schema.
# If you don't define EXTENSION_SCHEMA then all of the other options are ignored.
export EXTENSION_SCHEMA=/home/user/myproject/myschema-ext.json
export EXTENSION_PACKAGE=com.example.myproject.extension
export EXTENSION_IMPL_SOURCE_TARGET_DIR=/home/user/myproject/src/main/generated
export EXTENSION_API_SOURCE_TARGET_DIR=/home/user/myproject/src/main/generated
export EXTENSION_MODEL_SUFFIX=ExtensionModel
export EXTENSION_TAG_SUFFIX=ExtensionApi
export EXTENSION_RESPONSE_SUFFIX=ExtensionResponse
export EXTENSION_REQUEST_SUFFIX=ExtensionRequest
export EXTENSION_OPERATION_SUFFIX=Extended

#######################################################################################

# Downloaded from GitHub releases, needs Java 11 or newer. 
# If your schema is particularly gigantic then you may need to set -Xmx
java -jar robust-openapi-generator.jar
```


```java
// ApiImpl is generated
public class MyApi extends ApiImpl {

    public MyApi(String baseUrl, HttpClient.Builder builder) {
        super(baseUrl, builder);
    }

    //Overwriting this method is optional, it is called before every request.
    //The default implementation does nothing.
    @Override
    protected void customizeRequestContext(RequestContext context) throws IOException {
        //Basic Authentication is added to every request here.
        //Naturally, do whatever you need to do to authenticate here.
        context.addHeaderParam("Authorization", "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8)));
    }

    //Customize the GSON framework here. Perhaps you wish to enable lenient parsing or register a custom TypeAdapter?
    private static final Gson GSON = new Gson();

    @Override
    protected <T> T deserializeData(RequestContext context, TypeInfo<T> desiredType, String contentType, HttpHeaders headers, InputStream stream) {
        //Note: This assumes that the response is UTF-8 text, this is almost universally true.
        //If you need to support other charsets, then you need to probe the headers for that information.
        //The InputStream is closed by the caller.
        return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), desiredType.getType());
    }

    @Override
    protected HttpRequest.BodyPublisher serializeData(RequestContext context, Object requestBody) {
        return HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody));
    }

    @Override
    public void close() {
        //Should be made a noop on Java versions older than 21.
        //Since Java 21 you can and should do this:
        this.getClient().close();
    }
}

public static void main(String[] args) {
    MyApi api = new MyApi("http://localhost:9000/", HttpClient.newBuilder());
    //getUser would be a operation for which a method was generated in ApiImpl
    GetUserResponse userResponse = api.getUser("Tom");
    if (userResponse.isS404()) {
        System.out.println("User not found");
        return;
    }
    if (userResponse.isS200Json()) {
        User user = userResponse.getResponseBodyS200Json();
        System.out.println("User=" + user);
        return;
    }
    int statusCode = userResponse.getStatusCode();
    //switch case status code here, at your discretion.
    
    GetUserResponse.Variant variantEnum = userResponse.getVariant();
    //switch case on enum here, at your discretion.
}
```
### Usage from within Gradle or Maven
Due to frequent changes in the API of Gradle or Maven, we decided against making a dedicated Gradle or Maven plugin.
Both Maven and Gradle can be configured to run the generator by running either a shell script or invoking the Java executable directly.

Gradle can also trivially load the generator jar into the buildscript classpath and invoke it directly.

Our Gradle build script looks similar to this:
```kotlin
sourceSets {
    main {
        java {
            srcDir("generated/common-api")
            srcDir("generated/schema-api")

            //We recommend using a separate maven/Gradle module for this.
            //We like to separate API and impl, but doing both in a single module
            //is also possible.
            srcDir("generated/common-impl")
            srcDir("generated/schema-impl")
        }
    }
}

tasks.getByName("clean").doFirst {
    delete("${rootDir}/generated")
    println("Deleted Generated API's")
}

buildscript {
    dependencies {
        //This jar file sits in the project root, you can also use a maven dependency notation instead.
        classpath(files("robust-openapi-generator-0.1.0.jar"))
    }
}

tasks.register("generateOpenAPI") {
    var config = ch.imagic.openapi.OpenApiGeneratorConfig()

    config.schema = project.file("schema.json")
    config.packageName = "org.example.http.schema"
    config.commonPackageName = "org.example.http.common"
    config.apiSourceTargetDir = project.file("generated/schema-api")
    config.implSourceTargetDir = project.file("generated/schema-impl")
    config.commonImplSourceTargetDir = project.file("generated/common-impl")
    config.commonApiSourceTargetDir = project.file("generated/common-api")

    inputs.files(config.schema)
    outputs.dir(config.apiSourceTargetDir)
    outputs.dir(config.implSourceTargetDir)
    outputs.dir(config.commonImplSourceTargetDir)
    outputs.dir(config.commonApiSourceTargetDir)

    doLast {
        ch.imagic.openapi.OpenApiGenerator.generate(config)
    }
    
    //In our config this "block" repeats itself for every schema.
    //We generate all schemas in a single gradle task and simply have multiple doLast 
    //blocks and config var's.
    //You can naturally split this up if that's more convenient for you.
}
```

## License
The code generator itself is released under the Apache License 2.0.

The code generator copies some handwritten .java files into the generated output; these files are provided to you under the 0-BSD license, allowing you to use them and the rest of the generated code freely without providing any attribution to the original authors.

This is made obvious by the comments at the beginning of the copied files which contain the entire 0-BSD license text.

All other generated files are merely marked as machine-generated.

## Non goals
1. Usage of a templating language to template the output. There is no intention to provide this much flexibility.
    * If you have a suggestion that doesn't cause a breaking change, then please open an issue or create a pull request.
    * If you need more complex changes, that require breaking changes then feel free to suggest them, but we may not accept them.
        - There is nothing wrong with forking the project and making your own changes.
2. Validation of OpenAPI schemas
    * The generator may generate output for invalid schema files. There is no guarantee that the generator catches schema errors.
3. Security/Sanitization of OpenAPI schemas
    * Only run this generator on trusted or reviewed schema files, a malicious schema is able to perform code injection or possibly mess with file paths in very undesired ways.
    * This is a dev tool, "security" is therefore not a concern.
4. Support of .yaml schemas. Only .json schemas are supported. Tools to convert the .yaml to .json exist.
5. Support for multi-file OpenAPI schemas
6. Support of any HTTP client framework outside Java's default java.net.http.HttpClient
7. Generation of a project stub. This generator will always only generate Java source files. It won't generate buildscripts for any build tool. It is intended that those source files will be included in your project either by you manually copying the output to your project, or by including the output folder as a source folder in your buildscript, be that Maven, Gradle or Ant.
8. Callbacks + "ondata" (They are simply ignored)
9. Cookie Parameters (They are simply ignored, you can control cookies via the HttpClient directly)
10. Authentication (Must be handled by the user as part of creating the HttpClient)
11. Support for older OpenAPI versions than 3.0
12. Generation of HTML documentation.
    * The existing OpenAPI generator works fine for this.

## Non goals (for now)
1. Support of XML entities, all XML endpoints will just be treated as application/octet-stream endpoints, and you will be able to stream and parse the XML data yourself.
    * If your OpenAPI schema mainly contains XML endpoints, then this generator will probably not be of much use to you.
2. Support of mangled enumerations. (for example, Enums that contain Java keywords such as 'class' cannot be mapped by this generator)

## Future work
1. Support wildcard content types other than "*/*" properly. ex: "image/*"
2. Support for String request parameter patterns.
    * For path parameters the pattern ".*" is already implemented, as it is commonly used for the last path parameter to transmit a file name that contains "/".

## "anyOf" / "oneOf"
If your schema contains "anyOf" or "oneOf" schemas, then those are mapped to an interface.

When using schemas with anyOf the generator will generate annotations for the Jackson JSON framework. To disable this run the generator with the -D option "-D JACKSON=false" or set the process environment variable "JACKSON" to "false". In this case the generator will not generate any Jackson annotations and the case has to be handled manually. This avoids a hard dependency to the Jackson framework and allows you to use a different JSON framework if you so desire.

It is probably worth considering to avoid "anyOf" or "oneOf" when designing an API, because not all JSON frameworks can trivially handle it.

## Any type
If your schema uses the Any type then this type is simply mapped to java.lang.Object. It's unlikely that your JSON Framework will by itself deserialize this correctly. It's likely necessary to write custom code for any deserialization of the Any type in a Response. It is probably worth considering to avoid the Any type when designing an API.

## Name mangling
If the API contains members that have the same name as reserved Java keywords (such as for example 'class') then the generator will mangle the name. By default, it will generate an appropriate annotation for both the Jackson and GSON JSON frameworks.

To disable generation for Jackson run the generator with the -D option "-D JACKSON=false" or set the process environment variable "JACKSON" to "false". In this case the generator will not generate any Jackson annotations and the case has to be handled manually. This avoids a hard dependency to the Jackson framework and allows you to use a different JSON framework if you so desire.

To disable generation for Gson run the generator with the -D option "-D GSON=false" or set the process environment variable "GSON" to "false". In this case the generator will not generate any Gson annotations and the case has to be handled manually. This avoids a hard dependency to the Gson framework and allows you to use a different JSON framework if you so desire.

It is probably worth considering to avoid using keywords of common programming languages when designing an API, because it will almost always create issues like these.

## Exceptions
The generator will generate a class called ApiException. This exception is thrown when an IOException occurs during a request or when the server responds with any status code and content type combination that is NOT present in the OpenAPI schema.

If your schema, for example, declares 4XX or 5XX responses, then those will not cause an exception to be thrown, and instead you will be able to handle them while processing the normal response object. If those responses have a defined response body, then that response body is also parsed and will be available in the response object.

The information which "variant" of the response was received is available in the response object.

If you attempt to get the response body for a different variant, then the getter for the variant will throw an ApiException that contains all information about what was actually received, including the response body.

## JSR-380
The generator will generate validation annotations according to the JSR-380 specification. Only the "new" annotations are supported. For example. "jakarta.validation.constraints.Positive" on a number that must be positive. The old annotations using "javax.validation.constraints" are not supported.

To disable generation for JSR-380 run the generator with the -D option "-D JSR380=false" or set the process environment variable "JSR380" to "false".

If there is a demand for support for the old annotations, then I will consider adding support for them.