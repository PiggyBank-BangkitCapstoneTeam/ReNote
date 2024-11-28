## Buat Project Google Cloud yang terhubung dengan Firebase

- Buat project Google Cloud baru di https://console.cloud.google.com/
- Pilih selector project di pojok kiri atas, lalu klik "NEW PROJECT"
- Masukkan nama project
- Klik "Create"


- Buka Firebase Console di https://console.firebase.google.com/
- Klik "Add project" / "Get started with a Firebase project" / "Create a project"
- Klik "Add Firebase to Google Cloud project"
- Pilih project yang sudah dibuat sebelumnya
- Klik "Continue"
- Jika ada "Confirm Firebase billing plan", klik "Confirm plan"
- Jika ada "A few things to remember when adding Firebase to a Google Cloud project," klik "Continue"
- Untuk "Google Analytics for your Firebase project", matikan slider "Enable Google Analytics for this project", lalu klik "Add Firebase"
- Tunggu hingga proses penyiapan selesai
- Klik "Continue"
- Done

## Aktifkan Firebase Authentication
- Buka Firebase Console di https://console.firebase.google.com/
- Pilih project yang sudah dibuat sebelumnya
- Klik "Authentication" > "Get started"
- Enable semua metode autentikasi yang diinginkan (Email/Password, Google, Facebook, dll)
	> Saat diminta untuk memberikan "Support email", pilih email yang sudah terdaftar di Firebase, lalu klik "Save"

## Daftarkan Android App ke Firebase
- Klik "Add app" > "Android"
- Masukkan nama package ID aplikasi Android pada field "Android package name"
- Masukkan nama aplikasi pada field "App nickname"
- Buka Android Studio dan run "./gradlew signingReport" untuk mendapatkan SHA-1 key
	> Atau cek perintah yang sesuai di [sini](https://developers.google.com/android/guides/client-auth)
- Masukkan SHA-1 key yang didapatkan pada field "Debug signing certificate SHA-1"
- Klik "Register app"
- Download file "google-services.json" dan letakkan pada folder "app" di project Android
- Klik "Next"
- Tambahkan Firebase SDK ke project Android (ikuti instruksi yang ada)
- Klik "Next" saat sudah selesai menambahkan SDK
- Klik "Continue to console"
- Done

# Daftarkan Backend ke Firebase
- Pastikan file .env sudah ada di backend dan menggunakan template dari file .env.example
	> Jika file .env belum ada, copy file .env.example menjadi .env
- Klik "Add app" > "Web"
- Masukkan nama aplikasi pada field "App nickname"
- **PASTIKAN Jangan centang "Also set up Firebase Hosting for this app"**
- Klik "Register app"
- Pilih "use npm"
- Ambil informasi firebaseConfig yang ada, lalu sesuaikan kedalam file .env
- Klik "Continue to console"
- Done