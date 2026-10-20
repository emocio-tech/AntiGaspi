package fr.antigaspi.profile.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.antigaspi.profile.model.UserProfile;

class ProfileServiceTest {

    @Test
    void getProfile_renvoieLeProfilDuRepository() {
        UserProfile profile = new UserProfile(List.of("Sans gluten"));

        assertThat(new ProfileService(() -> profile).getProfile()).isEqualTo(profile);
    }
}
