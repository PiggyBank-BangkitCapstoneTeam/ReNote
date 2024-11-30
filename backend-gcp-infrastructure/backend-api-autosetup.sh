# Script untuk melakukan proses deployment secara otomatis pada Google Cloud Platform

# Pastikan script mendapatkan akses root
if [ "$(whoami)" != "root" ]; then
	echo "Script ini membutuhkan akses root"
	exit 1
fi

# Jika tidak ada /opt/ReNote, jangan lanjutkan script
if [ ! -d "/opt/ReNote" ]; then
	echo "Folder /opt/ReNote tidak ditemukan, setup tidak akan dilanjutkan"
	exit 1
fi

cd /opt/ReNote

sudo apt update
sudo apt upgrade -y

# Setup backend user without no password (system)
sudo useradd --system --create-home --shell /bin/bash backend
sudo usermod -aG sudo backend

# Ganti ownership folder /opt/ReNote ke user backend
sudo chown -R backend:backend /opt/ReNote

# Atur permission semua folder di /opt/ReNote ke 755
find /opt/ReNote -type d -print0 | sudo xargs -0 chmod 755

# Atur permission semua file di /opt/ReNote ke 644
find /opt/ReNote -type f -print0 | sudo xargs -0 chmod 644

# Switch to the backend user
sudo su - backend

# Install Node.js dan NPM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.bashrc
nvm install 22

node --version
npm --version

# Go to the backend folder
cd /opt/ReNote/backend/

npm install
npm run build

# Take note about the npm installation path
USER_PATH="$PATH"
NPM_LOCATION="$(whereis npm | cut -d' ' -f 2)"

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

# Exit out of the backend user
exit

# Make sure we're on the backend folder still
cd /opt/ReNote/backend

# Move the systemd service to /etc/systemd/system/
sudo mv renote-backend-api.service /etc/systemd/system/

# Reload the systemd daemon
sudo systemctl daemon-reload

# Enable and start the renote-backend-api service
sudo systemctl enable renote-backend-api.service
sudo systemctl start renote-backend-api.service