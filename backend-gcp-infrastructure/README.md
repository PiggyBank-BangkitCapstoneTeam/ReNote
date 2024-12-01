# ReNote Backend GCP Infrastructure


## Tutorial Auto Setup ReNote Backend API di Google Cloud Platform

1. Akses Google Cloud Console (web) dan buat proyek baru
2. Nyalakan Cloud Shell
3. Upload file `backend-gcp-infrastructure/setup-cloud-infrastructure.sh` ke Cloud Shell

	atau download file tersebut langsung dari Repository:<br>
	`wget -O "setup-cloud-infrastructure.sh" https://raw.githubusercontent.com/PiggyBank-BangkitCapstoneTeam/ReNote/refs/heads/cc-backend/backend-gcp-infrastructure/setup-cloud-infrastructure.sh`

4. Kasih izin untuk run setup script nya<br>
	`chmod 744 setup-cloud-infrastructure.sh`

5. Run file setup:<br>
	`./setup-cloud-infrastructure.sh`<br>

	Jika terkadang ditanya apakah ingin mengaktifkan API, ketik `y` dan tekan `Enter`.<br>
	**NOTE:** Sebenarnya script sudah menghandle aktivasi API yang dibutuhkan, tetapi terkadang masih ada aja konfirmasi.

6. Hapus file setup `setup-cloud-infrastructure.sh`:<br>
	`rm setup-cloud-infrastructure.sh`

7. Hapus folder ``renote-infrastructure-setup-cache``:<br>
	`rm -r renote-infrastructure-setup-cache`

8. Di Google Cloud Console, buka Compute Engine dan salin External IP instance backend-api ke Postman Variable `HOST` di Collection ``ReNote API Endpoints``.
   
9. Lakukan testing API untuk memastikan backend API sudah berjalan dengan baik.

<br>

----

## Cara melihat auto setup log Backend API
1. Masuk ke VM instance yang digunakan untuk backend API
2. Jalankan perintah berikut untuk melihat log setup realtime (follow mode):<br>
	`sudo journalctl -f -u google-startup-scripts.service`

	atau jalankan perintah ini jika ingin melihat log saja:<br>
	`sudo journalctl -u google-startup-scripts.service`

<br>

----

## Cara melihat log Backend API
1. Masuk ke VM instance yang digunakan untuk backend API
2. Jalankan perintah berikut untuk melihat log realtime (follow mode):<br>
	`sudo journalctl -f -u renote-backend-api.service`

	atau jalankan perintah ini jika ingin melihat log saja:<br>
	`sudo journalctl -u renote-backend-api.service`