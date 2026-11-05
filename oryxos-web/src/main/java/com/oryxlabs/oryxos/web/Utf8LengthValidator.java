package com.oryxlabs.oryxos.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

/** 按传输编码计量消息，避免把中文和 emoji 的字符数误当字节数。 */
public final class Utf8LengthValidator implements ConstraintValidator<Utf8Length, CharSequence> {
  private int max;

  @Override
  public void initialize(Utf8Length constraint) {
    max = constraint.max();
    if (max < 0) {
      throw new IllegalArgumentException("UTF-8 length limit must be nonnegative");
    }
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
  }
}
