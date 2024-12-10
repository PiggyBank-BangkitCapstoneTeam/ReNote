# Script to setup the cloud infrastructure for ReNote on GCP

#region Variabel untuk nilai nilai default
GCP_PROJECT_ID="$(gcloud config get-value project)"
DEFAULT_SQL_ROOT_PASSWORD="" # biarkan kosong jika ingin generate password random
DEFAULT_SQL_BACKEND_API_PASSWORD="" # biarkan kosong jika ingin generate password random

# Untuk memastikan nama bucket unik, kita menggunakan Project ID sebagai postfix
DEFAULT_CLOUD_STORAGE_BUCKET_NAME="renote-usermedia-$GCP_PROJECT_ID"

DEFAULT_REGION="asia-southeast2"
DEFAULT_ZONE="$DEFAULT_REGION-b"

# SSH Public Key untuk akses VM dari luar GCP
# FORMAT: username:ssh-................................................................
EXTRA_SSH_PUBLIC_KEY="""
c115b4ky2432:ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAINnhmh+QEajnW8qtPHsY4E3HXbPT+Q+4a9HnzZVDvc+N 
"""
#endregion

#region Pre-init preparations

# Matikan output gcloud agar lebih rapi
export CLOUDSDK_CORE_DISABLE_PROMPTS=1

# Function untuk membuat warna output script menjadi lebih menarik
setup_echo() {
	# $1 harus antara "normal", "error", "warning", "info"
	case "$1" in
		"normal")
			echo -en "\033[0;32m"
		;;

		"error")
			echo -en "\033[1;31m"
		;;

		"warning")
			echo -en "\033[0;33m"
		;;

		"info")
			echo -en "\033[0;34m"
		;;

		*)
			echo "Input invalid!"
			exit 0
		;;
	esac
		

	echo -n "$2"
    echo -e "\033[0m"
}

# Check if we have renote-infrastructure-setup-cache/
if [ -d "renote-infrastructure-setup-cache" ]; then
	echo -n "Renote cache setup ditemukan dan perlu dihapus, apakah anda ingin menghapusnya? (y/n) "

	read delete_cache
	if [ "$delete_cache" == "y" ]; then
		rm -rf "renote-infrastructure-setup-cache"
		setup_echo "info" "Renote cache setup sebelumnya dihapus!"
	elif [ "$delete_cache" == "n" ]; then
		setup_echo "error" "Tidak dapat melanjutkan setup tanpa menghapus cache setup!"
		exit 0
	else
		setup_echo "error" "Input tidak valid!"
		exit 0
	fi
fi

setup_echo "normal" "Membuat cache untuk sesi ini..."
mkdir renote-infrastructure-setup-cache/
cd renote-infrastructure-setup-cache/

# Pastikan ada gcloud
setup_echo "normal" "Memeriksa gcloud..."
gcloud version > /dev/null
if [ $? -ne 0 ]; then
	setup_echo "error" "gcloud tidak ditemukan, harap install gcloud terlebih dahulu lalu run script lagi"
	setup_echo "info" "Tips: https://cloud.google.com/sdk/docs/install"
	exit 1
fi

# Pastikan ada openssl
setup_echo "normal" "Memeriksa openssl..."
openssl version > /dev/null
if [ $? -ne 0 ]; then
	setup_echo "error" "openssl tidak ditemukan, harap install openssl terlebih dahulu lalu run script lagi"
	setup_echo "info" "Tips: sudo apt update && sudo apt install openssl -y"
	exit 1
fi

# Generate password untuk MySQL root user jika tidak diisi
if [ -z "$DEFAULT_SQL_ROOT_PASSWORD" ]; then
	setup_echo "info" "Password MySQL root user tidak diisi, generate password random..."
	DEFAULT_SQL_ROOT_PASSWORD="$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-24)"
fi

# Pastikan DEFAULT_SQL_ROOT_PASSWORD sudah diisi
if [ -z "$DEFAULT_SQL_ROOT_PASSWORD" ]; then
	setup_echo "error" "Gagal generate password MySQL root user, exiting..."
	exit 1
