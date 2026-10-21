# Image du backend AntiGaspi : aucun Java ni Maven n'est nécessaire sur la machine hôte.
#   docker compose build            (compile + lance les tests)
#   docker compose run --rm backend (démo : recette générée dans le terminal)

# --- Étape 1 : compilation et tests avec Maven + JDK 21 ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dépendances d'abord : mises en cache tant que les pom.xml ne changent pas
COPY pom.xml ./
COPY backend/pom.xml backend/
RUN mvn -B -q -pl backend dependency:go-offline

COPY backend/src backend/src
# Gabarit du profil : utilisé par les tests (ProfileService lit ../data depuis backend/)
COPY data/profil.example.md data/

# SKIP_TESTS=true pour accélérer : docker compose build --build-arg SKIP_TESTS=true
ARG SKIP_TESTS=false
RUN mvn -B -pl backend verify -DskipTests=${SKIP_TESTS} \
    && cp backend/target/backend-*.jar /workspace/app.jar

# --- Étape 2 : exécution avec un simple JRE 21 ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home antigaspi
COPY --from=build /workspace/app.jar app.jar
# Gabarit par défaut ; le dossier data/ de l'hôte est monté par-dessus par docker compose
COPY data/profil.example.md data/
RUN chown -R antigaspi /app
USER antigaspi

ENV PROFILE_PATH=/app/data/profil.md
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
