# Installation du projet NFT — Never Forget Your Travel

1. Présentation du projet

NFT (Never Forget Your Travel) est une web app permettant aux utilisateurs d’uploader leurs photos de voyage afin de générer automatiquement un épisode souvenir sous forme de diaporama vidéo.

Le projet est composé de 3 parties :

```
frontend/     → Application Angular
backend/      → API Spring Boot
ai-service/   → Service IA Python / FastAPI
```

Stack technique :

```
Frontend : Angular
Backend : Spring Boot
IA : Python / FastAPI
DB : PostgreSQL
CI/CD : GitHub Actions
Qualité code : SonarQube Cloud
Versionning : GitHub
```
⸻

2. Prérequis à installer

Cette documentation part du principe que la machine est vierge.

2.1 Installer Git

Pour Mac :
```
git --version
```
Si Git n’est pas installé, macOS proposera d’installer les Command Line Tools.

Sinon :
```
xcode-select --install
```
Vérifier :
```
git --version
```
⸻

2.2 Installer Homebrew

Homebrew permet d’installer facilement les outils nécessaires.
```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
Vérifier :
```
brew --version
```
⸻

2.3 Installer Node.js et npm

Angular nécessite Node.js.
```
brew install node
```
Vérifier :
```
node -v
npm -v
```
Installer Angular CLI :
```
npm install -g @angular/cli
```
Vérifier :
```
ng version
```
⸻

2.4 Installer Java

Le backend utilise Spring Boot avec Java 21.
```
brew install openjdk@21
```
Ajouter Java au PATH :
```
echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```
Vérifier :
```
java -version
```
Résultat attendu :
```
java version "21..."
```
⸻

2.5 Installer Python

Le service IA utilise Python.
```
brew install python
```
Vérifier :
```
python3 --version
pip3 --version
```
⸻

2.6 Installer PostgreSQL
```
brew install postgresql@16
```
Démarrer PostgreSQL :
```
brew services start postgresql@16
```
Vérifier :
```
psql --version
```
Créer une base locale :
```
createdb nft_db
```
⸻

2.7 Installer FFmpeg

FFmpeg servira à générer les vidéos.
```
brew install ffmpeg
```
Vérifier :
```
ffmpeg -version
```
⸻

3. Récupérer le projet GitHub

Se placer dans le dossier souhaité :
```
cd ~/Documents
```
Cloner le projet :
```
git clone https://github.com/oxygenzoo/projet_IP_I2_NFT.git
```
Entrer dans le projet :
```
cd projet_IP_I2_NFT
```
Vérifier les branches :
```
git branch -a
```
Se placer sur develop :
```
git checkout develop
git pull origin develop
```
⸻

4. Structure du projet
```
projet_IP_I2_NFT/
├── frontend/
├── backend/
├── ai-service/
├── .github/
│   └── workflows/
│       └── ci.yml
├── sonar-project.properties
├── README.md
└── docker-compose.yml
```
⸻

5. Installer le frontend Angular

Se placer dans le frontend :
```
cd frontend
```
Installer les dépendances :
```
npm install
```
Lancer le frontend :
```
ng serve
```
Ou :
```
npm start
```
L’application sera disponible sur :

http://localhost:4200

Tester le build :
```
npm run build
```
Revenir à la racine :
```
cd ..
```
⸻

6. Installer le backend Spring Boot

Se placer dans le backend :
```
cd backend
```
Vérifier que le wrapper Maven existe :
```
ls
```
Il doit y avoir :
```
mvnw
mvnw.cmd
pom.xml
```
Donner les droits d’exécution si nécessaire :
```
chmod +x mvnw
```
Installer et vérifier le projet :
```
./mvnw clean verify
```
Lancer le backend :
```
./mvnw spring-boot:run
```
Par défaut, l’API sera disponible sur :

http://localhost:8080

Revenir à la racine :
```
cd ..
```
⸻

7. Installer le service IA Python

Se placer dans le service IA :
```
cd ai-service
```
Créer l’environnement virtuel :
```
python3 -m venv .venv
```
Activer l’environnement virtuel :
```
source .venv/bin/activate
```
Installer les dépendances :
```
pip install -r requirements.txt
```
Tester que le code Python compile :
```
python -m compileall .
```
Lancer le service IA si un fichier principal existe, par exemple :
```
uvicorn main:app --reload --port 8000
```
Le service IA sera disponible sur :

http://localhost:8000

Désactiver l’environnement virtuel :
```
deactivate
```
Revenir à la racine :
```
cd ..
```
⸻

8. Variables d’environnement

Ne jamais commit de vrais secrets dans GitHub.

Créer des fichiers d’exemple :
```
.env.example
backend/.env.example
frontend/.env.example
ai-service/.env.example
```
Exemple pour le backend :

DATABASE_URL=jdbc:postgresql://localhost:5432/nft_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=password
JWT_SECRET=change_me
AI_SERVICE_URL=http://localhost:8000

Exemple pour le frontend :

API_URL=http://localhost:8080

Exemple pour le service IA :

UPLOAD_DIR=uploads
GENERATED_DIR=generated

Chaque développeur doit créer ses propres fichiers .env.

Les fichiers .env doivent être ignorés par Git.

Vérifier dans .gitignore :
```
.env
.env.local
backend/.env
frontend/.env
ai-service/.env
```
⸻

9. Lancer tout le projet en local

Ouvrir 3 terminaux.

Terminal 1 — Frontend
```
cd frontend
npm install
ng serve
```
Terminal 2 — Backend
```
cd backend
./mvnw spring-boot:run
```
Terminal 3 — Service IA
```
cd ai-service
source .venv/bin/activate
uvicorn main:app --reload --port 8000
```
Adresses locales :
```
Frontend Angular : http://localhost:4200
Backend API      : http://localhost:8080
Service IA       : http://localhost:8000
PostgreSQL       : localhost:5432
```
⸻

10. Workflow Git

Branches principales
```
main       → version stable / production
develop    → version de développement / préproduction
feature/*  → nouvelles fonctionnalités
fix/*      → corrections
hotfix/*   → corrections urgentes sur main
```
### Ne jamais développer directement sur main.

⸻

Récupérer les dernières modifications

Avant de commencer à travailler :
```
git checkout develop
git pull origin develop
```
⸻

Créer une nouvelle branche de feature

Exemple :
```
git checkout -b feature/upload-photos
```
Travailler normalement, puis vérifier :
```
git status
```
Ajouter les fichiers :
```
git add .
```
Créer un commit :
```
git commit -m "feat: add photo upload page"
```
Pousser la branche :
```
git push -u origin feature/upload-photos
```
⸻

11. Faire une Pull Request / Merge Request

Sur GitHub :

Pull Requests → New Pull Request

Choisir :

base: develop
compare: feature/upload-photos

Titre clair :

feat: add photo upload page

Description conseillée :

## Description
Ajout de la page d’upload des photos.
## Changements
- Création du composant Angular
- Ajout du formulaire d’upload
- Préparation de l’appel API backend
## Tests
- Build Angular OK
- CI GitHub Actions OK

Attendre que la CI passe.

Si tout est vert :
```
Merge pull request
```
⸻

12. Merger develop dans main

Quand develop est stable :

Pull Requests → New Pull Request

Choisir :

base: main
compare: develop

Titre :

release: merge develop into main

Attendre la CI.

Si tout est vert :
```
Merge pull request
```
Puis synchroniser en local :
```
git checkout main
git pull origin main
git checkout develop
git pull origin develop
```
⸻

13. GitHub Actions

La CI est dans :
```
.github/workflows/ci.yml
```
Elle vérifie automatiquement :

Angular build
Spring Boot build
Python compile check
SonarQube Cloud analysis

Elle se lance sur :

push sur main
push sur develop
pull request vers main
pull request vers develop

⸻

14. SonarQube Cloud

SonarQube Cloud sert à vérifier :

bugs
code smells
sécurité
duplication
maintenabilité
accessibilité
qualité globale

Le fichier de configuration est :
```
sonar-project.properties
```
Exemple :

sonar.projectKey=oxygenzoo_projet_IP_I2_NFT
sonar.organization=oxygenzoo
sonar.sources=frontend/src,backend/src/main,ai-service
sonar.tests=backend/src/test
sonar.exclusions=**/node_modules/**,**/dist/**,**/target/**,**/.venv/**,**/__pycache__/**
sonar.java.binaries=backend/target/classes
sonar.sourceEncoding=UTF-8

⸻

15. Installer Sonar côté GitHub

15.1 Créer un compte SonarQube Cloud

Aller sur :

https://sonarcloud.io

Se connecter avec GitHub.

Importer le repository :

oxygenzoo/projet_IP_I2_NFT

⸻

15.2 Générer un token Sonar

Dans SonarQube Cloud :

My Account → Security → Generate Tokens

Nom du token :

github-actions-nft

Copier le token.

Attention : il n’est affiché qu’une seule fois.

⸻

15.3 Ajouter le token dans GitHub

Dans GitHub :

Repository → Settings → Secrets and variables → Actions → New repository secret

Nom :

SONAR_TOKEN

Valeur :

token SonarQube Cloud

⸻

16. Erreurs fréquentes Sonar

16.1 SONAR_TOKEN not found

Cause :

Le secret GitHub n’existe pas ou le nom est incorrect.

Solution :

Vérifier dans GitHub :

Settings → Secrets and variables → Actions

Le secret doit s’appeler exactement :

SONAR_TOKEN

Dans le fichier CI :

env:
SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

⸻

16.2 Project not found

Cause :

Mauvais sonar.projectKey ou mauvaise organization.

Solution :

Aller dans SonarQube Cloud → Project Information.

Vérifier :

sonar.projectKey=...
sonar.organization=...

Mettre à jour sonar-project.properties.

⸻

16.3 sonar.java.binaries missing

Cause :

Sonar analyse du Java mais ne trouve pas les fichiers compilés.

Solution :

Vérifier que la CI compile le backend avant Sonar :

- name: Build backend classes
  working-directory: backend
  run: ./mvnw clean compile

Et dans sonar-project.properties :

sonar.java.binaries=backend/target/classes

⸻

17. Résoudre un conflit Git

Pendant un pull ou une Pull Request, GitHub peut indiquer un conflit.

Exemple

CONFLICT (content): Merge conflict in frontend/src/app/app.html

Ouvrir le fichier concerné.

Git affiche :

<<<<<<< HEAD
code actuel
=======
code entrant
feature/...

Garder la bonne version, supprimer les marqueurs :

Puis :
```
git add .
git commit -m "fix: resolve merge conflict"
git push
```
La Pull Request se mettra à jour automatiquement.

⸻

18. Mettre à jour sa branche avec develop

Quand une branche feature est en retard :
```
git checkout feature/ma-feature
git fetch origin
git merge origin/develop
```
S’il y a des conflits, les résoudre.

Puis :
```
git add .
git commit -m "chore: merge develop into feature branch"
git push
```
Alternative plus propre mais plus avancée :
```
git rebase origin/develop
```
À utiliser seulement si vous êtes à l’aise avec Git.

⸻

19. Bonnes pratiques de commit

Format conseillé :

feat: nouvelle fonctionnalité
fix: correction de bug
docs: documentation
style: formatage
refactor: refactorisation
test: ajout/modification de tests
ci: modification CI/CD
chore: tâches diverses

Exemples :
```
git commit -m "feat: add trip creation endpoint"
git commit -m "fix: correct angular accessibility issue"
git commit -m "ci: add sonarqube analysis"
git commit -m "docs: add installation guide"
```
⸻

20. Vérifications avant de push

Avant chaque push :

Frontend
```
cd frontend
npm run build
cd ..
```
Backend
```
cd backend
./mvnw clean verify
cd ..
```
AI service
```
cd ai-service
source .venv/bin/activate
python -m compileall .
deactivate
cd ..
```
Git
```
git status
```
Puis :
```
git add .
git commit -m "message clair"
git push
```
⸻

21. Checklist nouveau développeur

Un nouveau développeur doit faire :
```
□ Installer Git
□ Installer Homebrew
□ Installer Node.js
□ Installer Angular CLI
□ Installer Java 21
□ Installer Python
□ Installer PostgreSQL
□ Installer FFmpeg
□ Cloner le repo GitHub
□ Se placer sur develop
□ Installer frontend
□ Installer backend
□ Installer ai-service
□ Créer les fichiers .env locaux
□ Lancer PostgreSQL
□ Lancer Angular
□ Lancer Spring Boot
□ Lancer FastAPI
□ Créer une branche feature
□ Faire une Pull Request vers develop
```
⸻

22. Commandes utiles rapides

Voir la branche actuelle
```
git branch
```
Voir l’état du repo
```
git status
```
Récupérer les dernières modifications
```
git pull origin develop
```
Créer une branche
```
git checkout -b feature/nom-feature
```
Push une branche
```
git push -u origin feature/nom-feature
```
Revenir sur develop
```
git checkout develop
```
Supprimer une branche locale
```
git branch -d feature/nom-feature
```
Voir les remotes
```
git remote -v
```
⸻

23. Règle d’or du projet

Ne jamais travailler directement sur main.
Toujours créer une branche depuis develop.
Toujours faire une Pull Request.
Toujours attendre que la CI et Sonar passent avant de merge.

⸻