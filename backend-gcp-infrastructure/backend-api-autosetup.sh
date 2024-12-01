# Script untuk melakukan proses deployment secara otomatis pada Google Cloud Platform

# Pastikan script mendapatkan akses root
if [ "$(whoami)" != "backend" ]; then
	echo "Script ini harus dijalankan oleh user backend"
	exit 1
fi

# Jika tidak ada /opt/ReNote, jangan lanjutkan script
if [ ! -d "/opt/ReNote" ]; then
	echo "Folder /opt/ReNote tidak ditemukan, setup tidak akan dilanjutkan"
	exit 1
fi

cd /opt/ReNote

# Install Node.js dan NPM menggunakan Node Version Manager (NVM)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"

nvm install 22

# Check if installation is successful
if [ $? -ne 0 ]; then
    echo "Gagal menginstall Node.js dan NPM, setup tidak akan dilanjutkan"
    exit 1
fi

node --version

# Check apakah node bisa dijalankan
if [ $? -ne 0 ]; then
	echo "Gagal spawn Node.js, setup tidak akan dilanjutkan"
	exit 1
fi

npm --version

# Check apakah npm bisa dijalankan
if [ $? -ne 0 ]; then
	echo "Gagal spawn NPM, setup tidak akan dilanjutkan"
	exit 1
fi

# Go to the backend folder
cd /opt/ReNote/backend/

npm install
npm run build

# Take note about the npm installation path
USER_PATH="$PATH"
NPM_LOCATION="$(whereis npm | cut -d ' ' -f 2)"

# Create a systemd service for renote-backend-api.service
cat << EOF | tee renote-backend-api.service
[Unit]
Description=ReNote Backend API
After=network.target

[Service]
Type=simple
Restart=on-failure
RestartSec=30

User=backend
Environment=PATH=${USER_PATH}
WorkingDirectory=/opt/ReNote/backend
ExecStart=${NPM_LOCATION} run start

[Install]
WantedBy=multi-user.target
EOF
