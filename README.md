# Java JSON Schema Generator – Module Swagger (1.5)
[![Build Status](https://travis-ci.org/victools/jsonschema-module-swagger-1.5.svg?branch=master)](https://travis-ci.org/victools/jsonschema-module-swagger-1.5)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-swagger-1.5/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.victools/jsonschema-module-swagger-1.5)

Module for the `jsonschema-generator` – deriving JSON Schema attributes from `swagger` (1.5.x) annotations

## Features
1. Optionally override a field's property name with `@ApiModelProperty(name = ...)`
2. Optionally ignore a field/method if `@ApiModelProperty(hidden = true)`
3. Optionally provide a field/method's "title" as per `@ApiModel(value = ...)`
4. Provide a field/method's "description" as per `@ApiModelProperty(value = ...)` or (optionally) `@ApiModel(description = ...)`
5. Indicate a number's (field/method) "minimum" (inclusive) according to `@ApiModelProperty(allowableValues = "range[...")`
6. Indicate a number's (field/method) "exclusiveMinimum" according to `@ApiModelProperty(allowableValues = "range(...")`
7. Indicate a number's (field/method) "maximum" (inclusive) according to `@ApiModelProperty(allowableValues = "range...]")`
8. Indicate a number's (field/method) "exclusiveMaximum" according to `@ApiModelProperty(allowableValues = "range...)")`
9. Indicate a field/method's "const"/"enum" as `@ApiModelProperty(allowableValues = ...)` (if it is not a numeric range declaration)

Schema attributes derived from `@ApiModelProperty` on fields are also applied to their respective getter methods.
Schema attributes derived from `@ApiModelProperty` on getter methods are also applied to their associated fields.

## Usage
### Dependency (Maven)
```xml
<dependency>
    <groupId>com.github.victools</groupId>
    <artifactId>jsonschema-module-swagger-1.5</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Code
#### Passing into SchemaGeneratorConfigBuilder.with(Module)
```java
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
```
```java
SwaggerModule module = new SwaggerModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper)
    .with(module);
```

#### Enabling optional processing options
```java
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
import com.github.victools.jsonschema.module.swagger15.SwaggerOption;
```
```java
SwaggerModule module = new SwaggerModule(SwaggerOption.IGNORING_HIDDEN_PROPERTIES, SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES);
```

#### Complete Example
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.module.swagger15.SwaggerModule;
```
```java
ObjectMapper objectMapper = new ObjectMapper();
SwaggerModule module = new SwaggerModule();
SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(objectMapper, OptionPreset.PLAIN_JSON)
    .with(module);
SchemaGeneratorConfig config = configBuilder.build();
SchemaGenerator generator = new SchemaGenerator(config);
JsonNode jsonSchema = generator.generateSchema(YourClass.class);

System.out.println(jsonSchema.toString());
```
