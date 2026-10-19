---
description: Review systématique du code modifié avec rapport détaillé avant tout commit
allowed-tools: Bash(git status:*), Bash(git diff:*), Bash(git log:*), Bash(./mvnw:*), Bash(mvnw.cmd:*), Read, Grep, Glob
---

<!-- Commande en lecture seule : ne jamais créer de commit ni modifier de fichier. Cible : projets Java / Maven. -->

# Review avant commit

Effectue une review systématique des modifications en cours (projet Java / Maven) et produis un rapport détaillé. **Ne crée aucun commit et ne modifie aucun fichier** : cette commande est en lecture seule.

## Contexte
- État du dépôt : !`git status --short`
- Diff indexé : !`git diff --cached --stat`
- Diff non indexé : !`git diff --stat`

## Étapes

1. **Périmètre** : lis le diff complet (`git diff HEAD`) et lis en entier chaque fichier modifié ou nouveau (y compris non suivis). Si rien n'est modifié, dis-le et arrête-toi.
2. **Build et tests** : exécute `./mvnw -q test` (ou `mvnw.cmd`) et rapporte les résultats exacts (tests passés/échoués, erreurs de compilation).
3. **Analyse** selon les axes suivants :
   - **Correction** : bugs, cas limites, NPE, gestion des erreurs et exceptions métier, calculs (montants, TVA/TTC, arrondis).
   - **Sécurité** : injection SQL, secrets ou identifiants en dur, données sensibles loguées, validation des entrées.
   - **Conception** : respect de l'architecture existante (contrôleurs, bdd/BaseDonnees, exceptions métier), duplication, responsabilités.
   - **Tests** : couverture des changements, cas d'erreur testés, nommage des cas, tests fragiles.
   - **Lisibilité et conventions** : nommage, cohérence avec le code voisin, code mort, imports inutilisés, commentaires.
   - **Performance** : requêtes redondantes, boucles coûteuses, ressources non fermées.
   - **Hygiène du commit** : fichiers à ne pas commiter (artefacts, config locale, secrets), commit à scinder en plusieurs, message de commit proposé cohérent avec l'historique (`git log -10 --oneline`).
4. **Rapport** au format ci-dessous.

## Format du rapport

### Résumé
Périmètre (fichiers, lignes), résultat du build/tests, et **verdict** : ✅ Prêt à commiter / ⚠️ Commiter après corrections mineures / ❌ Ne pas commiter.

### Résultats des tests
Commande lancée et sortie utile.

### Constats
Tableau trié par sévérité (🔴 Bloquant, 🟠 Important, 🟡 Mineur, 🔵 Suggestion) :

| Sévérité | Fichier:ligne | Constat | Correction proposée |
|---|---|---|---|

### Points positifs
Ce qui est bien fait (bref).

### Fichiers à exclure / à scinder
Le cas échéant.

### Message de commit proposé
Au format de l'historique (`feat:`, `refacto:`, `chore:`…).

## Règles
- Sois factuel : chaque constat cite un fichier et une ligne ; n'invente pas de problème.
- Ne corrige rien toi-même : propose les corrections et attends la décision de l'utilisateur.
- Termine en demandant si l'utilisateur souhaite appliquer les corrections ou procéder au commit.
