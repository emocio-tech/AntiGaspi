package fr.antigaspi.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UrgencyTest {

    @ParameterizedTest(name = "{0} jour(s) restant(s) -> {1}")
    @CsvSource({ "-5, EXPIRED_OR_TODAY", "-1, EXPIRED_OR_TODAY", "0, EXPIRED_OR_TODAY", "1, SOON", "3, SOON", "4, WEEK",
            "7, WEEK", "8, LATER", "365, LATER" })
    void urgenceSelonJoursRestants(long daysLeft, Urgency expected) {
        assertThat(Urgency.of(daysLeft)).isEqualTo(expected);
    }
}
