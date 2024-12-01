# Script untuk mengubah infrastruktur agar mengizinkan testing diluar GCP

#region Variabel untuk nilai nilai default
GCP_PROJECT_ID="$(gcloud config get-value project)"
#endregion

#region Cloud SQL

# Update CloudSQL (MySQL) instance agar dapat diakses dari Internet
gcloud sql instances patch renote-mysql \
	--database-version="MYSQL_8_4" \
	--network="renote-network" \
	--assign-ip \
	--edition="enterprise"

#endregion
