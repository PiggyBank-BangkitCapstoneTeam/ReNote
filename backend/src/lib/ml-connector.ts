import { PubSub, Subscription, Topic } from "@google-cloud/pubsub";
import { InternalMLScanRequest, InternalMLScanResult } from "../models/note.js";
import GCP_CloudSQL from "./gcp-cloudsql.js";
import { ResultSetHeader } from "mysql2";
import { Bucket, Storage } from "@google-cloud/storage";
import { InstancesClient, ZoneOperationsClient } from "@google-cloud/compute";

type MLServerStatus = "RUNNING" | "SUSPENDED" | "STOPPED" | "TERMINATED";

export default class ReNote_MLConnector {
	private photo_scan_queue: InternalMLScanRequest[] = []; // Array id gambar yang butuh di scan oleh model tim ML
	private current_photo_scan_job?: InternalMLScanRequest;
	private WorkTimer?: NodeJS.Timeout;
	private WORKER_BUSY: boolean = false;
	private WORKER_LAST_BUSY_TIMESTAMP: number;
	/** Variabel Increment untuk menghindari menunggu worker yang hang/dead selamanya */
	private WORKER_BUSY_DEAD_PROTECTION = 0;
	private WORKER_BUSY_DEAD_PROTECTION_SEC = 30; // maksimum 30 detik
	private pubsub?: PubSub;
	private pubsub_RequestTopic?: Topic;
	private pubsub_ResponseTopic?: Topic;
	private pubsub_ResponseSubscription?: Subscription;
	private CloudSQL: GCP_CloudSQL;
	private CloudStorageBucket: Bucket;
	private ComputeInstance: InstancesClient;
	private ComputeManager_BUSY: boolean = false;
	private ComputeManagerTimer?: NodeJS.Timeout;
	private ComputeManagerStatusCheckTimer?: NodeJS.Timeout;
	private ComputeManagerInstanceParam = {
		project: "",
		zone: "",
		instance: "machine-learning-vm"
	}
	private ComputeManagerSleepAfterIdleForSeconds: number = 300; // Default 5 menit
	private ComputeManagerInstanceState: MLServerStatus = "RUNNING";

	constructor(CloudSQL: GCP_CloudSQL, CloudStorage: Storage, GCEInstanceManager: InstancesClient) {
		this.CloudSQL = CloudSQL;
		this.ComputeInstance = GCEInstanceManager;
		this.WORKER_LAST_BUSY_TIMESTAMP = Date.now();

		const CloudStorage_UserMediaBucket = process.env.CloudStorage_UserMediaBucket;
		if (!CloudStorage_UserMediaBucket) {
			throw new Error("[MLConnector] Cloud Storage diaktifkan tetapi CloudStorage_UserMediaBucket tidak diisi");
		}
		this.CloudStorageBucket = CloudStorage.bucket(CloudStorage_UserMediaBucket);
	}

