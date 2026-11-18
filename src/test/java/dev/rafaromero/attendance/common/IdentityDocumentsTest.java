package dev.rafaromero.attendance.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IdentityDocumentsTest {

    @ParameterizedTest
    @CsvSource({
            "12345678Z, 12345678Z",
            "' 12.345.678-z ', 12345678Z",
            "x1234567l, X1234567L",
            "Y-1234567-X, Y1234567X"
    })
    void normalizesAndAcceptsValidDocuments(String raw, String expected) {
        assertThat(IdentityDocuments.requireValid(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "12345678A, 12345678Z",
            "X1234567A, X1234567L"
    })
    void suggestsTheCorrectCheckLetter(String raw, String suggestion) {
        assertThatThrownBy(() -> IdentityDocuments.requireValid(raw))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining(suggestion);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1234567Z", "123456789", "A1234567L", "PASSPORT1"})
    void rejectsMalformedDocuments(String raw) {
        assertThatThrownBy(() -> IdentityDocuments.requireValid(raw))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("DNI");
    }
}
