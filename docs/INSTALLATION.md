# Installation du projet NFT - Never Forget Your Trip

Guide V4 pour installer, lancer, tester et presenter le projet.

## 1. Prerequis

- Git
- Node.js 22 LTS et npm
- Java 21
- Python 3.12
- PostgreSQL 16 si une base locale est utilisee
- FFmpeg si le service IA rend des videos

macOS avec Homebrew :

```bash
brew install node openjdk@21 python postgresql@16 ffmpeg
```

Verifier :

```bash
git --version
node -v
npm -v
java -version
python3 --version
ffmpeg -version
```

## 2. Recuperer le projet

```bash
git clone https://github.com/oxygenzoo/projet_IP_I2_NFT.git
cd projet_IP_I2_NFT
git checkout develop
```

Structure :

```text
frontend/     Application Angular
backend/      API Spring Boot
ai-service/   Service IA Python / FastAPI
docs/         Documentation projet
```

## 3. Variables d'environnement

Ne jamais commit de vrais secrets.

Frontend local, fichier `frontend/.env` :

```text
API_URL=http://localhost:8080
SUPABASE_URL=https://cyjkygaevdcippenlfed.supabase.co
SUPABASE_ANON_KEY=cle_publique_supabase
```

Backend local :

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/nft_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=password
JWT_SECRET=change_me
AI_SERVICE_URL=http://localhost:8000
CORS_ALLOWED_ORIGINS=http://localhost:4200
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app,http://localhost:*
```

Service IA :

```text
AI_WORKDIR=workdir
AI_RENDER_VIDEO=false
AI_TOP_PHOTOS=50
AI_MAX_EPISODES=6
AI_LLM_API_KEY=
GROQ_API_KEY=
GOOGLE_VISION_KEY=
GEMINI_API_KEY=
```

Page admin :

```text
Dans Supabase Auth, ajouter role=admin dans app_metadata ou user_metadata de l'utilisateur.
```

## 4. Installer le frontend

```bash
cd frontend
npm install
npm start
```

URL :

```text
http://localhost:4200
```

Build :

```bash
npm run build
```

## 5. Installer le backend

```bash
cd backend
chmod +x mvnw
./mvnw test
./mvnw clean package
./mvnw spring-boot:run
```

URL :

```text
http://localhost:8080
```

## 6. Installer le service IA

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
python -m compileall .
uvicorn app.main:app --reload --port 8000
```

URL :

```text
http://localhost:8000
```

## 7. Lancer en local

Terminal 1 :

```bash
cd backend
./mvnw spring-boot:run
```

Terminal 2 :

```bash
cd frontend
npm start
```

Terminal 3, seulement si le pipeline IA est teste :

```bash
cd ai-service
source .venv/bin/activate
uvicorn app.main:app --reload --port 8000
```

## 8. Lancer les tests

Backend :

```bash
cd backend
./mvnw test
./mvnw clean package
```

Frontend :

```bash
npm run build --prefix frontend
```

Service IA :

```bash
cd ai-service
python -m compileall .
```

## 9. Parcours de validation V4

```text
1. Se connecter.
2. Ouvrir /home : voyages, episodes, etats loading/error/empty.
3. Ouvrir /upload : consentement, import image, erreurs fichiers.
4. Ouvrir /preferences puis /generating avec des photos.
5. Ouvrir /episode/:id et /player/:id depuis une carte episode.
6. Ouvrir /admin avec un compte role=admin.
7. Tester mobile et desktop avec les outils navigateur.
```

## 10. Erreurs frequentes

`Supabase Auth n'est pas configure` :

```text
Verifier SUPABASE_URL et SUPABASE_ANON_KEY dans frontend/.env ou Vercel.
```

`API indisponible` :

```text
Verifier que le backend tourne sur http://localhost:8080 et que API_URL pointe dessus.
```

Erreur CORS :

```text
Ajouter le domaine exact dans CORS_ALLOWED_ORIGINS, ou `https://*.vercel.app` dans CORS_ALLOWED_ORIGIN_PATTERNS cote backend Render.
```

`/admin` redirige vers `/home` :

```text
Le compte connecte n'a pas role=admin dans Supabase.
```

`./mvnw: Permission denied` :

```bash
cd backend
chmod +x mvnw
```

`npm install` echoue :

```text
Utiliser Node.js 22 LTS et npm 11, puis relancer npm install dans frontend/.
```

## 11. Render et Vercel

Vercel frontend :

```text
Build command : npm run build --prefix frontend
Output folder : frontend/dist/frontend/browser
Variables     : API_URL, SUPABASE_URL, SUPABASE_ANON_KEY
```

Render backend :

```text
Build command : cd backend && ./mvnw clean package -DskipTests
Start command : java -jar backend/target/*.jar
Variables     : DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD, CORS_ALLOWED_ORIGINS, CORS_ALLOWED_ORIGIN_PATTERNS, JWT_SECRET, AI_SERVICE_URL
```

Supabase Auth redirects :

```text
Site URL      : https://TON_FRONT.vercel.app
Redirect URLs : https://TON_FRONT.vercel.app/**, http://localhost:4200/**
```
