# Product Requirements Document — T9 Launcher

**Documento:** PRD T9 Launcher
**Versione:** 1.0
**Data:** 2026-04-28
**Owner:** Antonio Fasolato
**Stato:** Bozza basata sull'implementazione corrente

---

## 1. Sintesi esecutiva

T9 Launcher è un launcher Android leggero che permette di cercare e avviare le app installate digitando i numeri di un tastierino in stile T9 (come i vecchi cellulari). L'app si presenta come un overlay traslucido richiamato da una scorciatoia sulla home screen: invece di scorrere il drawer delle app o digitare su una tastiera completa, l'utente preme i digit `2`–`9` e il launcher filtra le app installate in tempo reale.

L'obiettivo è offrire un'esperienza di lancio app **più rapida del drawer standard**, **utilizzabile con una sola mano** e **senza bisogno della tastiera software**.

---

## 2. Contesto e motivazione

I drawer Android standard richiedono molti tap per scorrere o l'apertura della tastiera per cercare. Un sottoinsieme di utenti — power user, persone con dispositivi grandi o chi vuole minimizzare l'attrito — preferisce un launcher specializzato.

Il T9 fornisce una mappatura nota e immediata tra digit numerici e lettere; combinato con il tracking dell'uso ricente, permette di lanciare le app più frequenti in 1–3 tap.

---

## 3. Obiettivi

### 3.1 Obiettivi di prodotto

- Lanciare un'app frequente in **≤ 3 tap** (apertura overlay + 1–2 digit + tap sull'app).
- **Zero tastiera software**: l'interazione è interamente sui pulsanti del card.
- **Overlay non invasivo**: il wallpaper resta visibile, l'overlay si chiude tappando fuori o lanciando un'app.
- Esperienza **localizzata** in italiano e inglese.

### 3.2 Obiettivi di non-prodotto

- **Privacy by design**: nessun dato esce dal dispositivo. Tutta la cronologia di lancio e installazione vive in `SharedPreferences` locale.
- **Footprint minimo**: nessuna dipendenza pesante, nessun servizio in background, nessun broadcast receiver registrato staticamente.
- **Manutenibilità**: app a singola Activity con logica concentrata in pochi file Kotlin.

### 3.3 Non obiettivi

- **Non** sostituire il launcher di sistema (non è un home replacement, non gestisce widget, non ha drawer permanente).
- **Non** sincronizzare cronologia o impostazioni tra dispositivi.
- **Non** offrire ricerca web, contatti o azioni di sistema generiche (sebbene esista uno scaffolding `SettingsRepository` non ancora integrato).
- **Non** supportare device sotto Android 13 (vincolato da `setBackgroundBlurRadius`).

---

## 4. Utenti target

| Persona | Bisogno | Come T9 Launcher risponde |
|---------|---------|---------------------------|
| Power user mobile | Lancio rapido one-hand di app frequenti | Tastierino T9, sorting per frequenza ultimi 10 giorni |
| Utente con dispositivo grande | Difficoltà a raggiungere drawer/tastiera | Card che si posiziona vicino al tap di apertura |
| Utente attento alla privacy | Non vuole launcher che traccia uso lato cloud | Tutti i dati restano locali in SharedPreferences |
| Utente nostalgico T9 | Esperienza di scrittura predittiva tipo feature phone | Mapping classico 2→ABC … 9→WXYZ |

---

## 5. Funzionalità

### 5.1 Apertura dell'overlay

- L'utente aggiunge una scorciatoia "T9 Launcher" sulla home screen.
- Tap → si apre `MainActivity` con tema `Theme.T9Launcher.Launcher` (traslucido, dim 0.6, blur sfondo 20px).
- La card si **posiziona intelligentemente**: nei 4 quadranti dello schermo in base al `sourceBounds` del tap della scorciatoia, con margini di 20dp (laterale) e 140dp (basso). Se `sourceBounds` non è disponibile, viene centrata.
- Tap fuori dalla card → `finishAndRemoveTask()`. L'activity ha `excludeFromRecents=true` e `taskAffinity=""` per non comparire nei recenti.

### 5.2 Tastierino T9

Layout 3×3 con i digit 2–9 e un tasto **Clear**.

| Digit | Lettere |
|-------|---------|
| 2 | A B C |
| 3 | D E F |
| 4 | G H I |
| 5 | J K L |
| 6 | M N O |
| 7 | P Q R S |
| 8 | T U V |
| 9 | W X Y Z |

Comportamenti:

- **Tap su digit** → appende il carattere alla query corrente (`currentDigits`) e ricalcola la lista filtrata.
- **Tap su Clear** → svuota la query.
- **Long-press su Clear** → apre `OptionsActivity` (impostazioni).

### 5.3 Algoritmo di matching T9