	public async Initialize() {
		await this.InitializePubSub();

		if (this.WorkTimer) {
			clearInterval(this.WorkTimer);
			this.WorkTimer = undefined;
		}

		this.WorkTimer = setInterval(() => {
			if (this.WORKER_BUSY_DEAD_PROTECTION > this.WORKER_BUSY_DEAD_PROTECTION_SEC) {
				console.error(`[MLConnector] Tidak ada respon dari kode ML selama ${this.WORKER_BUSY_DEAD_PROTECTION_SEC} detik, job lock di reset`);

				// Cek apakah ada job yang harusnya diproses
				const active_job = this.current_photo_scan_job;

				// Jika ada, insert lagi di paling awal
				if (active_job) {
					this.photo_scan_queue.unshift(active_job);
				}

				this.current_photo_scan_job = undefined;

				this.WORKER_BUSY_DEAD_PROTECTION = 0;
				this.WORKER_BUSY = false;
				return;
			}

			if (this.IsWorkerBusy()) {
				this.WORKER_BUSY_DEAD_PROTECTION++;
				return;
			}

			this.WORKER_BUSY = true;
			this.WORKER_BUSY_DEAD_PROTECTION = 0;

			const photo_scan_job = this.photo_scan_queue.shift();

			if (!photo_scan_job) {
				this.WORKER_BUSY = false;
				return;
			}
			this.current_photo_scan_job = photo_scan_job;
			this.WORKER_LAST_BUSY_TIMESTAMP = Date.now();

			this.ScanImage(photo_scan_job);
		}, 1000);

		if (process.env.COMPUTE_MANAGER_Enabled === "true") {
			this.ComputeManagerInstanceParam.project = process.env.COMPUTE_MANAGER_ProjectId || "";
			if (!this.ComputeManagerInstanceParam.project) {
				throw new Error("[MLConnector] COMPUTE_MANAGER_ProjectId harus diisi");
			}

			this.ComputeManagerInstanceParam.zone = process.env.COMPUTE_MANAGER_ML_SERVER_Zone || "";
			if (!this.ComputeManagerInstanceParam.zone) {
				throw new Error("[MLConnector] COMPUTE_MANAGER_ML_SERVER_ZONE harus diisi");
			}

			const COMPUTE_MANAGER_ML_SERVER_SleepAfterIdleForSeconds = process.env.COMPUTE_MANAGER_ML_SERVER_SleepAfterIdleForSeconds;
			if (!COMPUTE_MANAGER_ML_SERVER_SleepAfterIdleForSeconds) {
				throw new Error("[MLConnector] COMPUTE_MANAGER_ML_SERVER_SleepAfterIdleForSeconds harus diisi");
			}

			// Ambil dari .env atau default ke 5 menit
			this.ComputeManagerSleepAfterIdleForSeconds = parseInt(COMPUTE_MANAGER_ML_SERVER_SleepAfterIdleForSeconds) || 300; // 5 menit default

			this.ComputeManagerTimer = setInterval(() => {
				if (this.ComputeManager_BUSY) {
					return;
				}

				this.ComputeManager_BUSY = true;
				this.ComputeManagerDoWork();
			}, Math.max(60, this.ComputeManagerSleepAfterIdleForSeconds) * 1000); // Pilih mana yang lebih lama agar gak spam

			this.ComputeManagerStatusCheckTimer = setInterval(async () => {
				this.ComputeManagerInstanceState = await this.GetMLServerStatus();
			}, 30000);

			// Ambil status awal dari compute engine ML
			this.ComputeManagerInstanceState = await this.GetMLServerStatus();
		}
	}

	public async InitializePubSub() {
		if (!process.env.PUBSUB_ProjectId) {
			throw new Error("[MLConnector] PUBSUB_ProjectId harus diisi");
		}

		if (!process.env.PUBSUB_ML_RequestTopicId) {
			throw new Error("[MLConnector] PUBSUB_ML_RequestTopicId harus diisi");
		}

		if (!process.env.PUBSUB_ML_RequestSubscriptionId) {
			throw new Error("[MLConnector] PUBSUB_ML_RequestSubscriptionId harus diisi");
		}

		if (!process.env.PUBSUB_ML_ResponseTopicId) {
			throw new Error("[MLConnector] PUBSUB_ML_ResponseTopicId harus diisi");
		}

		if (!process.env.PUBSUB_ML_ResponseSubscriptionId) {
			throw new Error("[MLConnector] PUBSUB_ML_ResponseSubscriptionId harus diisi");
		}

		this.pubsub = new PubSub({
			projectId: process.env.PUBSUB_ProjectId,
			keyFilename: process.env.GOOGLE_APPLICATION_CREDENTIALS
		});

		this.pubsub_RequestTopic = this.pubsub.topic(process.env.PUBSUB_ML_RequestTopicId);

		this.pubsub_ResponseTopic = this.pubsub.topic(process.env.PUBSUB_ML_ResponseTopicId);

		// Pastikan subscription sudah dibuat
		this.pubsub_ResponseSubscription = this.pubsub_ResponseTopic.subscription(process.env.PUBSUB_ML_ResponseSubscriptionId);

		this.pubsub_ResponseSubscription.on("message", (message) => {
			message.ack();

			this.onMLResponse(message.data);
		});

		this.pubsub_ResponseSubscription.on("error", (err) => {
			console.error("[MLConnector] Error pada subscription:", err);
		});
	}

