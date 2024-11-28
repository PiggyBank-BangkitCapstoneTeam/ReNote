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

	public async GetConnection() {
		if (!GCP_CloudSQL.Pool) {
			throw new Error("Koneksi Pool belum dibuat, panggil InitializePool() terlebih dahulu");
		}

		if (!GCP_CloudSQL.Connection) {
			GCP_CloudSQL.Connection = await GCP_CloudSQL.Pool.getConnection();
		}

		return GCP_CloudSQL.Connection;
	}

	public GetConnectionCallback(callback: (conn: mysql.PoolConnection) => void) {
		this.GetConnection().then(conn => {
			callback(conn);
			conn.release();
		});
	}
}