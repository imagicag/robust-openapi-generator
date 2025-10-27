# Supporting multiple versions of the same API seamlessly.

## Our situation

The usual strategy of having different API versions be on different base paths is sadly not possible for us,
as the backend components only support exactly 1 version of the API, so we have to support multiple versions in the client.

If you have the opportunity to do this on the server side, then we recommend doing so.

If you can avoid having to deal with multiple versions of the API, then naturally you should.

For various reasons, we cannot do either.

## Our goal
We would like to only use the latest version of the API when writing business logic in the client
and hide this version complexity. We don't want to deal with it in every single place where we do an API call.

## The trivial approach
If your API has a small number of endpoints and a reasonably simple data model, then you can generate
a client for each version and manually translate the model objects from one version to the other.
This can also be done by abusing your JSON framework of choice to do so, as the model properties will be the same.
Simply doing Object -> Json String -> Object will do the trick. (This is not 'fast' at runtime,
but it works and is trivial to implement)

For the few actual changes you can write manual mapper functions, be that manually or using a library.

Unfortunately, our API is huge and our models contain very complex, nested data structures.
So whenever we cannot abuse the JSON framework to do this conversion, we have to write a custom mapper function.
Doing so when dealing with 100+ Model Objects that are sometimes nested several layers deep is not ideal,
especially if the model changes at a high depth.

Using the normal OpenAPI generator, this is probably the best you can do.

As you can probably guess, doing this every 6 months for a large number of components takes up a large amount of time.

## Our approach
Using the robust-openapi-generator.jar, you can specify a base OpenAPI schema as well as several additional OpenAPI schemas
as part of a single generation. All these OpenAPI schema files are completely separate from each other and would correspond to a single
version of the API for a single component.

In our use case the latest version is the base version, and all older versions we still have to support are additional versions.
You can do this the other way around as well, but for us doing it this way has worked out better for us.
It really depends on the way your backend components API's evolve over time.

Once configured this way, the generator will then compare all operations as well as all
their parameters/responses/schemas etc. between the last version and the older versions
and determine which operations are the same and which are different.

It will then generate a client for each version of the API just like you would do using the trivial approach,
however, the difference is that model classes (data model/requests/responses) are reused
between the latest version and the older versions as much as possible.

Since the generator generates an API interface for each client that contains all the operations for the respective versions,
we let our implementation for the old version implement the API interface of the latest version.

This way the Java compiler forces us to implement all operations that are different in the last version and old version manually.
Operations that are the same will be already implemented in the generated code and therefore won't generate a compiler error.

In the manual implementation of the changed or new operations, you can then either call the implementation of the additional version,
or you can throw an exception if performing the operation is not possible using the older version.

A common situation where this is the case is when a new version of the API introduces an entirely new endpoint for a new feature.
Since you can only throw an exception in the client implementation for the old version, having a utility that tells you if
a certain business operation is supported by the current version of the API is a good idea.

This approach still requires you to write some mapper functions that translate the object model from one version to the other.
However, since the generated models will re-use inner nested data structures as long as they are the same, you will only have to write
mapper functions for the data structures that are actually different. Even those will be smaller as not every object needs to be translated.

As a result of doing this, your actual client business logic will always appear to only use the latest version of the API.
Give or take a few 'if statements' to see if performing a certain business operation is possible using the current version of the API.

## Example configuration
```kotlin
sourceSets {
    main {
        java {
            srcDir("generated/common-api")
            srcDir("generated/schema-api")
            srcDir("generated/old-api")

            srcDir("generated/common-impl")
            srcDir("generated/schema-impl")
            srcDir("generated/old-impl")
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

    config.schema = project.file("headSchema.json")
    config.packageName = "org.example.http.schema"
    config.commonPackageName = "org.example.http.common"
    config.apiSourceTargetDir = project.file("generated/schema-api")
    config.implSourceTargetDir = project.file("generated/schema-impl")
    config.commonImplSourceTargetDir = project.file("generated/common-impl")
    config.commonApiSourceTargetDir = project.file("generated/common-api")
    config.isJsr380 = true
    config.isGson = true

    config.extensionSchema = project.file("oldSchema.json")
    config.extensionPackage = "org.example.http.schema.old"
    config.extensionApiSource =  project.file("generated/old-api")
    config.extensionImplSource =  project.file("generated/old-impl")
    config.extensionTagSuffix = "ApiOld"
    config.extensionModelSuffix = "Old"
    config.extensionRequestSuffix = "RequestOld"
    config.extensionResponseSuffix = "ResponseOld"
    config.extensionOperationSuffix = "Old"
    
    inputs.files(config.schema)
    inputs.files(config.extensionSchema)
    outputs.dir(config.apiSourceTargetDir)
    outputs.dir(config.implSourceTargetDir)
    outputs.dir(config.commonImplSourceTargetDir)
    outputs.dir(config.commonApiSourceTargetDir)

    doLast {
        ch.imagic.openapi.OpenApiGenerator.generate(ims25H1)
    }
    
    //If you have multiple older versions simply repeat this block for each version.
    //The generated files for the "headSchema" will simply overwrite the identical previous generation.
    //I do not recommend making seperate tasks in this case as it may cause gradle to run them in parallel.
}
```


