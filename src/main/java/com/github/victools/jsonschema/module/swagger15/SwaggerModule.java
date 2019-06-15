/*
 * Copyright 2019 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.victools.jsonschema.module.swagger15;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.MethodScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON Schema Generator Module – Swagger (1.5).
 */
public class SwaggerModule implements Module {

    private static final String OPENING_BRACKET = Pattern.quote("(") + '|' + Pattern.quote("[");
    private static final String NUMBER_OR_NEGATIVE_INFINITE = "-?[0-9]+\\.?[0-9]*|-infinity";
    private static final String NUMBER_OR_INFINITE = "-?[0-9]+\\.?[0-9]*|infinity";
    private static final String CLOSING_BRACKET = Pattern.quote(")") + '|' + Pattern.quote("]");
    private static final Pattern ALLOWABLE_VALUES_RANGE = Pattern.compile("range(" + OPENING_BRACKET
            + ")(" + NUMBER_OR_NEGATIVE_INFINITE + "), *(" + NUMBER_OR_INFINITE + ")(" + CLOSING_BRACKET + ")");

    private final List<SwaggerOption> options;

    /**
     * Constructor.
     *
     * @param options features to enable
     */
    public SwaggerModule(SwaggerOption... options) {
        this.options = options == null ? Collections.emptyList() : Arrays.asList(options);
    }

    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        SchemaGeneratorConfigPart<FieldScope> fieldConfigPart = builder.forFields();
        this.applyToConfigPart(fieldConfigPart);
        if (this.options.contains(SwaggerOption.ENABLE_PROPERTY_NAME_OVERRIDES)) {
            fieldConfigPart
                    .withPropertyNameOverrideResolver(this::resolvePropertyNameOverride);
        }
        this.applyToConfigPart(builder.forMethods());
    }

    /**
     * Apply configurations that are part of this module to the given configuration part – expectation being that fields and methods get the same.
     *
     * @param configPart configuration instance to add configurations too
     */
    private void applyToConfigPart(SchemaGeneratorConfigPart<?> configPart) {
        if (this.options.contains(SwaggerOption.IGNORING_HIDDEN_PROPERTIES)) {
            configPart.withIgnoreCheck(this::shouldIgnore);
        }
        configPart.withDescriptionResolver(this::resolveDescription);
        configPart.withNumberExclusiveMinimumResolver(this::resolveNumberExclusiveMinimum);
        configPart.withNumberInclusiveMinimumResolver(this::resolveNumberInclusiveMinimum);
        configPart.withNumberExclusiveMaximumResolver(this::resolveNumberExclusiveMaximum);
        configPart.withNumberInclusiveMaximumResolver(this::resolveNumberInclusiveMaximum);
        configPart.withEnumResolver(this::resolveAllowedValues);
    }

    /**
     * Retrieves the annotation instance of the given type, either from the field it self or (if not present) from its getter.
     *
     * @param <A> type of annotation
     * @param member field or method to retrieve annotation instance from (or from a field's getter or getter method's field)
     * @param annotationClass type of annotation
     * @return annotation instance (or {@code null})
     * @see MemberScope#getAnnotation(Class)
     * @see FieldScope#findGetter()
     * @see MethodScope#findGetterField()
     */
    protected <A extends Annotation> Optional<A> getAnnotationFromFieldOrGetter(MemberScope<?, ?> member, Class<A> annotationClass) {
        A annotation = member.getAnnotation(annotationClass);
        if (annotation == null) {
            MemberScope<?, ?> associatedGetterOrField;
            if (member instanceof FieldScope) {
                associatedGetterOrField = ((FieldScope) member).findGetter();
            } else if (member instanceof MethodScope) {
                associatedGetterOrField = ((MethodScope) member).findGetterField();
            } else {
                associatedGetterOrField = null;
            }
            annotation = associatedGetterOrField == null ? null : associatedGetterOrField.getAnnotation(annotationClass);
        }
        return Optional.ofNullable(annotation);
    }

    /**
     * Determine whether a given member should be ignored, i.e. excluded from the generated schema.
     *
     * @param member targeted field/method
     * @return whether to ignore the given field/method
     */
    protected boolean shouldIgnore(MemberScope<?, ?> member) {
        return this.getAnnotationFromFieldOrGetter(member, ApiModelProperty.class)
                .map(ApiModelProperty::hidden)
                .orElse(Boolean.FALSE);
    }

    /**
     * Look-up name override for a given field or its associated getter method from the {@link ApiModelProperty} annotation's {@code name}.
     *
     * @param field targeted field
     * @return applicable name override (or {@code null})
     */
    protected String resolvePropertyNameOverride(FieldScope field) {
        return this.getAnnotationFromFieldOrGetter(field, ApiModelProperty.class)
                .map(ApiModelProperty::name)
                .filter(name -> !name.isEmpty() && !name.equals(field.getName()))
                .orElse(null);
    }

    /**
     * Look-up a "description" for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's {@code value} –
     * falling-back on the member type's {@link ApiModel} annotation's {@code description}.
     *
     * @param member targeted field/method
     * @return description (or {@code null})
     */
    protected String resolveDescription(MemberScope<?, ?> member) {
        String propertyAnnotationValue = this.getAnnotationFromFieldOrGetter(member, ApiModelProperty.class)
                .map(ApiModelProperty::value)
                .filter(value -> !value.isEmpty())
                .orElse(null);
        if (propertyAnnotationValue != null) {
            return propertyAnnotationValue;
        }
        return Optional.ofNullable(member.getType())
                .map(type -> type.getErasedType().getAnnotation(ApiModel.class))
                .map(ApiModel::description)
                .filter(description -> !description.isEmpty())
                .orElse(null);
    }

    /**
     * Retrieve the given member's (or its associated getter/field's) {@link ApiModelProperty} annotation and extract its {@code allowableValues}.
     *
     * @param member targeted field/method
     * @return {@link ApiModelProperty} annotation's non-empty {@code allowableValues} (or {@code null})
     */
    private Optional<String> findModelPropertyAllowableValues(MemberScope<?, ?> member) {
        return this.getAnnotationFromFieldOrGetter(member, ApiModelProperty.class)
                .map(ApiModelProperty::allowableValues)
                .filter(allowableValues -> !allowableValues.isEmpty());
    }

    /**
     * Look-up a "const"/"enum" for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return list of allowed values (or {@code null})
     */
    protected List<String> resolveAllowedValues(MemberScope<?, ?> member) {
        return this.findModelPropertyAllowableValues(member)
                .filter(allowableValues -> !ALLOWABLE_VALUES_RANGE.matcher(allowableValues).matches())
                .map(allowableValues -> Arrays.asList(allowableValues.split(", *")))
                .orElse(null);
    }

    /**
     * Determine (inclusive) numeric minimum for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return inclusive numeric minimum (or {@code null})
     */
    protected BigDecimal resolveNumberInclusiveMinimum(MemberScope<?, ?> member) {
        return this.resolveNumberMinimum(member, "[");
    }

    /**
     * Determine (exclusive) numeric minimum for the given member or its associated getter/field from the {@link ApiModelProperty} annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return exclusive numeric minimum (or {@code null})
     */
    protected BigDecimal resolveNumberExclusiveMinimum(MemberScope<?, ?> member) {
        return this.resolveNumberMinimum(member, "(");
    }

    /**
     * Determine numeric minimum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @param inclusiveIndicator the opening parenthesis/bracket indicating the desired kind of minimum (either inclusive or exclusive)
     * @return numeric minimum (or {@code null})
     */
    private BigDecimal resolveNumberMinimum(MemberScope<?, ?> member, String inclusiveIndicator) {
        String allowableValues = this.findModelPropertyAllowableValues(member).orElse(null);
        if (allowableValues != null) {
            Matcher matcher = ALLOWABLE_VALUES_RANGE.matcher(allowableValues);
            if (matcher.matches() && inclusiveIndicator.equals(matcher.group(1)) && !"-infinity".equals(matcher.group(2))) {
                return new BigDecimal(matcher.group(2));
            }
        }
        return null;
    }

    /**
     * Determine (inclusive) numeric maximum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return inclusive numeric maximum (or {@code null})
     */
    protected BigDecimal resolveNumberInclusiveMaximum(MemberScope<?, ?> member) {
        return this.resolveNumberMaximum(member, "]");
    }

    /**
     * Determine (exclusive) numeric maximum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @return exclusive numeric maximum (or {@code null})
     */
    protected BigDecimal resolveNumberExclusiveMaximum(MemberScope<?, ?> member) {
        return this.resolveNumberMaximum(member, ")");
    }

    /**
     * Determine numeric maximum for the given member or its associated getter/field from the {@link ApiModelProperty}'s annotation's
     * {@code allowedValues}.
     *
     * @param member targeted field/method
     * @param inclusiveIndicator the closing parenthesis/bracket indicating the desired kind of minimum (either inclusive or exclusive)
     * @return numeric maximum (or {@code null})
     */
    private BigDecimal resolveNumberMaximum(MemberScope<?, ?> member, String inclusiveIndicator) {
        String allowableValues = this.findModelPropertyAllowableValues(member).orElse(null);
        if (allowableValues != null) {
            Matcher matcher = ALLOWABLE_VALUES_RANGE.matcher(allowableValues);
            if (matcher.matches() && inclusiveIndicator.equals(matcher.group(4)) && !"infinity".equals(matcher.group(3))) {
                return new BigDecimal(matcher.group(3));
            }
        }
        return null;
    }
}
