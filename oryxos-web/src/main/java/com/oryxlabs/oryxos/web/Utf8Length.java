package com.oryxlabs.oryxos.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 消息的 UTF-8 字节长度限制；空值交给 NotNull/NotBlank 等必填约束处理。 */
@Target({
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.PARAMETER,
  ElementType.ANNOTATION_TYPE,
  ElementType.TYPE_USE,
  ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Utf8LengthValidator.class)
public @interface Utf8Length {
  String message() default "must not exceed {max} UTF-8 bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int max() default 32768;
}
