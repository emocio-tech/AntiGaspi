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

import tools.jackson.databind.json.JsonMapper;

/**
 * Démarrage complet avec un vrai serveur web et la configuration par défaut : le chemin relatif du profil
 * ({@code ../data/profil.md}) ne doit pas être pris pour une ressource web, et l'inventaire amorcé en base est servi.
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

    @Test
    void apiInventaire_renvoieLesAlimentsAmorces() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ingredients"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonMapper.builder().build().readTree(response.body()).size()).isEqualTo(28);
    }
}
