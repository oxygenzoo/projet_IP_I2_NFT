# NFT - Never Forget your Trip

NFT est une application SaaS inspiree des plateformes de streaming.

Le concept : un utilisateur importe ses photos de voyage, puis une IA transforme ses souvenirs en saisons et episodes narratifs, comme une mini-serie personnelle.

Le frontend Angular consomme les APIs deployees via Vercel/Render et Supabase Auth.

## Etat actuel

- Frontend Angular : interface fonctionnelle et responsive.
- Backend Spring Boot : API voyages, episodes, pricing, preferences et generation.
- Service IA Python/FastAPI : pipeline de selection, narration et rendu optionnel.
- CI GitHub Actions et analyse SonarQube Cloud.
- Deploiement frontend configure pour Vercel via `vercel.json`.

## Structure

```text
.
├── frontend/                 # Application Angular
├── backend/                  # API Spring Boot
├── ai-service/               # Service IA Python / FastAPI
├── docs/
│   └── INSTALLATION.md       # Guide detaille Mac + Windows
├── .github/workflows/ci.yml  # CI
├── vercel.json               # Build Vercel du frontend Angular
└── README.md
```

## Ce qui va dans quel fichier ?

Le `README.md` doit rester la porte d'entree du projet :

- expliquer le concept ;
- montrer la structure ;
- donner les commandes rapides ;
- indiquer les URLs locales ;
- expliquer le deploiement ;
- pointer vers les docs detaillees.

Le fichier `docs/INSTALLATION.md` doit contenir :

- l'installation des outils sur Windows et macOS ;
- les commandes detaillees par OS ;
- les problemes frequents ;
- la configuration locale ;
- les commandes longues ou optionnelles.

En pratique : si quelqu'un connait deja Node/Git/Java, le README suffit. Si quelqu'un installe tout depuis zero, il va dans `docs/INSTALLATION.md`.

## Prerequis rapides

Pour lancer le frontend :

- Node.js 22 LTS recommande ;
- npm ;
- Git.

Pour lancer tout le projet :

- Java 21 ;
- Python 3.12 ;
- PostgreSQL 16 si le backend utilise une base locale ;
- FFmpeg si le service IA genere des videos.

Le guide complet pour Windows et macOS est ici : [docs/INSTALLATION.md](docs/INSTALLATION.md).

## Lancer le frontend

Depuis la racine du repository :

```bash
cd frontend
npm install
npm start
```

Ouvrir ensuite :

```text
http://localhost:4200
```

Build de verification :

```bash
npm run build
```

## Commandes par partie

### Frontend Angular

```bash
cd frontend
npm install
npm start
npm run build
npm test
```

### Backend Spring Boot

macOS / Linux :

```bash
cd backend
chmod +x mvnw
./mvnw clean verify
./mvnw spring-boot:run
```

Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

URL par defaut :

```text
http://localhost:8080
```

### Service IA Python

macOS / Linux :

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
python -m compileall .
```

Windows PowerShell :

```powershell
cd ai-service
py -3.12 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
python -m compileall .
```

Si un point d'entree FastAPI est ajoute :

```bash
uvicorn app.main:app --reload --port 8000
```

URL par defaut :

```text
http://localhost:8000
```

## URLs locales

```text
Frontend Angular : http://localhost:4200
Backend API      : http://localhost:8080
Service IA       : http://localhost:8000
PostgreSQL       : localhost:5432
```

## Deploiement Vercel

Le projet Angular est dans `frontend/`, pas a la racine. Le fichier `vercel.json` indique donc a Vercel :

- d'installer les dependances avec `npm install --prefix frontend` ;
- de builder avec `npm run build --prefix frontend` ;
- de publier `frontend/dist/frontend/browser` ;
- de rediriger les routes Angular vers `index.html`.

Important : sur Vercel, ne pas mettre `ng build` directement comme commande de build. Utiliser le script npm :

```text
npm run build --prefix frontend
```

## CI et qualite

La CI est dans :

```text
.github/workflows/ci.yml
```

Elle verifie :

- le build Angular ;
- le build Spring Boot ;
- la compilation Python ;
- l'analyse SonarQube Cloud.

Les actions GitHub sont pinnees par SHA complet pour eviter les alertes de securite Sonar.

## Workflow Git conseille

```bash
git checkout develop
git pull origin develop
git checkout -b feature/nom-de-la-feature
```

Avant de pousser :

```bash
cd frontend
npm run build
cd ..
git status
```

Puis :

```bash
git add .
git commit -m "feat: message clair"
git push -u origin feature/nom-de-la-feature
```

## Documentation utile

- Installation detaillee Windows/macOS : [docs/INSTALLATION.md](docs/INSTALLATION.md)
- Frontend Angular : [frontend/README.md](frontend/README.md)
- Workflow CI : [.github/workflows/ci.yml](.github/workflows/ci.yml)