fi

# Generate password untuk MySQL backend user jika tidak diisi
if [ -z "$DEFAULT_SQL_BACKEND_API_PASSWORD" ]; then
	setup_echo "info" "Password MySQL backend user tidak diisi, generate password random..."
	DEFAULT_SQL_BACKEND_API_PASSWORD="$(openssl rand -base64 24 | tr -d "=+/" | cut -c1-24)"
fi

# Pastikan DEFAULT_SQL_BACKEND_API_PASSWORD sudah diisi
if [ -z "$DEFAULT_SQL_BACKEND_API_PASSWORD" ]; then
	setup_echo "error" "Gagal generate password MySQL backend user, exiting..."
	exit 1
fi

# Test otentikasi Cloud Shell
setup_echo "normal" "Mencoba otentikasi Cloud Shell..."
gcloud auth print-access-token > /dev/null
if [ $? -ne 0 ]; then
	setup_echo "error" "Gagal melakukan otentikasi gcloud, run script lagi jika ingin mencoba lagi"
	exit 1
fi

#endregion

#region Service Account for Backend API Service

# Create a service account for the Backend API Compute Engine
setup_echo "normal" "Membuat service account untuk Backend Compute Engine..."
gcloud iam service-accounts create "backend-service-account" \
	--description="Service Account for Backend API Compute Engine" \
	--display-name="Backend Service Account"

SERVICE_ACCOUNT_EMAIL="backend-service-account@$GCP_PROJECT_ID.iam.gserviceaccount.com"

# Terkadang ada error service-account masih belum terdaftar, jadi tunggu dulu sejenak
sleep 30

# Create a JSON key to be used to authenticate the service account
setup_echo "normal" "Membuat JSON key untuk service account \"$SERVICE_ACCOUNT_EMAIL\"..."
gcloud iam service-accounts keys create "backend-sak.json" \
	--iam-account="$SERVICE_ACCOUNT_EMAIL" \
	--key-file-type="json"

# Grant the service account the "Cloud SQL Client" role
setup_echo "normal" "Memberikan role kepada service account..."
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
	--role="roles/cloudsql.client"

# Grant the service account the "Compute Engine Admin" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
	--role="roles/compute.admin"

# Grant the service account the "Storage Object User" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
	--role="roles/storage.objectUser"

# Grant the service account the "Firebase Authentication Viewer" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
	--role="roles/firebaseauth.viewer"

# Grant the service account the "Pub/Sub Publisher" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
    --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
    --role="roles/pubsub.publisher"

# Grant the service account the "Pub/Sub Subscriber" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
    --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
    --role="roles/pubsub.subscriber"

echo

#endregion

#region Service Account for Machine Learning Service

# Create a service account for the Backend API Compute Engine
setup_echo "normal" "Membuat service account untuk Machine Learning Compute Engine..."
gcloud iam service-accounts create "maclea-service-account" \
	--description="Service Account for Machine Learning Compute Engine" \
	--display-name="Machine Learning Service Account"

ML_SERVICE_ACCOUNT_EMAIL="maclea-service-account@$GCP_PROJECT_ID.iam.gserviceaccount.com"

# Terkadang ada error service-account masih belum terdaftar, jadi tunggu dulu sejenak
sleep 30

# Create a JSON key to be used to authenticate the service account
setup_echo "normal" "Membuat JSON key untuk service account \"$ML_SERVICE_ACCOUNT_EMAIL\"..."
gcloud iam service-accounts keys create "maclea-sak.json" \
	--iam-account="$ML_SERVICE_ACCOUNT_EMAIL" \
	--key-file-type="json"

# Grant the service account the "Compute Engine Admin" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$ML_SERVICE_ACCOUNT_EMAIL" \
	--role="roles/compute.admin"

# Grant the service account the "Storage Object Viewer" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$ML_SERVICE_ACCOUNT_EMAIL" \
	--role="roles/storage.objectViewer"

