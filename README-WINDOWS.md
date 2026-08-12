# Hoops Edge - Ghid Rulare & Compilare pe Windows

Acest proiect este o aplicație nativă Android (scrisă în Kotlin și Jetpack Compose) de previziuni și analize baschet NBA/WNBA.

---

## Opțiunea 1: Rulare Directă cu Emulator pe Windows (Recomandat)

Cea mai rapidă cale de a folosi aplicația pe calculator fără a configura mediul de dezvoltare:

1. **Descarcă fișierul `.apk`**:
   - În interfața Google AI Studio, folosește opțiunea **Export / Download APK**.
2. **Instalează un emulator Android pe Windows**:
   - Recomandat: **BlueStacks 5**, **LDPlayer 9** sau **Windows Subsystem for Android (WSA)**.
3. **Deschide APK-ul**:
   - Trage (Drag & Drop) fișierul `.apk` descărcat în fereastra emulatorului.
   - Aplicația se va instala automat și va rula exact ca pe telefon sau tableta Android!

---

## Opțiunea 2: Deschidere și Rulare din Android Studio pe Windows

Dacă dorești să rulezi proiectul din codul sursă:

1. **Descarcă Proiectul ZIP** sau fă **Push pe GitHub** din AI Studio.
2. Deschide **Android Studio** pe PC.
3. Selectează **File -> Open...** și alege folderul extras.
4. Lasă Gradle să sincronizeze dependențele.
5. Apasă butonul **Run** (`Shift + F10`) selectând un emulator Android integrat sau un telefon conectat prin cablu USB (cu USB Debugging activat).

---

## Opțiunea 3: Compilare pachet executabil pentru Windows

Dacă vrei să transformi codul UI (Jetpack Compose) în aplicație nativă Desktop:

1. Deschide proiectul în **IntelliJ IDEA** sau **Android Studio**.
2. Asigură-te că ai instalat **JDK 17** sau mai nou.
3. Deschide Terminalul integrat din IDE și rulează:
   ```bash
   ./gradlew packageDistributionForCurrentOS
   ```
   sau pentru installer MSI:
   ```bash
   ./gradlew packageMsi
   ```
4. Executabilul generat se va găsi în calea:
   `app/build/compose/binaries/main/msi/` sau `app/build/compose/binaries/main/app/`.

---

### Structura Proiectului
- `app/src/main/java/com/example/ui/` - Interfața Jetpack Compose (MainScreen, Components, ViewModel)
- `app/src/main/java/com/example/domain/` - Modele matematice (WinProbabilityModel, HoopsRepository, EspnService)
- `app/src/main/java/com/example/data/` - Retrofit & Moshi Network API Client (ESPN Scoreboard & Statisitici)
