# Variables d'environnement

## Vercel frontend

Le deploiement Vercel publie uniquement le frontend Angular. Ajouter ces variables dans Vercel :

```text
API_URL=https://URL_PUBLIQUE_DE_TON_BACKEND
SUPABASE_URL=https://cyjkygaevdcippenlfed.supabase.co
SUPABASE_ANON_KEY=...
```

En local, `frontend/.env` contient :

```text
API_URL=http://localhost:8080
SUPABASE_URL=https://cyjkygaevdcippenlfed.supabase.co
SUPABASE_ANON_KEY=...
```

Le script `frontend/scripts/write-env.mjs` genere `frontend/src/environments/environment.generated.ts` avant `npm start`, `npm run build`, `npm run watch` et `npm test`.

## Backend Spring Boot

Ces variables sont lues par `backend/src/main/resources/application.properties`.

```text
SPRING_APPLICATION_NAME=travel-app
SPRING_PROFILES_ACTIVE=prod
PORT=8080

DATABASE_URL=jdbc:postgresql://db.cyjkygaevdcippenlfed.supabase.co:5432/postgres?ssl=true&sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=...

SUPABASE_URL=https://cyjkygaevdcippenlfed.supabase.co
SUPABASE_ANON_KEY=...
SUPABASE_SERVICE_KEY=...

AI_SERVICE_URL=http://localhost:8000

JWT_SECRET=...
JWT_EXPIRATION_MS=86400000

CORS_ALLOWED_ORIGINS=https://TON_FRONT.vercel.app,http://localhost:4200
CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS

SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=...
```

Ne jamais mettre les vraies valeurs de `DATABASE_PASSWORD`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_KEY`, `JWT_SECRET` ou `SMTP_PASSWORD` dans Git.

## Service IA Python

Ces variables sont lues par `ai-service/app/main.py` et les modules du pipeline IA.

```text
AI_WORKDIR=workdir
AI_RENDER_VIDEO=false
AI_TOP_PHOTOS=50
AI_MAX_EPISODES=6

AI_LLM_API_KEY=...
GROQ_API_KEY=...
GOOGLE_VISION_KEY=...
GEMINI_API_KEY=...
```

`AI_RENDER_VIDEO=false` genere le rapport photo et le script narratif sans rendre de MP4. Mettre `true` seulement si le service dispose d'assez de CPU/RAM.

Ne jamais mettre de cle Groq, Gemini ou Google Vision dans le code. Utiliser uniquement les variables Render/locales.

## Render backend gratuit

Le fichier `render.yaml` permet de deployer le backend Spring Boot sur un Web Service Render gratuit.

Dans Render, renseigner au minimum :

```text
CORS_ALLOWED_ORIGINS=https://TON_FRONT.vercel.app,http://localhost:4200
JWT_SECRET=...
AI_SERVICE_URL=https://TON_AI_SERVICE.onrender.com
```

Puis, quand la base Supabase est utilisee par le code :

```text
DATABASE_URL=jdbc:postgresql://db.cyjkygaevdcippenlfed.supabase.co:5432/postgres?ssl=true&sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=...
SUPABASE_URL=https://cyjkygaevdcippenlfed.supabase.co
SUPABASE_ANON_KEY=...
SUPABASE_SERVICE_KEY=...
```

Une fois le backend deploye, copier son URL Render dans Vercel avec les infos publiques Supabase Auth :

```text
API_URL=https://TON_BACKEND.onrender.com
SUPABASE_URL=https://cyjkygaevdcippenlfed.supabase.co
SUPABASE_ANON_KEY=...
```

Ne jamais mettre `SUPABASE_SERVICE_KEY` dans Vercel. Cette clé reste uniquement côté backend Render.

## Supabase Auth

Dans Supabase, configurer les redirects :

```text
Authentication > URL Configuration
Site URL = https://TON_FRONT.vercel.app
Redirect URLs =
  https://TON_FRONT.vercel.app/**
  http://localhost:4200/**
```

Activer ensuite les providers souhaites dans :

```text
Authentication > Providers
```

La connexion email/mot de passe fonctionne avec le provider Email. Le bouton Google fonctionne seulement si le provider Google est active et configure dans Supabase.
