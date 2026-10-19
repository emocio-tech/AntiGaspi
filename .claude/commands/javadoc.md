---
description: Vérifie la présence de Javadoc sur le code Java et génère celle qui manque
argument-hint: [chemin optionnel, par défaut : src/main/java]
allowed-tools: Bash(git status:*), Bash(git diff:*), Read, Grep, Glob, Edit
---

# Javadoc

Vérifie que la Javadoc est présente sur tout le code Java de `$ARGUMENTS` (par défaut `src/main/java`), puis génère celle qui manque.

## Étapes

1. **Audit** : parcours les fichiers `.java` (hors `src/test`) et repère les éléments sans Javadoc :
   classes, interfaces, enums, records, méthodes et constructeurs `public`/`protected`, champs `public`/`protected`.
   Signale aussi les Javadoc incomplètes (`@param`, `@return`, `@throws` manquants ou obsolètes).
2. **Rapport** : tableau `Fichier | Élément | Problème`, avec un total par fichier. Si tout est conforme, dis-le et arrête-toi.
3. **Validation** : demande à l'utilisateur la confirmation avant toute modification, en proposant le périmètre (tout, ou une liste de fichiers).
4. **Génération** : après accord, ajoute ou complète la Javadoc avec Edit.

## Règles de génération
- Rédige en français, de façon concise : une phrase de résumé, puis les balises utiles.
- Décris ce que fait réellement le code (lis le corps de la méthode) ; n'invente rien.
- Ne modifie jamais le code, seulement les commentaires. Respecte la Javadoc existante et ne la réécris que si elle est fausse ou incomplète.
- Utilise `{@inheritDoc}` ou ignore les méthodes annotées `@Override` triviales.
- Termine par un résumé des fichiers modifiés, sans créer de commit.