# Grant the service account the "Pub/Sub Publisher" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
    --member="serviceAccount:$ML_SERVICE_ACCOUNT_EMAIL" \
    --role="roles/pubsub.publisher"

# Grant the service account the "Pub/Sub Subscriber" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$ML_SERVICE_ACCOUNT_EMAIL" \
	--role="roles/pubsub.subscriber"

echo
#endregion

#region VPC Network

# Enable Compute Engine API
setup_echo "normal" "Mengaktifkan Compute Engine API (dapat memakan waktu beberapa menit)..."
gcloud services enable compute.googleapis.com
sleep 30

# Create VPC Network
setup_echo "normal" "Membuat VPC Network..."
gcloud compute networks create renote-network \
	--description="Google Cloud VPC Network for PiggyBank's ReNote project" \
	--subnet-mode="custom" \
	--mtu="1500" \
	--bgp-routing-mode="regional"

# Create subnet for internal services
setup_echo "normal" "Membuat subnet untuk internal services..."
gcloud compute networks subnets create renote-network-internal \
	--description="PiggyBank's ReNote subnet for connection between internal services (private routing)" \
	--range="192.168.0.0/28" \
	--stack-type="IPV4_ONLY" \
	--network="renote-network" \
	--region="$DEFAULT_REGION" \
	--enable-private-ip-google-access

# Setup default firewall rules
setup_echo "normal" "Menyiapkan default firewall rules..."
gcloud compute firewall-rules create renote-allow-internal \
	--direction="INGRESS" \
	--priority="65534" \
	--network="renote-network" \
	--action="ALLOW" \
	--rules="all" \
	--source-ranges="192.168.0.0/28"

gcloud compute firewall-rules create renote-allow-icmp \
	--direction="INGRESS" \
	--priority="65534" \
	--network="renote-network" \
	--action="ALLOW" \
	--rules="icmp" \
	--source-ranges="0.0.0.0/0"

gcloud compute firewall-rules create renote-allow-ssh-browser \
	--direction="INGRESS" \
	--priority="65534" \
	--network="renote-network" \
	--action="ALLOW" \
	--rules="tcp:22" \
	--source-ranges="35.235.240.0/20"

gcloud compute firewall-rules create renote-allow-ssh-public \
	--direction="INGRESS" \
	--priority="65534" \
	--network="renote-network" \
	--action="ALLOW" \
	--rules="tcp:22" \
	--source-ranges="0.0.0.0/0" \
	--target-tags="backend-api-server"

gcloud compute firewall-rules create renote-allow-api-access \
	--direction="INGRESS" \
	--priority="65533" \
	--network="renote-network" \
	--action="ALLOW" \
	--rules="tcp:3000" \
	--source-ranges="0.0.0.0/0" \
	--target-tags="backend-api-server"

# Reserve internal static IP for the Backend API Compute Engine
setup_echo "normal" "Melakukan konfigurasi IP Static untuk compute engine Backend API"
gcloud compute addresses create backend-api \
	--description="Reserved Internal IP for Backend API Compute Engine" \
	--addresses="192.168.0.2" \
	--region="$DEFAULT_REGION" \
	--subnet="projects/$GCP_PROJECT_ID/regions/$DEFAULT_REGION/subnetworks/renote-network-internal" \
	--purpose="GCE_ENDPOINT"

# Reserve internal static IP for the Machine Learning Compute Engine
setup_echo "normal" "Melakukan konfigurasi IP Static untuk compute engine Machine Learning Service"
gcloud compute addresses create maclea-api \
	--description="Reserved Internal IP for Machine Learning Compute Engine" \
	--addresses="192.168.0.3" \
	--region="$DEFAULT_REGION" \
	--subnet="projects/$GCP_PROJECT_ID/regions/$DEFAULT_REGION/subnetworks/renote-network-internal" \
	--purpose="GCE_ENDPOINT"

