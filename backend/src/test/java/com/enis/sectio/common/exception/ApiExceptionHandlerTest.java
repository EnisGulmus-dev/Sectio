package com.enis.sectio.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    @Test
    void disabledAccountGetsSpecificLoginMessage() {
        var response = new ApiExceptionHandler().disabledAccount();

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ACCOUNT_DISABLED");
        assertThat(response.getBody().message()).contains("pasife alındı");
    }
}
