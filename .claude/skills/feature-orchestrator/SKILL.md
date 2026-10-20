---
name: feature-orchestrator
description: Orchestre le développement complet d'une feature, de l'exploration au commit, en déléguant à des agents spécialisés (impact-analyst, test-plan-writer, implementer, reviewer, doc-reviewer) et aux skills generate-tests et generate-doc, avec deux validations humaines. À utiliser pour développer une feature de bout en bout.
argument-hint: [description de la feature]
---

# Orchestrateur de feature

Tu développes la feature `$ARGUMENTS` en orchestrant des agents spécialisés. Tu es l'agent principal : tu discutes avec l'utilisateur, tu délègues, tu synthétises et tu décides. Les agents exécutent, ils ne décident pas. Les sous-agents ne peuvent pas en lancer d'autres : tout passe par toi.

## Principes
- Deux validations humaines obligatoires (fin de l'étape 2 et fin de l'étape 5). Ne passe jamais à l'étape suivante sans accord explicite.
- Chaque prompt d'agent est autonome : donne-lui la feature, les décisions déjà prises, les rapports utiles des étapes précédentes et les restrictions de CLAUDE.md.
- Lance en parallèle (un seul message, plusieurs appels Agent) uniquement les agents indépendants en lecture seule.
- Relaie à l'utilisateur l'essentiel de chaque rapport, de façon concise, sans recopier les rapports entiers.
- Respecte CLAUDE.md et les restrictions du projet (par exemple celles d'un atelier).

## Étape 0 : cadrage
Discute avec l'utilisateur pour clarifier l'idée : objectif, comportement attendu, hors-périmètre, contraintes. Si `$ARGUMENTS` est vide ou flou, pose les questions nécessaires. Reformule le besoin en quelques lignes et fais-le confirmer.

## Étape 1 : exploration
Lance l'agent `impact-analyst` avec le besoin cadré. En même temps, lance `test-plan-writer` avec le même besoin (il explorera les tests existants). Attends les deux rapports.

## Étape 2 : plan et plan de test
1. Consolide les deux rapports et rédige :
   - le **plan d'implémentation** : étapes ordonnées, fichier par fichier, avec le changement attendu à chaque étape ;
   - le **plan de test** : version finale cohérente avec le plan d'implémentation, avec les commandes de test.
2. Si des questions ouvertes subsistent, pose-les à l'utilisateur.
3. **VALIDATION HUMAINE n°1** : présente les deux plans, les risques principaux et les questions. Ne continue qu'avec un accord explicite. Si l'utilisateur demande des changements, ajuste (et relance un agent si nécessaire).

## Étape 3 : TDD, tests d'abord puis implémentation
Le projet suit le TDD (voir CLAUDE.md). Aucun code de production n'est écrit avant les tests.

### 3a. Tests d'abord (Red)
Écris les tests du plan de test validé avec le skill `generate-tests`. Seuls des stubs minimaux, sans logique, sont autorisés pour que ça compile.
- Lance les tests ciblés et vérifie qu'ils **échouent pour la bonne raison** : assertion ou `UnsupportedOperationException`, pas une erreur de compilation ni de configuration.
- Un test qui passe déjà est suspect : il ne teste rien de nouveau. Corrige-le ou retire-le avec l'accord de l'utilisateur.

### 3b. Implémentation (Green, puis Refactor)
Lance l'agent `implementer` avec les plans validés et la liste des tests en échec. Il écrit le code qui les fait passer, **sans modifier les tests**, puis lance les tests indiqués.
- Si l'implementer remonte un blocage ou un écart, c'est à toi de décider : tranche si c'est mineur, sinon consulte l'utilisateur, puis relance l'implementer avec le plan mis à jour.
- Vérifie son compte rendu : statut, tests passés, écarts. Ne considère pas l'étape finie sur la seule affirmation de l'agent : relance toi-même la commande de test ciblée si le résultat est douteux.

## Étape 4 : review du code
1. Lance l'agent `reviewer` avec les plans validés.
2. Traite le rapport : les bloquants et importants sont corrigés en relançant l'`implementer` avec une liste de corrections précise. Fais au plus deux allers-retours reviewer/implementer, puis remonte à l'utilisateur ce qui reste.
3. Si le reviewer signale des lacunes de tests, comble-les avec le skill `generate-tests` (le plan de test validé fait foi).
4. Relance les tests ciblés après les corrections.

## Étape 5 : documentation et relecture
1. Génère la doc avec le skill `generate-doc` sur le diff final.
2. Lance l'agent `doc-reviewer` sur la doc produite. Corrige les erreurs factuelles et les manques signalés (au plus un aller-retour).
3. **VALIDATION HUMAINE n°2** : présente le bilan complet : résumé des changements, résultats de tests, points du reviewer et du doc-reviewer, écarts restants. Ne continue qu'avec un accord explicite.

## Étape 6 : commit
- Vérifie `git status` et `git diff` : seuls les fichiers de la feature doivent être inclus.
- Rédige un message de commit clair (résumé, puis le pourquoi), avec l'attribution demandée par le système si elle est définie.
- Commite uniquement après l'accord de la validation n°2. Ne pousse pas, sauf demande explicite de l'utilisateur.
- Termine par un résumé concis : ce qui a été fait, fichiers touchés, tests lancés, ce qui reste éventuellement.

## En cas de problème
- Test qui échoue pour une raison non prévue, plan incohérent avec le code réel, conflit entre rapports : arrête-toi, expose les faits et propose une recommandation à l'utilisateur.
- Ne contourne jamais un test, un hook ou une restriction du projet pour avancer.
