# Script to setup the cloud infrastructure for ReNote on GCP

#region Variabel untuk nilai nilai default
GCP_PROJECT_ID="$(gcloud config get-value project)"
DEFAULT_SQL_ROOT_PASSWORD="renote-mysql-root"
DEFAULT_SQL_BACKEND_API_PASSWORD="renote-mysql-backend-api"

DEFAULT_REGION="asia-southeast2"
DEFAULT_ZONE="$DEFAULT_REGION-b"
#endregion

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

# Test otentikasi Cloud Shell
setup_echo "normal" "Mencoba otentikasi Cloud Shell..."
gcloud auth print-access-token > /dev/null
if [ $? -ne 0 ]; then
	setup_echo "error" "Gagal melakukan otentikasi gcloud, run script lagi jika ingin mencoba lagi"
	exit 1
fi

#region Service Account

# Create a service account for the Backend API Compute Engine
setup_echo "normal" "Membuat service account..."
gcloud iam service-accounts create "backend-service-account" \
	--description="Service Account for Backend API Compute Engine" \
	--display-name="Backend Service Account"

SERVICE_ACCOUNT_EMAIL="backend-service-account@$GCP_PROJECT_ID.iam.gserviceaccount.com"

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

# Grant the service account the "Storage Object User" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
	--role="roles/storage.objectAdmin"

# Grant the service account the "Firebase Authentication Viewer" role
gcloud projects add-iam-policy-binding "$GCP_PROJECT_ID" \
	--member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" \
	--role="roles/firebaseauth.viewer"

echo
#endregion

#region VPC Network

# Enable Compute Engine API
setup_echo "normal" "Mengaktifkan Compute Engine API (dapat memakan waktu beberapa menit)..."
gcloud services enable compute.googleapis.com
sleep 5

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
	--source-ranges="35.235.240.0/20" \
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


# Enable Service Networking API
setup_echo "normal" "Mengaktifkan Service Networking API..."
gcloud services enable servicenetworking.googleapis.com
sleep 5

# Create a VPC Peering allocation for Managed Services (Redis, Cloud SQL, etc.) connection
setup_echo "normal" "Membuat VPC Peering untuk Managed Services..."
gcloud compute addresses create managed-services-peering \
	--global \
	--purpose="VPC_PEERING" \
	--addresses="192.168.10.0" \
	--prefix-length="24" \
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
sleep 5

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
#endregion

#region Cloud Storage
#TODO: Setup Cloud Storage bucket buat nyimpen file dari user
#endregion

#region Memorystore (Redis)
#TODO: Create Memorystore (Redis) instance buat caching data backend
#endregion

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

# Setup backend user without no password (system)
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

EOF

# Create a Compute Engine for the Backend API
setup_echo "normal" "Membuat compute engine untuk Backend API (dapat memakan waktu beberapa menit)..."
gcloud compute instances create backend-api \
	--zone="$DEFAULT_ZONE" \
	--machine-type="e2-small" \
	--network-interface="network-tier=PREMIUM,private-network-ip=192.168.0.2,stack-type=IPV4_ONLY,subnet=renote-network-internal" \
	--maintenance-policy="MIGRATE" \
	--provisioning-model="STANDARD" \
	--service-account="backend-service-account@$GCP_PROJECT_ID.iam.gserviceaccount.com" \
	--no-scopes \
	--tags="backend-api-server" \
	--create-disk="auto-delete=yes,boot=yes,device-name=instance-20241130-102327,image=projects/ubuntu-os-cloud/global/images/ubuntu-2404-noble-amd64-v20241115,mode=rw,size=10,type=pd-balanced" \
	--no-shielded-secure-boot \
	--shielded-vtpm \
	--shielded-integrity-monitoring \
	--labels="goog-ec-src=vm_add-gcloud" \
	--reservation-affinity="any" \
	--metadata-from-file="startup-script=backend-api-compute-engine-startup.sh"

#endregion

cd ..
setup_echo "normal" "GCP Backend Infrastructure setup complete!"