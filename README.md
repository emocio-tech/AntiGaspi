# AntiGaspi 🥕

Application de lutte contre le gaspillage alimentaire : on suit ce qu'on a dans le frigo et le garde-manger, on voit d'abord ce qui périme bientôt, et on génère des recettes pour l'utiliser.

> Statut : **initialisation** — stack et maquettes en cours de définition.

## Fonctionnalités prévues

### 1. Inventaire priorisé par date de péremption
- Liste des items stockés, par emplacement (**frigo**, **garde-manger**, éventuellement **congélateur**).
- Pour chaque item : nom, quantité / unité, emplacement, date de péremption (DLC / DDM), date d'ajout.
- Tri par défaut : **péremption la plus proche en premier**.
- Indicateur visuel d'urgence (ex. : périmé / < 2 jours / < 7 jours / OK).
- Ajout, modification, suppression, marquage « consommé » ou « jeté » (pour d'éventuelles statistiques).

### 2. Génération de recettes par LLM
- Un appel LLM construit des recettes à partir de l'inventaire, **en priorité avec les items qui périment le plus tôt**.
- Options envisagées : nombre de personnes, temps de préparation, contraintes alimentaires.
- Sortie structurée (titre, ingrédients utilisés / manquants, étapes) pour un affichage propre.

### 3. Serveur MCP
- Exposer l'inventaire via un serveur [MCP (Model Context Protocol)](https://modelcontextprotocol.io) pour qu'un assistant compatible (Claude Desktop, Claude Code…) puisse le consulter et le modifier.
- Outils (tools) envisagés :
  - `list_items` — lister l'inventaire (filtre par emplacement, tri par péremption)
  - `get_expiring_items` — items qui périment sous N jours
  - `add_item` / `update_item` / `remove_item`
  - `suggest_recipes` — recettes à partir des items prioritaires

### 4. Interface minimaliste
- Une vue principale : la liste priorisée.
- Ajout rapide d'un item.
- Une vue « Recettes ».
- Design à valider avec le maquettage (voir [docs/maquettes/](docs/maquettes/)).

## Architecture (proposition)

```
┌──────────────┐      ┌──────────────────┐      ┌──────────┐
│  Interface   │ ───▶ │   API / backend  │ ───▶ │  Stockage│
│  (web)       │      │  (inventaire,    │      │  (DB)    │
└──────────────┘      │   recettes)      │      └──────────┘
                      └───────┬──────────┘
                              │ appel LLM
┌──────────────┐              ▼
│ Client MCP   │ ───▶  Serveur MCP (réutilise la même logique métier)
│ (assistant)  │
└──────────────┘
```

La logique métier (inventaire, priorisation, construction du prompt) est partagée entre l'API web et le serveur MCP, pour ne pas la dupliquer.

## Structure du repo (cible)

```
AntiGaspi/
├── docs/
│   └── maquettes/     # Maquettes / wireframes de l'interface
├── backend/           # API + logique métier (à créer)
├── frontend/          # Interface web (à créer)
├── mcp/               # Serveur MCP (à créer)
├── .env.example       # Variables d'environnement à copier dans .env
└── README.md
```

## Démarrage

> À compléter une fois la stack choisie.

```bash
git clone git@github.com:emocio-tech/AntiGaspi.git
cd AntiGaspi
cp .env.example .env   # puis renseigner la clé API du LLM
```

## Décisions à prendre

- [ ] Stack backend (ex. : Python/FastAPI ou Node/TypeScript)
- [ ] Stack frontend (ex. : React/Vite, Svelte, ou simple HTML)
- [ ] Stockage (SQLite pour commencer ?)
- [ ] Fournisseur / modèle LLM
- [ ] Maquettes de l'interface

## Roadmap

1. **MVP inventaire** — CRUD des items + liste triée par péremption
2. **Recettes** — appel LLM à partir des items prioritaires
3. **MCP** — exposition de l'inventaire et des recettes en tools MCP
4. **UI** — interface minimaliste d'après les maquettes
5. Bonus — scan de code-barres, notifications avant péremption, statistiques de gaspillage

## Contribuer

- Branche par fonctionnalité : `feat/<nom>`, `fix/<nom>`
- Pull request vers `main`
- Ne jamais commiter le fichier `.env` ni de clé API
