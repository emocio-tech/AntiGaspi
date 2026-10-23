# Exposer le serveur MCP avec un tunnel Cloudflare

Le serveur MCP d'AntiGaspi (profil Spring `mcp`, Streamable HTTP sur `/mcp`, tools de l'inventaire : `listerIngredients`, `ajouterIngredients`, `supprimerIngredients`, `modifierIngredients`) n'écoute que sur `127.0.0.1:8080`. Un assistant hébergé dans le cloud, comme ChatGPT, ne peut pas l'atteindre : il lui faut une URL HTTPS publique. Un **tunnel Cloudflare nommé** (`cloudflared`) la fournit sans ouvrir de port : le poste établit une connexion sortante vers Cloudflare, qui relaie les requêtes.

> **Sécurité : lisez ceci avant de commencer.**
> - Le serveur MCP n'a **aucune authentification**. Tant que le tunnel tourne, toute personne connaissant le nom d'hôte peut lire **et modifier** l'inventaire (ajouter, modifier ou supprimer des aliments dans la base MySQL). Les noms d'hôte ne sont pas secrets (ils apparaissent dans les journaux publics de certificats).
> - Le backend sert **tout** sur le port 8080, y compris l'API REST du front (`PUT /api/profile`, `DELETE /api/utensils/{id}`, et `POST /api/chat` ou `POST /api/recipes`, qui consomment le LLM). **Le tunnel ne doit transmettre que `/mcp`** : c'est le rôle de la règle `path` du gabarit `deploy/cloudflared/config.example.yml`. Ne la retirez pas.
> - Arrêtez le tunnel quand vous ne l'utilisez pas.
> - Une authentification (jeton Bearer dans Spring, Cloudflare Access ou OAuth) est à prévoir avant d'y mettre des données réelles : voir la question ouverte du [README](../README.md).

## Prérequis
- Un compte Cloudflare et un **domaine dont le DNS est géré par Cloudflare** (l'offre gratuite suffit).
- Un abonnement ChatGPT qui propose le mode développeur et les connecteurs MCP personnalisés. Les offres concernées changent : vérifiez dans l'interface.
- Java 21 (le backend), et `winget` pour installer `cloudflared` sous Windows.

Dans la suite, remplacez `mcp.exemple.fr` par votre nom d'hôte.

## 1. Installer `cloudflared` et créer le tunnel
```powershell
winget install --id Cloudflare.cloudflared
cloudflared tunnel login                 # ouvre le navigateur ; écrit cert.pem
cloudflared tunnel create antigaspi-mcp  # affiche l'UUID ; écrit <UUID>.json
cloudflared tunnel route dns antigaspi-mcp mcp.exemple.fr
```
`cert.pem` et `<UUID>.json` sont dans `%USERPROFILE%\.cloudflared\`. Ce sont des **secrets** : ne les copiez pas dans le dépôt (le `.gitignore` les écarte au cas où).

## 2. Configurer le tunnel
Copiez [`deploy/cloudflared/config.example.yml`](../deploy/cloudflared/config.example.yml) vers `%USERPROFILE%\.cloudflared\config.yml`, puis remplacez `<UUID>`, le chemin du fichier d'identifiants et le nom d'hôte.

Points importants du gabarit :
- `path: ^/mcp(/.*)?$` : seul `/mcp` traverse le tunnel, le reste reçoit un 404 de Cloudflare sans atteindre le backend.
- `service: http://127.0.0.1:8080` : écrire `127.0.0.1` et non `localhost`, qui peut se résoudre en IPv6 alors que le backend n'écoute qu'en IPv4.
- La dernière règle (`http_status:404`) est obligatoire : `cloudflared` refuse une configuration sans règle finale.

## 3. Lancer
Deux terminaux :
```powershell
# 1. À la racine du projet : backend avec le profil mcp (ne pas le combiner avec demo ni chat)
.\mvnw.cmd -pl backend spring-boot:run "-Dspring-boot.run.profiles=mcp"

# 2. Le tunnel
cloudflared tunnel run antigaspi-mcp
```

## 4. Vérifier avant de brancher ChatGPT
Les deux premières requêtes doivent répondre **404** (bloquées par Cloudflare, elles n'atteignent jamais le backend) :
```powershell
curl.exe -i https://mcp.exemple.fr/api/ingredients
curl.exe -i -X PUT https://mcp.exemple.fr/api/profile
```
La troisième doit répondre **200** avec un en-tête `Mcp-Session-Id` :
```powershell
curl.exe -i -X POST https://mcp.exemple.fr/mcp -H "Content-Type: application/json" -H "Accept: application/json, text/event-stream" -d "{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}"
```
Si `/mcp` répond mal à travers le tunnel (en-tête `Host` ou `Origin` refusé), décommentez `originRequest` dans `config.yml`, redémarrez le tunnel et recommencez.

## 5. Brancher ChatGPT
1. Activer le **mode développeur** : Paramètres, Connecteurs (ou Apps), Avancé.
2. Créer un connecteur : URL `https://mcp.exemple.fr/mcp`, authentification **Aucune**.
3. Demander par exemple : « Qu'est-ce qui périme bientôt dans mon frigo ? ». ChatGPT doit appeler `listerIngredients` et citer les quantités de l'inventaire.

À vérifier à l'essai : le mode développeur accepte un tool au nom arbitraire (les fonctions de recherche approfondie, elles, attendent des tools nommés `search` et `fetch`).

## Dépannage
| Symptôme | Piste |
|---|---|
| `502` de Cloudflare | Le backend n'est pas lancé, ou n'écoute pas sur `127.0.0.1:8080`. |
| `404` sur `/mcp` | La règle `path` ou le nom d'hôte ne correspond pas : relire `config.yml`. |
| ChatGPT n'arrive pas à se connecter | Refaire les trois `curl` ci-dessus ; vérifier que l'URL se termine par `/mcp`. |
| Le tunnel ne démarre pas | `cloudflared tunnel info antigaspi-mcp` ; vérifier le chemin de `credentials-file`. |

## Tout retirer
```powershell
cloudflared tunnel delete antigaspi-mcp
```
Supprimez aussi l'enregistrement DNS `mcp` dans le tableau de bord Cloudflare (DNS), puis, dans ChatGPT, le connecteur.
