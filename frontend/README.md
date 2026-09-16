# Bike Tracker – Static Web Frontend

Nowoczesny, responsywny i w 100% darmowy frontend do prezentacji tras rowerowych z aplikacji mobilnej na interaktywnej mapie z możliwością pobierania plików GPX.

---

## 🚀 Jak uruchomić lokalnie

W katalogu `frontend/`:

```bash
# Opcja 1: Python
python -m http.server 8080

# Opcja 2: Node.js npx serve
npx serve .
```

Otwórz w przeglądarce: `http://localhost:8080`

---

## 🌐 Wdrożenie na GitHub Pages (100% Darmowe)

### Krok 1: Wypchnięcie kodu na GitHub
Jeśli projekt znajduje się w repozytorium git:
```bash
git add .
git commit -m "Add static frontend for bike tracker"
git push origin main
```

### Krok 2: Włączenie GitHub Pages
1. Przejdź do swojego repozytorium na **GitHub.com**.
2. Kliknij **Settings** ➔ **Pages** (w lewym menu).
3. W sekcji **Build and deployment**:
   - **Source**: `Deploy from a branch`
   - **Branch**: `main`
   - **Folder**: `/frontend` (lub `/docs`, jeśli przeniesiesz pliki)
4. Kliknij **Save**.

Po 1–2 minutach Twoja strona będzie dostępna pod adresem:
`https://<twój-username>.github.io/<nazwa-repo>/`

---

## 🔒 Bezpieczeństwo kluczy Firebase

Plik `js/firebase-config.js` zawiera publiczne identyfikatory projektu Firebase. Jest to oficjalny i bezpieczny standard dla aplikacji frontendowych SPA. 

Bezpieczeństwo danych opiera się na regułach **Firestore Security Rules** wdrożonych w pliku `firestore.rules`:
- **Odczyt:** publiczny dla każdego użytkownika odwiedzającego stronę.
- **Zapis / Modyfikacja:** zablokowane dla osób nieautoryzowanych, możliwe tylko z poziomu zalogowanej aplikacji mobilnej.
