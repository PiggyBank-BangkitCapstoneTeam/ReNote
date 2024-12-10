import { PubSub, Subscription, Topic } from "@google-cloud/pubsub";
import { InternalMLScanRequest, InternalMLScanResult } from "../models/note.js";
import GCP_CloudSQL from "./gcp-cloudsql.js";
import { ResultSetHeader } from "mysql2";
import { Bucket, Storage } from "@google-cloud/storage";

export default class ReNote_MLConnector {
	private photo_scan_queue: InternalMLScanRequest[] = []; // Array id gambar yang butuh di scan oleh model tim ML
	private WorkTimer?: NodeJS.Timeout;
	private WORKER_BUSY: boolean = false;
	/** Variabel Increment untuk menghindari menunggu worker yang hang/dead selamanya */
	private WORKER_BUSY_DEAD_PROTECTION = 0;
	private WORKER_BUSY_DEAD_PROTECTION_SEC = 30; // maksimum 30 detik
	private pubsub?: PubSub;
	private pubsub_RequestTopic?: Topic;
	private pubsub_ResponseTopic?: Topic;
	private pubsub_ResponseSubscription?: Subscription;
	private CloudSQL: GCP_CloudSQL;
	private CloudStorage: Storage;
	private CloudStorageBucket: Bucket;

	constructor(CloudSQL: GCP_CloudSQL, CloudStorage: Storage) {
		this.CloudSQL = CloudSQL;
		this.CloudStorage = CloudStorage;

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

			const photo_id = this.photo_scan_queue.shift();

			if (!photo_id) {
				this.WORKER_BUSY = false;
				return;
			}

			this.ScanImage(photo_id);
		}, 1000);
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
            return;
        }

		this.pubsub_RequestTopic.publishJSON(job_data, (_) => {
			console.log(`[MLConnector] Sukses broadcast scan gambar untuk photo_id ${job_data.photo_id}`);
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
	}
}