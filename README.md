# Sunday City Companion Ultimate — Android

App companion indipendente e non ufficiale. Non modifica Sunday City, non legge file o traffico del gioco, non accede agli account e non comunica con i server.

## Funzioni versione 0.3.0
- Home con riepilogo di garage, stanze e obiettivi.
- Garage virtuale con catalogazione personale dei veicoli.
- Casa Designer 2D: creazione stanze, stili, catalogo mobili, griglia 5×5, posizionamento automatico negli spazi liberi, spostamento senza sovrapposizioni e rimozione con pulizia degli arredi della stanza.
- Sandbox Wallet con soldi e diamanti illimitati simulati solo nell'app.
- Obiettivi personali.
- Guide e consigli.
- Schermate con scorrimento corretto anche su telefoni compatti.
- Workflow GitHub Actions per compilare automaticamente l’APK debug e pubblicarlo come artifact.
- Database Room locale con migrazione automatica dal database 1.0.

## Requisiti
- Android Studio recente, JDK 17, Android SDK 35.
- Aprire la cartella in Android Studio, sincronizzare Gradle e avviare su Android 8.0 o superiore.

## Build

### In locale
Aprire la cartella in Android Studio, sincronizzare Gradle e avviare `:app:assembleDebug`.

### GitHub Actions
Il workflow `.github/workflows/android.yml` compila l'APK debug a ogni push o pull request su `main`/`master`, oppure manualmente da **Actions → Android CI → Run workflow**. Al termine, scaricare l'artifact `SundayCityCompanion-debug-apk` dalla run completata.

Nota: in questa sessione non è disponibile l'ambiente Android SDK/Gradle, quindi il workflow è stato predisposto ma non eseguito qui. L'APK non è ancora verificato.

## Limiti attuali
La casa è un designer 2D a griglia, non ancora una scena 3D. I mobili si posizionano e si spostano con controlli direzionali; la disposizione viene salvata localmente.

## Privacy
Dati salvati localmente. Nessun login Sunday City richiesto. Nessuna integrazione con il gioco originale.

Sunday City è un marchio di KEFIR. Questa app è indipendente, non ufficiale e non affiliata a KEFIR.