# Enable Service Networking API
setup_echo "normal" "Mengaktifkan Service Networking API..."
gcloud services enable servicenetworking.googleapis.com
sleep 30

# Create a VPC Peering allocation for Managed Services (Redis, Cloud SQL, etc.) connection
setup_echo "normal" "Membuat VPC Peering untuk Managed Services..."
gcloud compute addresses create managed-services-peering \
	--global \
	--purpose="VPC_PEERING" \
	--addresses="192.168.16.0" \
	--prefix-length="20" \
	--description="Reserved Internal IP for Managed Services Peering" \
	--network="renote-network"

# Create a private connection for the Managed Services
setup_echo "normal" "Menghubungkan VPC Network dengan Managed Services (dapat memakan waktu beberapa menit)..."
gcloud services vpc-peerings connect \
	--service="servicenetworking.googleapis.com" \
	--ranges="managed-services-peering" \
	--network="renote-network"

echo
#endregion

#region Cloud SQL

# Enable SQL Admin API
setup_echo "normal" "Mengaktifkan SQL Admin API..."
gcloud services enable sqladmin.googleapis.com
sleep 30

# Create a CloudSQL (MySQL) instance
setup_echo "normal" "Membuat Cloud SQL (MySQL) instance (dapat memakan waktu beberapa menit)..."
gcloud sql instances create renote-mysql \
	--database-version="MYSQL_8_4" \
	--tier="db-g1-small" \
	--region="$DEFAULT_REGION" \
	--network="renote-network" \
	--no-assign-ip \
	--root-password="$DEFAULT_SQL_ROOT_PASSWORD" \
	--edition="enterprise"

# Create a database
setup_echo "normal" "Membuat database ReNote..."
gcloud sql databases create renote --instance="renote-mysql"

# Create a user for the Backend API Compute Engine
setup_echo "normal" "Membuat user MySQL untuk Backend API Compute Engine..."
gcloud sql users create "backend-api" \
	--instance="renote-mysql" \
	--host="%" \
	--password="$DEFAULT_SQL_BACKEND_API_PASSWORD" \
	--type="BUILT_IN"

sleep 30
#endregion

#region Cloud Storage

# Create a Cloud Storage bucket for user media
gcloud storage buckets create "gs://$DEFAULT_CLOUD_STORAGE_BUCKET_NAME" \
	--location="$DEFAULT_REGION" \
	--uniform-bucket-level-access \
	--no-public-access-prevention \
	--soft-delete-duration="0"

# Allow everyone to read any object in the user media bucket
gcloud storage buckets add-iam-policy-binding "gs://$DEFAULT_CLOUD_STORAGE_BUCKET_NAME" \
	--member="allUsers" \
	--role="roles/storage.objectViewer"
	
#endregion

#region Memorystore (Redis)
# Create Memorystore (Redis) instance buat caching data backend
setup_echo "normal" "Mengaktifkan Memorystore (Redis) API... (dapat memakan waktu beberapa menit)"
gcloud services enable redis.googleapis.com
sleep 30

gcloud redis instances create renote-redis1 \
	--tier="basic" \
	--size="1" \
	--region="$DEFAULT_REGION" \
	--zone="$DEFAULT_ZONE" \
	--redis-version="redis_7_0" \
	--network="projects/$GCP_PROJECT_ID/global/networks/renote-network" \
	--connect-mode="PRIVATE_SERVICE_ACCESS" \
	--reserved-ip-range="managed-services-peering" \
	--display-name="Renote Redis 1"
#endregion

#region Pub/Sub

# Enable Pub/Sub API
setup_echo "normal" "Mengaktifkan Pub/Sub API..."
gcloud services enable pubsub.googleapis.com
sleep 30

# Create a Pub/Sub topic for the Machine Learning Service
setup_echo "normal" "Membuat Pub/Sub topic untuk Machine Learning Service..."
gcloud pubsub topics create "renote-ml-request-topic" \
	--message-retention-duration="10m"
	
