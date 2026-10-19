---
name: generate-doc
description: Génère ou complète la documentation d'une feature ou d'un ensemble de fichiers modifiés (Javadoc, commentaires utiles, doc utilisateur ou développeur, fichiers de langue). À utiliser après l'implémentation, sur le périmètre du diff ou sur un chemin donné.
argument-hint: [chemin ou description optionnelle, par défaut : fichiers modifiés du diff]
allowed-tools: Bash(git status:*), Bash(git diff:*), Read, Grep, Glob, Edit, Write
---

# Génération de la documentation

Génère ou complète la documentation du périmètre `$ARGUMENTS` (par défaut : les fichiers modifiés ou ajoutés, d'après `git status` et `git diff`).

## Étapes

1. **Périmètre** : liste les fichiers concernés. Ignore les fichiers de test, sauf si l'utilisateur le demande.
2. **Langue et style** : détecte la langue et le style de la doc existante du projet (Javadoc, README, docs) et aligne-toi dessus. Si l'utilisateur indique une langue, elle prime. Consulte CLAUDE.md pour les conventions du projet.
3. **Audit** : repère ce qui manque ou est faux :
   - code : classes, interfaces, enums, records, méthodes, constructeurs et champs `public`/`protected` sans Javadoc, ou avec `@param`, `@return`, `@throws` manquants ou obsolètes ;
   - doc du projet : pages, README, références (fonctions, commandes, options) à mettre à jour ou à créer pour la feature ;
   - ressources : fichiers de langue ou index à compléter si la feature ajoute des textes.
4. **Rapport** : tableau `Fichier | Élément | Problème | Action proposée`, avec un total par fichier. Si tout est conforme, dis-le et arrête-toi.
5. **Validation** : demande à l'utilisateur la confirmation avant toute modification, en proposant le périmètre (tout, ou une liste de fichiers).
6. **Génération** : après accord, ajoute ou complète la doc avec Edit ou Write.
7. **Résumé** : liste concise des fichiers modifiés ou créés, sans créer de commit.

## Règles de génération
- Une phrase de résumé, puis seulement les balises utiles ; reste concis.
- Décris ce que fait réellement le code (lis le corps des méthodes) ; n'invente rien et signale toute zone où le comportement est incertain.
- Ne modifie jamais le code, seulement les commentaires et les fichiers de doc. Respecte la doc existante et ne la réécris que si elle est fausse ou incomplète.
- Utilise `{@inheritDoc}` ou ignore les méthodes `@Override` triviales.
- Pour une feature visible par l'utilisateur, documente le comportement (usage, paramètres, exemples) et non l'implémentation.
- Ne crée pas de nouveau fichier de doc si une page existante peut être complétée.
- Respecte les restrictions du projet (CLAUDE.md), notamment celles d'un éventuel exercice ou atelier.
