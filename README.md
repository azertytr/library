# BiblioScan

Application Android qui utilise l'appareil photo pour scanner le code-barres ISBN d'un livre, récupérer ses métadonnées, et gérer une collection personnelle **100 % locale**.

## Fonctionnalités

- **Scan** du code-barres ISBN (EAN-13) via CameraX + ZXing (pas de dépendance Google Play Services).
- **2 modes de scan**, persistés entre les sessions :
  - **Collection** : chaque scan ajoute automatiquement le livre à la bibliothèque locale.
  - **Brocante** : chaque scan vérifie seulement si le livre est déjà possédé, sans jamais écrire dans la bibliothèque — idéal pour éviter les doublons en vide-grenier.
- **Autocomplétion** des métadonnées (titre, auteurs, couverture, résumé...) via Google Books, avec repli sur Open Library si indisponible.
- **Statut visuel immédiat** : ✅ vert si le livre est déjà dans la collection, ❌ rouge sinon.
- **Tri de la bibliothèque** par titre (avec regroupement par série), date d'ajout ou auteur, préférence mémorisée.
- **Détection de série** best-effort à partir des métadonnées (champ série Open Library, ou motifs "Tome N" / "#N" dans le titre/sous-titre), avec regroupement automatique dans la bibliothèque (mode de tri "Titre").
- **Stockage 100 % local** (base Room/SQLite) — aucune donnée de collection ne quitte l'appareil, aucun compte.
- **Export / import JSON** de toute la bibliothèque via le sélecteur de fichiers système (Storage Access Framework).

Seul le lookup ISBN au moment du scan fait un appel réseau ; le reste (collection, export/import) est entièrement local.

## Stack technique

- Kotlin + Jetpack Compose (Material 3)
- CameraX + ZXing pour le scan de codes-barres
- Room pour la persistance locale
- Retrofit + kotlinx.serialization pour les appels Google Books / Open Library
- Coil pour le chargement des couvertures

## Build

```bash
./gradlew :app:assembleDebug
```

L'APK de debug est généré dans `app/build/outputs/apk/debug/app-debug.apk`.

Nécessite le SDK Android (compileSdk 35, minSdk 26) et un JDK 17.