- Il nome dell'app viene splittato per delimitatori `[\s\-_.]+` in parole.
- Per ogni parola: la query matcha se per ogni `i ∈ [0, digits.length)`, `word[i]` appartiene al set di lettere mappate da `digits[i]` (oppure è esattamente quel digit, in `MainActivity.wordMatchesT9`).
- Una app matcha se **almeno una** delle sue parole matcha la query.
- Esempio: query `9 2` → matcha "WhatsApp" perché `W → 9` e `h → 4`? No: la verifica è posizionale sulla parola, quindi `W (9) h (4)` non matcha `9 2`. Matcha invece "Wallet App" perché `W (9) a (2)` matcha `9 2`. (NB: l'esempio del README "92 → WhatsApp" è quindi parzialmente impreciso; il match avviene sulle prime N lettere di una singola parola, non su iniziali di più parole.)
- L'esito viene **evidenziato** nel nome: i primi `digits.length` caratteri della parola vincente ottengono background giallo (`#FFEB3B` in `AppAdapter`, `#A78BFA` viola in `AppPageAdapter`) + grassetto.

### 5.4 Lista risultati e paginazione

- Mostrata in `ViewPager2` con `AppPageAdapter`.
- **3 app per pagina**, swipe orizzontale per scorrere le pagine.
- Indicatori a pallini (`llPageDots`) sotto il viewpager: pieno bianco per la pagina attiva, semitrasparente per le altre. Nascosti se le pagine sono ≤ 1.
- Ogni cella mostra **icona + nome** dell'app. Le icone sono caricate in modo asincrono (un thread per icona) e cachate in `iconCache: HashMap<String, Drawable>`.
- Se non ci sono risultati per la query corrente, viene mostrato `tvNoResults`.

### 5.5 Ordinamento

**Con query digitata:**
1. App che matchano sul nome → ordinate per `launchCount` desc (ultimi 10 giorni), poi per nome asc.
2. Se l'opzione "ricerca in descrizione" è attiva, in coda le app che matchano **solo** sulla descrizione, stesso criterio.

**Senza query (lista vuota):**
1. App lanciate di recente in ordine di **timestamp ultimo lancio** decrescente (solo se l'opzione `showRecentlyLaunched` è attiva).
2. App installata di recente (entro `recentlyInstalledMinutes` minuti) eventualmente messa in cima (solo se `showRecentlyInstalled` è attiva).
3. Tutte le altre app, ordinate alfabeticamente.

### 5.6 Tracking lanci e installazioni (`LaunchTracker`)

- Persistenza in `SharedPreferences("launch_history")` come JSON.
- `recordLaunch(packageName)` → appende `System.currentTimeMillis()` alla lista timestamp di quel package.
- `getLaunchCount(packageName)` → conta i timestamp negli ultimi **10 giorni**.
- `getLastLaunchTimestamps()` → mappa `packageName → max(timestamp)`.
- Su ogni `saveHistory`, i timestamp più vecchi di 10 giorni vengono **purgati** (auto-pruning).
- `recordInstall(packageName)` viene chiamato dal `BroadcastReceiver` su `ACTION_PACKAGE_ADDED` (escluso il replace) e salva il timestamp di installazione in una chiave separata (`install_times`).
- `getRecentlyInstalledApp(maxAgeMs)` → restituisce l'ultimo package installato se entro la finestra.

### 5.7 Caricamento app installate

Strategia in due thread per ridurre il "first paint":

1. **Thread A (priority)** — risolve solo i package noti (recenti lanci + recente install). Query `pm.getApplicationInfo(pkg, 0)` mirate, restituite all'UI in pochi ms.
2. **Thread B (full)** — query `pm.queryIntentActivities(MAIN/LAUNCHER)` completa, escluso il package del launcher stesso. Sostituisce la lista priority quando completa.

Durante il caricamento (concettualmente) si usa `SkeletonAdapter` come placeholder. Le icone vengono caricate lazy in `AppPageAdapter.onBindViewHolder`.

### 5.8 BroadcastReceiver per cambi pacchetto

`MainActivity` registra a runtime un receiver per:
- `ACTION_PACKAGE_ADDED`
- `ACTION_PACKAGE_REMOVED`
- `ACTION_PACKAGE_REPLACED`

con `addDataScheme("package")`. Su ogni evento ricarica la lista app (`loadApps()`), e su install pulito chiama `launchTracker.recordInstall`.

### 5.9 Menu contestuale (long-press app)

- **App Info** → `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` con URI `package:<pkg>`.
- **Uninstall** → `Intent.ACTION_DELETE` con URI `package:<pkg>`.

Entrambi con `FLAG_ACTIVITY_NEW_TASK`.

### 5.10 Avvio app

- Lookup `packageManager.getLaunchIntentForPackage(pkg)`.
- Se trovato: `launchTracker.recordLaunch(pkg)` e `startActivity`.
- Se assente: `Toast` con messaggio di errore (`R.string.error_launch_app`).

### 5.11 Schermata Opzioni (`OptionsActivity`)

Persistenza in `SharedPreferences("t9launcher_options")` via `OptionsRepository`.

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `show_recently_installed` | `true` | Mostra in cima l'app installata di recente |
| `recently_installed_minutes` | `10` | Finestra (minuti) entro cui un'app è considerata "appena installata" |
| `show_recently_launched` | `true` | Quando la query è vuota, ordina prima per ultimi lanci |
| `search_in_description` | `true` | Estende il match T9 anche alla descrizione `applicationInfo` |

UI:
- 3 `SwitchCompat` + 1 `EditText` numerico per i minuti.
- L'`EditText` viene salvato `onPause` se intero positivo.
- Ritorno via freccia indietro dell'ActionBar.

### 5.12 Asset locali e i18n

- `values/strings.xml` (en) e `values-it/strings.xml` (it).
- Tema `Theme.T9Launcher.Launcher` (overlay) e `Theme.T9Launcher` (Options).
- `values-night` per dark mode.
- Stile tasti `T9Key` definito in `themes.xml`.

---

## 6. Flussi utente principali

### 6.1 Lanciare un'app frequente

1. Tap sulla scorciatoia T9 Launcher in home.
2. Card overlay si apre vicino al tap.
3. (Senza digitare nulla) Vede in cima le app lanciate di recente.
4. Tap sull'app → app lanciata, overlay chiuso.

**≈ 2 tap totali.**

### 6.2 Cercare un'app meno frequente

1. Tap sulla scorciatoia.
2. Digita 2–3 cifre T9 (es. `2 6` per `am…`, `bn…` ecc.).
3. La lista si filtra in real time, le iniziali matchanti sono evidenziate.
4. Tap sull'app desiderata.

**≈ 4 tap totali.**

### 6.3 Disinstallare un'app

1. Tap sulla scorciatoia → digita opzionalmente per restringere.
2. Long-press sulla cella dell'app.
3. Selezione "Disinstalla" dal popup → conferma sistema Android.

### 6.4 Modificare le preferenze

1. Tap sulla scorciatoia.
2. Long-press sul tasto Clear.
3. `OptionsActivity` si apre, modifica switch/minuti.
4. Indietro per tornare al launcher di sistema.

---

## 7. Requisiti non funzionali

### 7.1 Performance

- **First paint < 100ms**: la card e il tastierino devono essere visibili immediatamente, anche prima che la lista app sia pronta.
- **Lista priority < 50ms** dopo il primo paint, popolata con le app lanciate/installate di recente.
- **Lista completa** asincrona, senza bloccare il main thread.
- Caching icone in memoria (HashMap) per evitare re-query del PackageManager.

### 7.2 Privacy & sicurezza

- Nessuna richiesta di permessi runtime.
- Nessun accesso a rete (nessuna `INTERNET` permission).
- Tutti i dati persistenti sono in `SharedPreferences` locali, non condivisi.
- Backup Android di sistema: `allowBackup=true` con `backup_rules.xml` e `data_extraction_rules.xml`.

### 7.3 Compatibilità

- **Min SDK:** 33 (Android 13). Vincolato da `Window.setBackgroundBlurRadius`.
- **Target/Compile SDK:** 36.
- **Lingua:** Kotlin.
- **Build:** Gradle Kotlin DSL, AGP 9.2.0, Gradle 9.4.1, JDK 21.

### 7.4 Accessibilità

- Pulsanti con dimensioni adeguate per il touch.
- Contrasto evidenziazione match (giallo/viola) leggibile sia in light che dark.
- (Da verificare) Content description sulle icone e label sui tasti T9.

### 7.5 Localizzazione

- Inglese e italiano. Aggiungere lingue richiede solo nuovi file `values-<locale>/strings.xml`.

---

## 8. Architettura tecnica

### 8.1 Struttura

App **single-activity** (con una `OptionsActivity` separata per le impostazioni). Nessun fragment, nessun framework di navigazione.

```
fasolato.click.t9launcher/
├── MainActivity.kt           # Overlay, T9, loading, sorting, popup
├── AppPageAdapter.kt         # ViewPager2: 3 app per pagina, highlight
├── AppAdapter.kt             # RecyclerView semplice (legacy/alt path)
├── LaunchTracker.kt          # Storage JSON delle finestre di lancio/install (SharedPrefs)
├── OptionsActivity.kt        # Schermata Settings
├── OptionsRepository.kt      # Wrapper SharedPrefs per opzioni
├── SettingsAdapter.kt        # (non integrato in main UI) lista ScorciatoieSistema
├── SettingsEntry.kt          # Modello scorciatoia di sistema
├── SettingsRepository.kt     # Lista hardcoded di Intent settings (45+, IT)
└── SkeletonAdapter.kt        # Placeholder durante load
```

### 8.2 Layer di persistenza

| File SharedPreferences | Contenuto |
|------------------------|-----------|
| `launch_history` | `data` (JSON: package → array di timestamp), `install_times` (JSON: package → ts) |
| `t9launcher_options` | Quattro chiavi descritte in §5.11 |

### 8.3 Threading

- **Main thread:** UI, T9 input, filtering (operazioni O(N) su lista in-memory).
- **Background threads (raw `Thread {}`):** caricamento app via PackageManager, caricamento singole icone.
- Nessun `coroutine` o `WorkManager` usato attualmente.

### 8.4 Dipendenze

Dichiarate in `gradle/libs.versions.toml` (version catalog), referenziate via `libs.*`. Stack base AppCompat + RecyclerView + ViewPager2; nessuna libreria di terze parti pesante.

---

## 9. Edge case e comportamenti definiti

| Caso | Comportamento |
|------|---------------|
| Nessuna app installata che matcha | `tvNoResults` visibile, lista vuota, niente pallini |
| `getLaunchIntentForPackage` ritorna null | Toast con `error_launch_app`, app non lanciata |
| `sourceBounds` assente (lancio non da shortcut) | Card centrata sullo schermo |
| App auto-launcher esclusa | `filter { it.activityInfo.packageName != packageName }` in `loadApps` |
| Prima esecuzione (history vuota) | Lista priority vuota, full load è il primo paint |
| Package replaced | Receiver triggera `loadApps()`, ma `recordInstall` salta (`EXTRA_REPLACING=true`) |
| `OptionsActivity` con minuti = 0 o non numerico | Il valore non viene salvato (`onPause`) |
| Long-press su Clear | Imposta `launchingOptions=true` per non chiudere l'overlay in `onStop` |

---

## 10. Distribuzione e CI/CD

### 10.1 Build types

- **Debug:** sideload, `assembleDebug`.
- **Release:** firmato, signing config legge da env vars `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Localmente i valori sono in `keystore.properties` (gitignored). Keystore: `t9launcher.jks` in repo root (gitignored).

### 10.2 GitHub Actions

`.github/workflows/build.yml`:
- Trigger: push su `main`.
- Output: APK release firmato, caricato come artifact (retention 7 giorni).
- Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

### 10.3 Distribuzione attuale

- **GitHub Releases** (canale primario per gli utenti): APK firmato scaricabile.
- **Sideload** (sviluppatori): `./gradlew installDebug` o ADB.
- **Play Store**: non ancora pubblicato. Procedura manuale documentata in `TONY.md` (creazione keystore, account dev, bundle release `.aab`, privacy policy, scheda store, IARC rating).

---

## 11. Metriche di successo

> Le metriche sono concettuali: l'app **non** invia telemetria, quindi sono valutabili solo via testing manuale o ricerca utenti esplicita.

- **Tempo medio per lanciare app frequente:** target ≤ 2 secondi dall'apertura overlay.
- **Tap medi per lancio:** target ≤ 3.
- **Stabilità:** zero crash su device Android 13–16 nei flussi principali.
- **Cold start overlay:** target < 300ms su device di fascia media.

---

## 12. Roadmap e backlog (non commissionato)

Spunti emersi dal codice attuale ma non integrati:

- **Integrazione `SettingsRepository`**: scaffolding di 45+ scorciatoie Android Settings (in italiano) con `Intent` action già pronti. Rimosso dal flusso principale; potrebbe tornare come modalità alternativa.
- **`AppAdapter`** sembra un percorso legacy/alternativo rispetto a `AppPageAdapter`; valutare rimozione.
- **`SkeletonAdapter`** non risulta wireato in `MainActivity` corrente: potrebbe essere ricollegato per migliorare la percezione di velocità durante il `loadApps`.
- **i18n estesa**: oltre EN/IT.
- **Internazionalizzazione del matching T9**: oggi è ASCII; nomi accentati (`É`, `Ç`) potrebbero richiedere normalizzazione.
- **Tema custom user-selectable** (oltre al dark/light di sistema).
- **Pubblicazione su Play Store** (vedi `TONY.md`).

---

## 13. Riferimenti

- `README.md` — overview utente, build, sideload.
- `CLAUDE.md` — guida per assistant LLM (architettura, signing, CI).
- `TONY.md` — checklist manuale rilascio Play Store.
- `app/src/main/AndroidManifest.xml` — dichiarazione activity, query intents.
- Sorgenti Kotlin in `app/src/main/java/fasolato/click/t9launcher/`.