gcloud pubsub topics create "renote-ml-response-topic" \
	--message-retention-duration="10m"

# Create a Pub/Sub subscription for the Machine Learning Service
setup_echo "normal" "Membuat Pub/Sub subscription untuk Machine Learning Service..."
gcloud pubsub subscriptions create "renote-ml-request-subscription" \
	--topic="renote-ml-request-topic" \
	--ack-deadline="10" \
	--enable-exactly-once-delivery \
	--expiration-period="14d" \
	--message-retention-duration="10m"

gcloud pubsub subscriptions create "renote-ml-response-subscription" \
	--topic="renote-ml-response-topic" \
	--ack-deadline="10" \
	--enable-exactly-once-delivery \
	--expiration-period="14d" \
	--message-retention-duration="10m"

#region Compute Engine for Backend API

setup_echo "normal" "Membuat file startup script untuk compute engine Backend API..."
echo \#\!"/bin/bash" > backend-api-compute-engine-startup.sh

cat << "EOF" >> backend-api-compute-engine-startup.sh

# Jika sudah ada /opt/ReNote, jangan lanjutkan script
if [ -d "/opt/ReNote" ]; then
	echo "Folder /opt/ReNote sudah ada, setup tidak akan dilanjutkan"
	exit 0
fi

sudo apt update
sudo apt upgrade -y
sudo apt install curl bash git -y

# Cek instance name untuk compute engine ini
INSTANCE_NAME=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/name" -H "Metadata-Flavor: Google")
INSTANCE_ZONE=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor: Google")

# Setup backend user without password (system)
sudo useradd --system --create-home --shell /bin/bash backend
sudo usermod -aG sudo backend

sudo mkdir -p /opt/
cd /opt/
sudo git clone https://github.com/PiggyBank-BangkitCapstoneTeam/ReNote
cd ReNote/
sudo git checkout cc-backend

# Ganti ownership folder /opt/ReNote ke user backend
sudo chown -R backend:backend /opt/ReNote

# Atur permission semua folder di /opt/ReNote ke 755
find /opt/ReNote -type d -print0 | sudo xargs -0 chmod 755

# Atur permission semua file di /opt/ReNote ke 644
find /opt/ReNote -type f -print0 | sudo xargs -0 chmod 644

cd backend-gcp-infrastructure/

sudo chmod 744 backend-api-autosetup.sh
sudo su -c "bash /opt/ReNote/backend-gcp-infrastructure/backend-api-autosetup.sh" backend

# Check the output of the script
if [ $? -ne 0 ]; then
	echo "backend-api-autosetup.sh failed, exiting..."
	exit 1
fi

# Check if /opt/ReNote/Backend/renote-backend-api.service exists
if [ ! -f "/opt/ReNote/backend/renote-backend-api.service" ]; then
	echo "backend-api-autosetup.sh failed to create renote-backend-api.service, exiting..."
	exit 1
fi

cd /opt/ReNote/backend

# Move the systemd service to /etc/systemd/system/
sudo mv renote-backend-api.service /etc/systemd/system/

# Reload the systemd daemon
sudo systemctl daemon-reload

# Enable and start the renote-backend-api service
sudo systemctl enable renote-backend-api.service
sudo systemctl start renote-backend-api.service

# Add the compute engine to the backend-api-server tag for firewall rules
gcloud compute instances add-tags "$INSTANCE_NAME" --tags="backend-api-server" --zone="$INSTANCE_ZONE"

# Wait until /tmp/backend-api.env is uploaded
ELAPSED_WAITING_FOR_ENV=0
while [ ! -f "/tmp/backend-api.env" ]; do
	sleep 10
	ELAPSED_WAITING_FOR_ENV=$((ELAPSED_WAITING_FOR_ENV+10))

	if [ $ELAPSED_WAITING_FOR_ENV -gt 600 ]; then
		echo "Timeout waiting for /tmp/backend-api.env, exiting..."
		exit 1
	fi

	echo "Waiting for /tmp/backend-api.env to be uploaded... ($ELAPSED_WAITING_FOR_ENV detik / 600 detik)"
