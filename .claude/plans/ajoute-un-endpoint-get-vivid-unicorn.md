# Plan : endpoint GET /sante

## Contexte
Ajouter un endpoint de santé minimal `GET /sante` renvoyant `{"statut": "ok"}`, avec son test, en suivant les conventions du projet (Spring Boot, contrôleurs dans `controleurs/`, tests MockMvc héritant de `BaseTest`).

## Modifications

1. **Nouveau** `src/main/java/fr/alderanconseil/notafrais/controleurs/ControleurSante.java`
   - `@RestController`, javadoc en français comme les autres contrôleurs.
   - `@GetMapping("/sante")` → `Map<String, String>` : `Map.of("statut", "ok")` (sérialisé en JSON par Jackson).

2. **Nouveau** `src/test/java/fr/alderanconseil/notafrais/SanteTest.java`
   - `class SanteTest extends BaseTest` (réutilise `mvc`).
   - Test `sante_renvoieStatutOk` (nommage `methode_cas`, commentaires Préparation/Action/Vérification) :
     `mvc.perform(get("/sante"))` → `status().isOk()`, `content().contentTypeCompatibleWith(APPLICATION_JSON)`, `jsonPath("$.statut").value("ok")`.

## Vérification
- `./mvnw test -Dtest=SanteTest` puis `./mvnw test` pour vérifier l'absence de régression (et la couverture JaCoCo).
