---
name: decouverte-projet
description: Analyse un projet inconnu fraîchement récupéré : résume son rôle, son architecture, ses conventions, ses prérequis, ses commandes, comment le démarrer et ses pièges connus
---

Objectif : donner une vue précise mais concise d'un projet dont je ne connais pas le code, de façon à pouvoir ensuite répondre à des questions sur l'architecture, les conventions, les commandes et les pièges connus.

## Étapes
1. **Lecture de la doc** : README, CONTRIBUTING, docs/, CLAUDE.md s'ils existent.
2. **Identification de la stack** : fichiers de build (pom.xml, package.json,
   build.gradle, requirements.txt, pyproject.toml, *.csproj, Dockerfile, Makefile…),
   langages, frameworks, versions imposées.
3. **Structure et architecture** : arborescence de premier niveau, rôle de chaque
   module/dossier principal, points d'entrée (main, serveur, CLI), configuration, CI.
   Identifier aussi :
   - les grands modules et leurs dépendances (qui appelle qui)
   - l'emplacement de la logique métier (fichiers/packages clés)
4. **Prérequis locaux** : versions de JDK/Node/Python/etc., outils de build,
   base de données/services externes, variables d'environnement, ports.
   Vérifier ce qui est déjà installé sur le poste (java -version, node -v, mvn -v…)
   et signaler ce qui manque.
5. **Conventions** : relever par échantillonnage, avec un fichier cité pour chacune :
   nommage (classes, méthodes, fichiers), structure et style des tests
   (où ils vivent, comment ils sont écrits, sans les exécuter),
   gestion des erreurs/exceptions, logging.
6. **Commandes** : installer, builder, tester, linter/formater, lancer.
   Les chercher dans le README, les fichiers de build, la CI et les hooks.
   Indiquer « non configuré » si un outil (linter, par exemple) est absent.
   Les tests sont listés comme commande disponible, mais pas exécutés.
7. **Pièges connus** : ce qui surprend ou casse facilement (TODO/FIXME/HACK,
   code hérité, dépendances épinglées ou obsolètes, config spécifique à l'OS,
   étapes de build non évidentes, ordre d'exécution).
8. **Restitution**, format concis :
   - À quoi sert l'application (2-3 phrases)
   - Stack technique
   - Architecture (modules, qui appelle qui, où vit la logique métier)
   - Structure du projet (arborescence commentée)
   - Prérequis (✅ présent / ❌ manquant sur ce poste)
   - Conventions (nommage, tests, erreurs : un fichier cité pour chacune)
   - Commandes (installer, builder, tester, linter, lancer)
   - Comment démarrer (commandes exactes)
   - Pièges connus
9. **Fichier de rapport** : écrire l'intégralité du rapport (toutes les sections de
   l'étape 8) dans un fichier Markdown `RAPPORT-DECOUVERTE.md` à la racine du projet
   analysé. Demander validation à l'utilisateur avant de l'écrire. Terminer par un
   résumé de 5 lignes maximum dans le chat, avec le chemin du fichier.

## Règles
- Lecture seule sur le projet : le seul fichier créé est RAPPORT-DECOUVERTE.md,
  et uniquement après validation de l'utilisateur. Ne lance ni tests ni build.
  Seules les vérifications de version des outils sont autorisées (java -version, etc.).
- Toute affirmation sur l'architecture ou les conventions cite un fichier réel
  (chemin:ligne). Ne rien inventer.
- Rester concis : pas de recopie du README, synthétiser.
