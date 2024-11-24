import mysql from "mysql2/promise";
import { Connector, IpAddressTypes } from "@google-cloud/cloud-sql-connector";

export default class GCP_CloudSQL {
	private static Pool?: mysql.Pool;
	private static Connection?: mysql.PoolConnection;

	public async InitializePool() {
		const PoolDetails = {
			instanceConnectionName: process.env.CLOUD_SQL_CONNECTION_NAME || "",
			ipType: process.env.CLOUD_SQL_CONNECTION_IP_TYPE || "",

			user: process.env.CLOUD_SQL_DATABASE_USERNAME || "",
			password: process.env.CLOUD_SQL_DATABASE_PASSWORD || "",
			database: process.env.CLOUD_SQL_DATABASE_NAME || "",
		}

		const connector = new Connector();
		const clientOpts = await connector.getOptions({
			instanceConnectionName: PoolDetails.instanceConnectionName,
			ipType: PoolDetails.ipType as IpAddressTypes
		});

		GCP_CloudSQL.Pool = mysql.createPool({
			...clientOpts,
			...PoolDetails
		});
	}

	/** Buat koneksi ke Cloud SQL */
	public async Connect() {
		if (!GCP_CloudSQL.Pool) { throw new Error("Pool belum dibuat"); }
		GCP_CloudSQL.Pool.connect();

		console.log("Terhubung ke Cloud SQL");
	}

	/** Putuskan koneksi ke Cloud SQL */
	public async Disconnect() {
		if (!GCP_CloudSQL.Pool) { return; }

		await GCP_CloudSQL.Pool.end();
		GCP_CloudSQL.Pool = undefined;
		GCP_CloudSQL.Connection = undefined;
	}

	/** Mendapatkan SQL Connection dari pool, pastikan untuk release() setelah selesai */
	private async GetConnection() {
		if (!GCP_CloudSQL.Pool) {
			throw new Error("Koneksi Pool belum dibuat, panggil InitializePool() terlebih dahulu");
		}

		const conn = await GCP_CloudSQL.Pool.getConnection();

		return conn;
	}

	public async InitializeTable() {
		const conn = await this.GetConnection();

		await conn.query("CREATE TABLE IF NOT EXISTS item (id INT AUTO_INCREMENT PRIMARY KEY, nama VARCHAR(255) NOT NULL, author VARCHAR(255) NOT NULL)");

		console.log("Tabel item berhasil dibuat");
		conn.release();
	}
}