done

# Move the .env file to /opt/ReNote/backend/
sudo mv /tmp/backend-api.env /opt/ReNote/backend/.env

# Make sure the env is owned by backend user
sudo chown backend:backend /opt/ReNote/backend/.env

# Wait until /tmp/backend-sak.json is uploaded
ELAPSED_WAITING_FOR_SAK=0
while [ ! -f "/tmp/backend-sak.json" ]; do
	sleep 10
	ELAPSED_WAITING_FOR_SAK=$((ELAPSED_WAITING_FOR_SAK+10))

	if [ $ELAPSED_WAITING_FOR_SAK -gt 600 ]; then
		echo "Timeout waiting for /tmp/backend-sak.json, exiting..."
		exit 1
	fi

	echo "Waiting for /tmp/backend-sak.json to be uploaded... ($ELAPSED_WAITING_FOR_SAK detik / 600 detik)"
done

# Move the service account key to /opt/ReNote/backend/
sudo mv /tmp/backend-sak.json /opt/ReNote/backend/service-account.json

# symlink the service account key to /opt/ReNote/backend/firebase-service-account.json
sudo ln -s /opt/ReNote/backend/service-account.json /opt/ReNote/backend/firebase-service-account.json

# Make sure the service account key is owned by backend user
sudo chown backend:backend /opt/ReNote/backend/service-account.json
sudo chown backend:backend /opt/ReNote/backend/firebase-service-account.json

# Restart the renote-backend-api service
sudo systemctl restart renote-backend-api.service

EOF

# Buat file .env untuk backend-api
setup_echo "normal" "Membuat file .env untuk backend-api..."

cat << EOF > backend-api.env
ENVIRONMENT=development

FIREBASE_apiKey="AIza..."
FIREBASE_authDomain="....firebaseapp.com"
FIREBASE_projectId="..."
FIREBASE_storageBucket="....firebasestorage.app"
FIREBASE_messagingSenderId="..."
FIREBASE_appId=".:...:web:..."
FIREBASE_APPLICATION_CREDENTIALS="./firebase-service-account.json"

GOOGLE_APPLICATION_CREDENTIALS="./service-account.json"

CloudSQL_Enabled="true"
CloudSQL_ConnectionName="$(gcloud sql instances describe renote-mysql --format="value(connectionName)")"
CloudSQL_IpAddressType="PRIVATE"
CloudSQL_Username="backend-api"
CloudSQL_Password="$DEFAULT_SQL_BACKEND_API_PASSWORD"
CloudSQL_Database="renote"

CloudStorage_Enabled="true"
CloudStorage_UserMediaBucket="$DEFAULT_CLOUD_STORAGE_BUCKET_NAME"

MemoryStoreRedis_Enabled="true"
MemoryStoreRedis_HostName="$(gcloud redis instances describe renote-redis1 --region="$DEFAULT_REGION" --format="value(host)")"
MemoryStoreRedis_Port="6379"

EOF

# Dump SSH keys so it can be on metadata
setup_echo "normal" "Membuat file yang berisi semua SSH Public Key tambahan..."

echo "$EXTRA_SSH_PUBLIC_KEY" > additional-ssh-keys.txt

# Create a Compute Engine for the Backend API
setup_echo "normal" "Membuat compute engine untuk Backend API (dapat memakan waktu beberapa menit)..."
gcloud compute instances create backend-api \
	--zone="$DEFAULT_ZONE" \
	--machine-type="e2-small" \
	--network-interface="network-tier=PREMIUM,private-network-ip=192.168.0.2,stack-type=IPV4_ONLY,subnet=renote-network-internal" \
	--maintenance-policy="MIGRATE" \
	--provisioning-model="STANDARD" \
	--service-account="backend-service-account@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
	--scopes="compute-rw,storage-rw,service-control,service-management,sql-admin" \
	--create-disk="auto-delete=yes,boot=yes,device-name=instance-20241130-102327,image=projects/ubuntu-os-cloud/global/images/ubuntu-2404-noble-amd64-v20241115,mode=rw,size=10,type=pd-balanced" \
	--no-shielded-secure-boot \
	--shielded-vtpm \
	--shielded-integrity-monitoring \
	--labels="goog-ec-src=vm_add-gcloud" \
	--reservation-affinity="any" \
	--metadata-from-file="startup-script=backend-api-compute-engine-startup.sh,ssh-keys=additional-ssh-keys.txt"

