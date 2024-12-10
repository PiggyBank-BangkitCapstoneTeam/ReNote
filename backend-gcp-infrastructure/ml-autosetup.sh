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

python3.10 --version

# Check apakah python bisa dijalankan
if [ $? -ne 0 ]; then
	echo "Gagal spawn Python, setup tidak akan dilanjutkan"
	exit 1
fi

cd /opt/ReNote/machine-learning/

# Create python3.10 virtual environment
python3.10 -m venv .venv

# Masuk ke virtual environment
source .venv/bin/activate

pip --version

# Check apakah pip bisa dijalankan
if [ $? -ne 0 ]; then
	echo "Gagal spawn PIP, setup tidak akan dilanjutkan"
	exit 1
fi

# Install semua yang tim ML butuhkan
pip install -r requirements.txt

# Take note about the npm installation path
USER_PATH="$PATH"

# Create a systemd service for renote-ml.service
cat << EOF | tee renote-ml.service
[Unit]
Description=ReNote Machine Learning Service
After=network.target

[Service]
Type=simple
Restart=on-failure
RestartSec=30

User=backend
Environment=PATH=${USER_PATH}
WorkingDirectory=/opt/ReNote/machine-learning/Inference_Model
ExecStart=/opt/ReNote/machine-learning/.venv/bin/python backend-connector.py

[Install]
WantedBy=multi-user.target
EOF