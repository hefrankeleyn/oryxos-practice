package com.oryxlabs.oryxos.tool;

import com.fasterxml.jackson.databind.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.util.*;

/** 对受支持 schema 子集做递归参数校验；不支持的约束明确拒绝，绝不只以合法 JSON 放行。 */
public final class ToolInputValidator {
  private final ObjectMapper mapper =
      new ObjectMapper()
          .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

  public void validate(String arguments, String schema) {
    try {
      check(mapper.readTree(arguments), mapper.readTree(schema));
    } catch (java.io.IOException | RuntimeException failure) {
      throw new RuntimeFailure(RuntimeFailure.Code.TOOL_VALIDATION);
    }
  }

  private void check(JsonNode value, JsonNode schema) {
    if (value == null || schema == null || !schema.isObject()) throw invalid();
    Set<String> supported =
        Set.of(
            "$schema",
            "type",
            "title",
            "description",
            "properties",
            "required",
            "additionalProperties",
            "items",
            "enum",
            "minimum",
            "maximum",
            "minLength",
            "maxLength",
            "minItems",
            "maxItems");
    schema
        .fieldNames()
        .forEachRemaining(
            key -> {
              if (!supported.contains(key)) throw invalid();
            });
    String type = schema.path("type").asText();
    boolean match =
        switch (type) {
          case "object" -> value.isObject();
          case "array" -> value.isArray();
          case "string" -> value.isTextual();
          case "integer" -> value.isIntegralNumber();
          case "number" -> value.isNumber();
          case "boolean" -> value.isBoolean();
          case "null" -> value.isNull();
          default -> false;
        };
    if (!match) throw invalid();
    if (schema.has("enum")) {
      boolean found = false;
      for (var v : schema.get("enum")) if (v.equals(value)) found = true;
      if (!found) throw invalid();
    }
    if (value.isObject()) {
      for (var required : schema.path("required"))
        if (!value.has(required.asText())) throw invalid();
      var properties = schema.path("properties");
      value
          .fields()
          .forEachRemaining(
              e -> {
                if (properties.has(e.getKey())) check(e.getValue(), properties.get(e.getKey()));
                else if (schema.has("additionalProperties")) {
                  var additional = schema.get("additionalProperties");
                  if (additional.isBoolean()) {
                    if (!additional.booleanValue()) throw invalid();
                  } else check(e.getValue(), additional);
                }
              });
    }
    if (value.isArray()) {
      if (schema.has("minItems") && value.size() < schema.get("minItems").asInt()
          || schema.has("maxItems") && value.size() > schema.get("maxItems").asInt())
        throw invalid();
      if (schema.has("items")) for (var item : value) check(item, schema.get("items"));
    }
    if (value.isNumber()) {
      if (schema.has("minimum")
              && value.decimalValue().compareTo(schema.get("minimum").decimalValue()) < 0
          || schema.has("maximum")
              && value.decimalValue().compareTo(schema.get("maximum").decimalValue()) > 0)
        throw invalid();
    }
    if (value.isTextual()) {
      int size = value.textValue().codePointCount(0, value.textValue().length());
      if (schema.has("minLength") && size < schema.get("minLength").asInt()
          || schema.has("maxLength") && size > schema.get("maxLength").asInt()) throw invalid();
    }
  }

  private RuntimeFailure invalid() {
    return new RuntimeFailure(RuntimeFailure.Code.TOOL_VALIDATION);
  }
}