# Tunggu hingga compute engine selesai dibuat dan ada tag "backend-api-server"
setup_echo "normal" "Menunggu hingga setup script selesai..."
BACKEND_API_WAITING_ELAPSED=0
while true; do
	INSTANCE_TAGS=$(gcloud compute instances describe backend-api --zone="$DEFAULT_ZONE" --format="value(tags.items)")
	
	if [[ $INSTANCE_TAGS == *"backend-api-server"* ]]; then
		break
	fi

	if [ $BACKEND_API_WAITING_ELAPSED -ge 600 ]; then
		setup_echo "error" "Setup script untuk compute engine Backend API memakan waktu terlalu lama, exiting..."
		exit 1
	fi

	setup_echo "info" "Menunggu hingga setup script selesai... ($BACKEND_API_WAITING_ELAPSED detik / 600 detik)"

	sleep 10
	BACKEND_API_WAITING_ELAPSED=$((BACKEND_API_WAITING_ELAPSED+10))
done

# Copy the .env file to the Backend API Compute Engine
setup_echo "normal" "Mengirim file .env ke compute engine Backend API..."
gcloud compute scp backend-api.env backend-api:/tmp/backend-api.env --zone="$DEFAULT_ZONE"

# Copy the service account key to the Backend API Compute Engine
setup_echo "normal" "Mengirim service account key ke compute engine Backend API..."
gcloud compute scp backend-sak.json backend-api:/tmp/backend-sak.json --zone="$DEFAULT_ZONE"

#endregion

#region Compute Engine for Machine Learning Service

setup_echo "normal" "Membuat file startup script untuk compute engine Machine Learning..."
echo \#\!"/bin/bash" > maclea-compute-engine-startup.sh

cat << "EOF" >> maclea-compute-engine-startup.sh

# Jika sudah ada /opt/ReNote, jangan lanjutkan script
if [ -d "/opt/ReNote" ]; then
	echo "Folder /opt/ReNote sudah ada, setup tidak akan dilanjutkan"
	exit 0
fi

sudo apt update
sudo apt upgrade -y
sudo add-apt-repository --yes ppa:deadsnakes/ppa
sudo apt update
sudo apt install -y curl bash git unzip python3.10 python3.10-venv python3.10-dev python3.10-distutils ffmpeg libsm6 libxext6

# Cek instance name untuk compute engine ini
INSTANCE_NAME=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/name" -H "Metadata-Flavor: Google")
INSTANCE_ZONE=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/zone" -H "Metadata-Flavor: Google")

# Setup backend user without password (system)
sudo useradd --system --create-home --shell /bin/bash backend
sudo usermod -aG sudo backend

sudo mkdir -p /opt/
cd /opt/
sudo git clone https://github.com/PiggyBank-BangkitCapstoneTeam/ReNote
cd ReNote/

# Switch ke branch cc-backend
sudo git checkout ml-master

# Siapkan folder model YOLO
cd machine-learning/Inference_Model/trained_models/
sudo mkdir -p YOLO/
cd YOLO/

# Download file model_train_renfred_1.zip
sudo wget "https://github.com/PiggyBank-BangkitCapstoneTeam/ReNote/releases/download/v0.1.0-alpha/model_train_renfred_1.zip"

# Unzip file model_train_renfred_1.zip
sudo unzip "model_train_renfred_1.zip"

# Hapus file zip, karena sudah tidak dibutuhkan lagi
sudo rm "model_train_renfred_1.zip"