	private async ScanImage(job_data: InternalMLScanRequest) {
		if (!this.pubsub_RequestTopic) {
			this.WORKER_BUSY = false;
			return;
		}

		const file = this.CloudStorageBucket.file(job_data.photo_id);
		const [isFileExists] = await file.exists();

		if (!isFileExists) {
			console.error(`[MLConnector] Gambar photo_id ${job_data.photo_id} untuk note ${job_data.note_id} tidak ditemukan di Cloud Storage`);
			this.WORKER_BUSY = false;
			this.current_photo_scan_job = undefined;
			return;
		}

		if (this.ComputeManagerInstanceState !== "RUNNING") {
			// Reset waktu proteksi dead
			this.WORKER_BUSY_DEAD_PROTECTION = 0;

			console.log(`[MLConnector] Server ML sedang ${this.ComputeManagerInstanceState}, menyiapkan server ML...`);
			await this.BangunkanServer();
			this.WORKER_BUSY_DEAD_PROTECTION = 0;

			await new Promise((resolve) => setTimeout(resolve, 5000));
			this.WORKER_BUSY_DEAD_PROTECTION = 0;
		}

		this.pubsub_RequestTopic.publishJSON(job_data, (_) => {
			console.log(`[MLConnector] Sukses broadcast scan photo ${job_data.photo_id} pada note ${job_data.note_id}`);
		});
	}

	public AddImageToScan(photo_id: InternalMLScanRequest) {
		this.photo_scan_queue.push(photo_id);
	}

	public IsWorkerBusy(): boolean {
		return this.WORKER_BUSY;
	}

	private async onMLResponse(data: Buffer) {
		this.WORKER_BUSY = false;
		this.WORKER_LAST_BUSY_TIMESTAMP = Date.now();

		// JSON convert
		let jsonData: InternalMLScanResult;
		try {
			jsonData = JSON.parse(data.toString());
		}
		catch (error) {
			console.error("[MLConnector] Error parsing response dari ML", error);
			return;
		}

		if (typeof jsonData.success === "undefined") {
			console.error("[MLConnector] Response dari kode ML tidak ada indikator success");
			return;
		}

		if (!jsonData.success) {
			console.error("[MLConnector] Kode ML memberikan respon gagal", jsonData);
			return;
		}

		if (!jsonData.note_id) {
			console.error("[MLConnector] Response dari kode ML tidak ada note_id");
			return;
		}

		if (!jsonData.photo_id) {
			console.error("[MLConnector] Response dari kode ML tidak ada photo_id");
			return;
		}

		if (!jsonData.result) {
			console.error("[MLConnector] Response dari kode ML tidak ada result");
			return;
		}

		if (!jsonData.result.item) {
			console.error("[MLConnector] Response dari kode ML tidak ada array item");
			return;
		}

		if (!jsonData.result.total) {
			console.error("[MLConnector] Response dari kode ML tidak ada total");
			return;
		}

		// Buang semua . dan , dari total
		const total_clean = jsonData.result.total.replace(/[,.]/g, "");
		const nominal_total = parseInt(total_clean);

		if (!jsonData.result.shop) {
			console.error("[MLConnector] Response dari kode ML tidak ada shop");
			return;
		}

		let description = `${jsonData.result.shop}\n`;

		for (const item of jsonData.result.item) {
			description += `  - ${item}\n`;
		}

		// Hapus \n diakhir jika ada
		description = description.replace(/\n$/, "");

		// Simpan ke database
		const conn = await this.CloudSQL.GetConnection();
		const [result] = await conn.execute<ResultSetHeader>(
			"UPDATE note SET nominal = ?, deskripsi = ? WHERE id = ? AND photo_id = ?",
			[nominal_total, description, jsonData.note_id, jsonData.photo_id]
		);
		conn.release();

		if (result.affectedRows === 0) {
			console.warn("[MLConnector] Gagal update note dari hasil scan ML, tidak ada data yang diupdate");
			return;
		}
		console.log(`[MLConnector] Update note berhasil pada photo_id ${jsonData.photo_id}`);
	}

