# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.0.0] – 2019-06-15
### Added
- Optionally override a field's property name with `@ApiModelProperty(name = ...)`
- Optionally ignore a field/method if `@ApiModelProperty(hidden = true)`
- Provide a field/method's "description" as per `@ApiModelProperty(value = ...)` or `@ApiModel(description = ...)`
- Indicate a number's (field/method) "minimum" (inclusive) according to `@ApiModelProperty(allowableValues = "range[...")`
- Indicate a number's (field/method) "exclusiveMinimum" according to `@ApiModelProperty(allowableValues = "range(...")`
- Indicate a number's (field/method) "maximum" (inclusive) according to `@ApiModelProperty(allowableValues = "range...]")`
- Indicate a number's (field/method) "exclusiveMaximum" according to `@ApiModelProperty(allowableValues = "range...)")`
- Indicate a field/method's "const"/"enum" as `@ApiModelProperty(allowableValues = ...)` (if it is not a numeric range declaration)
- Consider the `@ApiModelProperty` annotation on a getter method also for its field
- Consider the `@ApiModelProperty` annotation on a field also for its getter method

[Unreleased]: https://github.com/victools/jsonschema-module-swagger-1.5/compare/v3.0.0...HEAD
[3.0.0]: https://github.com/victools/jsonschema-module-swagger-1.5/releases/tag/v3.0.0