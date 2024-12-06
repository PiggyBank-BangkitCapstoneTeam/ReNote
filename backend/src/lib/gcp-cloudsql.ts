import mysql from "mysql2/promise";
import { Connector, IpAddressTypes } from "@google-cloud/cloud-sql-connector";

export default class GCP_CloudSQL {
	private static Pool?: mysql.Pool;
	private static Connection?: mysql.PoolConnection;
	private static StateReportTimer?: NodeJS.Timeout;

	public async InitializePool() {
		const connector = new Connector();

		let clientOpts;
		try {
			clientOpts = await connector.getOptions({
				instanceConnectionName: process.env.CloudSQL_ConnectionName || "",
				ipType: (process.env.CloudSQL_IpAddressType || "") as (IpAddressTypes | undefined)
			});
		}
		catch (err: any) {
			if (err.message) {
				if (err.message.includes("not in an appropriate state to handle the request")) {
					throw new Error("Cloud SQL tidak siap untuk menerima koneksi, apakah sudah dinyalakan dan siap?");
				}
			}

			console.error("Gagal mendapatkan opsi koneksi Cloud SQL");
			throw err;
		}
		

		GCP_CloudSQL.Pool = mysql.createPool({
			...clientOpts,

			user: process.env.CloudSQL_Username,
			password: process.env.CloudSQL_Password,
			database: process.env.CloudSQL_Database,

			enableKeepAlive: true,
			keepAliveInitialDelay: 60 * 1000,
			idleTimeout: 1 * 60 * 1000 // Tutup koneksi jika sudah 15 menit idle
		});
	}

	/** Buat koneksi ke Cloud SQL */
	public async Connect() {
		if (!GCP_CloudSQL.Pool) { throw new Error("Pool belum dibuat"); }
		
		const conn = await GCP_CloudSQL.Pool.getConnection();

		await conn.connect();
		await conn.ping();
		conn.release();
		
		if (GCP_CloudSQL.StateReportTimer) {
			clearInterval(GCP_CloudSQL.StateReportTimer);
            GCP_CloudSQL.StateReportTimer = undefined;
		}

		GCP_CloudSQL.StateReportTimer = setInterval(() => {
			if (!GCP_CloudSQL.Pool) { return; }

			let TotalConnection = GCP_CloudSQL.Pool.pool._allConnections.length;
			let ConnectionFree = GCP_CloudSQL.Pool.pool._freeConnections.length;
			let PendingQueryCount = GCP_CloudSQL.Pool.pool._connectionQueue.length;

			console.log(`[Cloud SQL Status] Free Connection: ${ConnectionFree}, Queued: ${PendingQueryCount}, Total: ${TotalConnection}`);
		}, 60 * 1000); // Cek setiap 1 menit
	}

	/** Putuskan koneksi ke Cloud SQL */
	public async Disconnect() {
		if (!GCP_CloudSQL.Pool) { return; }

		await GCP_CloudSQL.Pool.end();
		GCP_CloudSQL.Pool = undefined;
		GCP_CloudSQL.Connection = undefined;

		if (GCP_CloudSQL.StateReportTimer) {
            clearInterval(GCP_CloudSQL.StateReportTimer);
            GCP_CloudSQL.StateReportTimer = undefined;
        }
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

	/** A variant of GetConnection that automatically releases the connection after execution is done */
	public GetConnectionCallback(callback: (conn: mysql.PoolConnection) => void) {
		this.GetConnection().then(conn => {
			callback(conn);
			conn.release();
		});
	}
}