	public ClearScanQueue() {
		this.photo_scan_queue = [];
	}

	public StopWorker() {
		if (this.WorkTimer) {
			clearInterval(this.WorkTimer);
		}

		if (this.pubsub_ResponseSubscription) {
			this.pubsub_ResponseSubscription.close();
		}

		if (this.ComputeManagerTimer) {
			clearInterval(this.ComputeManagerTimer);
		}

		if (this.ComputeManagerStatusCheckTimer) {
			clearInterval(this.ComputeManagerStatusCheckTimer);
		}
	}

	private async ComputeManagerDoWork() {
		// Cek apakah job ML terakhir sudah lebih dari batas waktu idle
		const now = Date.now();
		const elapsed_ms = now - this.WORKER_LAST_BUSY_TIMESTAMP;
		const elapsed_seconds = elapsed_ms / 1000;

		if (elapsed_seconds < this.ComputeManagerSleepAfterIdleForSeconds) {
			this.ComputeManager_BUSY = false;
			return;
		}

		console.log("[ComputeManager] Mulai mengecek status server ML...");

		let status = await this.GetMLServerStatus();

		if (status === "STOPPED" || status === "TERMINATED") {
			this.ComputeManager_BUSY = false;
			console.log("[ComputeManager] Server ML sedang mati, tidak perlu suspend");
			return;
		}
		else if (status === "SUSPENDED") {
			this.ComputeManager_BUSY = false;
			console.log("[ComputeManager] Server ML sudah di-suspend, tidak perlu di-suspend lagi");
			return;
		}

		console.log("[ComputeManager] Server ML akan di-suspend karena idle terlalu lama");

		// Tidurkan server ML
		await this.TidurkanServer();
		await new Promise((resolve) => setTimeout(resolve, 5000));
		status = await this.GetMLServerStatus();
		this.ComputeManagerInstanceState = status;

		if (status === "RUNNING") {
			console.log("[ComputeManager] Server ML masih berjalan padahal sudah di-suspend !!!");
			this.ComputeManager_BUSY = false;
			return;
		}

		this.ComputeManager_BUSY = false;
	}

	private async TidurkanServer() {
		const [response] = await this.ComputeInstance.suspend(this.ComputeManagerInstanceParam);
		let operation = response.latestResponse;

		const OperationsClient = new ZoneOperationsClient();

		while (true) {
			const [operationStatus] = await OperationsClient.get({
				project: this.ComputeManagerInstanceParam.project,
				zone: this.ComputeManagerInstanceParam.zone,
				operation: operation.name
			});

			if (operationStatus.status === "DONE") {
				break;
			}

			if (operationStatus.status === "UNDEFINED_STATUS") {
				console.error("[ComputeManager] Error mendapatkan status operasi suspend", operationStatus);
				return;
			}
		}
	}

	private async GetMLServerStatus() {
		const [response] = await this.ComputeInstance.get(this.ComputeManagerInstanceParam);

		if (!response.status) {
			console.warn("[MLConnector] Status server ML tidak diketahui, untuk aman dianggap aktif");
			return "RUNNING";
		}

		return response.status as MLServerStatus;
	}

	private async BangunkanServer() {
		let response;

		if (this.ComputeManagerInstanceState === "SUSPENDED") {
			[response] = await this.ComputeInstance.resume(this.ComputeManagerInstanceParam);
		}
		else {
			[response] = await this.ComputeInstance.start(this.ComputeManagerInstanceParam);
		}

		let operation = response.latestResponse;

		const OperationsClient = new ZoneOperationsClient();

		while (true) {
			const [operationStatus] = await OperationsClient.get({
				project: this.ComputeManagerInstanceParam.project,
				zone: this.ComputeManagerInstanceParam.zone,
				operation: operation.name
			});

			if (operationStatus.status === "DONE") {
				break;
			}

			if (operationStatus.status === "UNDEFINED_STATUS") {
				console.error("[ComputeManager] Error mendapatkan status operasi suspend", operationStatus);
				return;
			}
		}
	}
}