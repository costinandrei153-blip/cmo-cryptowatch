# CryptoWatch

Aplicație Android pentru urmărirea criptomonedelor: prețuri actuale, detalii despre fiecare monedă și o listă personală de favorite salvată pe dispozitiv.

Proiect de semestru la disciplina Calcul mobil. Temă aleasă: **Investments**.

> Aplicația are scop educativ și nu oferă sfaturi de investiții.

## Echipa

| Membru | Rol principal |
|---|---|
| Costin Andrei | Dezvoltare Android (toate componentele) |

## Obiective

- Afișarea unei liste cu primele criptomonede după capitalizare, cu preț și variație pe 24 de ore.
- Un ecran de detaliu pentru fiecare monedă, cu informații complete și evoluția prețului.
- Salvarea local a monedelor favorite și a ultimelor prețuri aduse (cache), care se păstrează după repornirea aplicației.
- Preluarea datelor dintr-un API extern.

## Funcționalități

Planificate pentru cerințele minimale:

- [ ] Ecran listă cu monede
- [ ] Ecran detaliu la selectarea unei monede
- [ ] Favorite salvate local (Room)
- [ ] Cache local pentru prețuri (Room)
- [ ] Date din API extern (Retrofit)

Propuse pentru nivelul superior (de confirmat):

- [ ] Grafic cu evoluția prețului
- [ ] Notificare când o monedă depășește un prag ales de utilizator

## Tehnologii

- Kotlin și Jetpack Compose
- Room pentru stocare locală
- Retrofit pentru apelurile REST
- API extern: [CoinGecko API](https://www.coingecko.com/en/api)

## Instrucțiuni de rulare

1. Clonează repository-ul și deschide-l în Android Studio.
2. Creează un cont gratuit CoinGecko și generează o cheie API de tip Demo.
3. În fișierul `local.properties` din rădăcina proiectului (ignorat de Git), adaugă linia:
   ```
   COINGECKO_API_KEY=cheia_ta
   ```
4. Rulează aplicația pe un emulator sau pe un dispozitiv fizic.

Cheia API nu se include niciodată în repository.

## Organizarea muncii

Sarcinile sunt gestionate în GitHub Projects, cu responsabil și stare (de făcut / în lucru
