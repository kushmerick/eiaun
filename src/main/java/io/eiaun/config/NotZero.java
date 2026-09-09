package io.eiaun.config;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotZero.NotZeroValidator.class)
public @interface NotZero {

    String message() default "must not be zero";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NotZeroValidator implements ConstraintValidator<NotZero, Number> {

        @Override public boolean isValid(Number value, ConstraintValidatorContext ignored) {
            return value == null || value.doubleValue() != 0d;
        }

    }

}