# Kembali ke root folder
cd /opt/ReNote/

# Ganti ownership folder /opt/ReNote ke user backend
sudo chown -R backend:backend /opt/ReNote

# Atur permission semua folder di /opt/ReNote ke 755
find /opt/ReNote -type d -print0 | sudo xargs -0 chmod 755

# Atur permission semua file di /opt/ReNote ke 644
find /opt/ReNote -type f -print0 | sudo xargs -0 chmod 644

cd backend-gcp-infrastructure/
sudo chmod 744 ml-autosetup.sh
sudo su -c "bash /opt/ReNote/backend-gcp-infrastructure/ml-autosetup.sh" backend

# Check the output of the script
if [ $? -ne 0 ]; then
	echo "ml-autosetup.sh failed, exiting..."
	exit 1
fi

# Check if /opt/ReNote/machine-learning/renote-ml.service exists
if [ ! -f "/opt/ReNote/machine-learning/renote-ml.service" ]; then
	echo "ml-autosetup.sh failed to create renote-ml.service, exiting..."
	exit 1
fi

cd /opt/ReNote/machine-learning/

# Move the systemd service to /etc/systemd/system/
sudo mv renote-ml.service /etc/systemd/system/

# Reload the systemd daemon
sudo systemctl daemon-reload

# Enable and start the renote-ml service
sudo systemctl enable renote-ml.service
sudo systemctl start renote-ml.service

# Add the compute engine to the backend-ml-server
gcloud compute instances add-tags "$INSTANCE_NAME" --tags="backend-ml-server" --zone="$INSTANCE_ZONE"

EOF

# Create a Compute Engine for the Backend API
setup_echo "normal" "Membuat compute engine untuk Machine Learning (dapat memakan waktu beberapa menit)..."
gcloud compute instances create machine-learning-vm \
	--zone="$DEFAULT_ZONE" \
	--machine-type="e2-medium" \
	--network-interface="network-tier=PREMIUM,private-network-ip=192.168.0.3,stack-type=IPV4_ONLY,subnet=renote-network-internal" \
	--maintenance-policy="MIGRATE" \
	--provisioning-model="STANDARD" \
	--service-account="maclea-service-account@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
	--scopes="compute-rw,storage-ro,service-control,service-management" \
	--create-disk="auto-delete=yes,boot=yes,device-name=machine-learning-vm,image=projects/ubuntu-os-cloud/global/images/ubuntu-2004-focal-v20241115,mode=rw,size=20,type=pd-ssd" \
	--no-shielded-secure-boot \
	--shielded-vtpm \
	--shielded-integrity-monitoring \
	--labels="goog-ec-src=vm_add-gcloud" \
	--reservation-affinity="any" \
	--metadata-from-file="startup-script=maclea-compute-engine-startup.sh,ssh-keys=additional-ssh-keys.txt"

# Tunggu hingga compute engine selesai dibuat dan ada tag "backend-ml-server"
setup_echo "normal" "Menunggu hingga setup script selesai..."
ML_VM_WAITING_ELAPSED=0
while true; do
	INSTANCE_TAGS=$(gcloud compute instances describe machine-learning-vm --zone="$DEFAULT_ZONE" --format="value(tags.items)")
	
	if [[ $INSTANCE_TAGS == *"backend-ml-server"* ]]; then
		break
	fi

	if [ $ML_VM_WAITING_ELAPSED -ge 600 ]; then
		setup_echo "error" "Setup script untuk compute engine Machine Learning memakan waktu terlalu lama, exiting..."
		exit 1
	fi

	setup_echo "info" "Menunggu hingga setup script selesai... ($ML_VM_WAITING_ELAPSED detik / 600 detik)"

	sleep 10
	ML_VM_WAITING_ELAPSED=$((ML_VM_WAITING_ELAPSED+10))
done

#endregion

cd ..
setup_echo "normal" "GCP Backend Infrastructure setup complete!"