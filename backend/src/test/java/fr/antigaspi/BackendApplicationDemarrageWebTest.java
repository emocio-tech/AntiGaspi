package fr.antigaspi;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Démarrage complet avec un vrai serveur web et la configuration par défaut : les chemins relatifs des données
 * ({@code ../data/profil.md}, {@code ../data/utensils.json}) ne doivent pas être pris pour des ressources web.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BackendApplicationDemarrageWebTest {

    @LocalServerPort
    private int port;

    @Test
    void demarreAvecLesCheminsRelatifsParDefaut_etRepondAuxRequetes() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isLessThan(500);
    }
}
