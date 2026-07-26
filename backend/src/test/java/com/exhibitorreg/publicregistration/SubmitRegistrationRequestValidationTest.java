package com.exhibitorreg.publicregistration;

import static org.assertj.core.api.Assertions.assertThat;

import com.exhibitorreg.publicregistration.dto.SubmitRegistrationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SubmitRegistrationRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static SubmitRegistrationRequest.PersonInput person(int i) {
        return new SubmitRegistrationRequest.PersonInput("Name " + i, "Designation " + i);
    }

    private static SubmitRegistrationRequest requestWithPeopleCount(int count) {
        List<SubmitRegistrationRequest.PersonInput> people =
                IntStream.range(0, count).mapToObj(SubmitRegistrationRequestValidationTest::person).toList();
        return new SubmitRegistrationRequest("Acme Exhibits", people, "captcha-token");
    }

    @Test
    void rejectsZeroPeople() {
        Set<ConstraintViolation<SubmitRegistrationRequest>> violations = validator.validate(requestWithPeopleCount(0));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void acceptsOnePerson() {
        assertThat(validator.validate(requestWithPeopleCount(1))).isEmpty();
    }

    @Test
    void acceptsNinePeople() {
        assertThat(validator.validate(requestWithPeopleCount(9))).isEmpty();
    }

    @Test
    void rejectsTenPeople() {
        Set<ConstraintViolation<SubmitRegistrationRequest>> violations = validator.validate(requestWithPeopleCount(10));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void rejectsBlankPersonName() {
        var request = new SubmitRegistrationRequest(
                "Acme Exhibits",
                List.of(new SubmitRegistrationRequest.PersonInput("", "Designation")),
                "captcha-token");

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
