---
name: generate-tests
description: Génère les tests d'une feature ou d'un code modifié à partir d'un plan de test validé (ou du diff), en suivant les conventions de test du projet, puis les exécute. À utiliser après l'implémentation ou, en TDD, avant elle.
argument-hint: [chemin, plan de test ou description optionnelle, par défaut : fichiers modifiés du diff]
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(mvn:*), Bash(./refine:*), Read, Grep, Glob, Edit, Write
---

# Génération des tests

Génère les tests du périmètre `$ARGUMENTS` (par défaut : les fichiers modifiés ou ajoutés, d'après `git status` et `git diff`). Si un plan de test validé existe dans la conversation, il fait foi.

## Étapes

1. **Périmètre et source** : identifie le code à couvrir et le plan de test validé. S'il n'y en a pas, dérive les cas du comportement du code (nominaux, limites, erreurs).
2. **Conventions** : lis les tests existants de la zone (framework, nommage, structure, utilitaires, fixtures, emplacement) et consulte CLAUDE.md pour les commandes de test. Aligne-toi dessus.
3. **Proposition** : liste les tests à écrire sous forme de tableau `Fichier de test | Cas | Entrée | Résultat attendu`, en distinguant les fichiers à créer de ceux à compléter.
4. **Validation** : demande à l'utilisateur la confirmation avant toute modification.
5. **Écriture** : après accord, crée ou complète les tests avec Edit ou Write.
6. **Exécution** : lance uniquement les tests concernés avec la commande ciblée du projet (par exemple `mvn test -pl main -Dtest=Classe#methode` pour OpenRefine) et relève le résultat.
7. **Résumé** : fichiers créés ou modifiés, nombre de tests ajoutés, résultat de l'exécution, sans créer de commit.

## Règles
- Ne modifie jamais le code de production, seulement les tests. Si un test révèle un défaut du code, signale-le à l'utilisateur au lieu de corriger.
- Un test échoue : ne l'affaiblis pas et ne le désactive pas. Explique la cause (test faux ou code faux) et demande la décision.
- Chaque test vérifie un comportement précis avec des assertions significatives ; pas de test vide ni redondant.
- Préfère les tests déterministes et isolés (pas de dépendance au réseau, à l'heure ou à l'ordre d'exécution).
- Respecte le style, les noms et les idiomes des tests environnants.
- Respecte les restrictions du projet (CLAUDE.md), notamment celles d'un éventuel exercice ou atelier.
