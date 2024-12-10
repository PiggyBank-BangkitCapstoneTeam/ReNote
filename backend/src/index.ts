import express from "express";
import FirebaseAuth from "./lib/firebase-auth.js";
import RouteHandler from "./lib/route_helper.js";
import { createModelSQL } from "./models/index.js";
import GCP_CloudSQL from "./lib/gcp-cloudsql.js";
import dotenv from "dotenv";
import multer from "multer";
import { Storage } from "@google-cloud/storage";
import MemoryStoreRedis from "redis";
import ReNote_MLConnector from "./lib/ml-connector.js";

// Load environment variable dari file .env
dotenv.config();

const app = express();
const file_upload_storage = multer.memoryStorage();
const file_upload = multer({
	storage: file_upload_storage,
	limits: {
		fileSize: 10 * 1024 * 1024, // 10 MB
		files: 2
	}
});
const CloudSQL = new GCP_CloudSQL();
const CloudStorage = new Storage();
const MLConnector = new ReNote_MLConnector(CloudSQL, CloudStorage);

async function InitializeDatabase() {
	console.log("Menyiapkan koneksi database di CloudSQL...");
	await CloudSQL.InitializePool();

	console.log("Menghubungkan ke Cloud SQL...");
	await CloudSQL.Connect();
	console.log("Terhubung ke Cloud SQL");


	console.log("Inisialisasi database dimulai...");
	const conn = await CloudSQL.GetConnection();
	await createModelSQL(conn);
	conn.release();
	console.log("Inisialisasi database selesai");
}

if (process.env.CloudSQL_Enabled === "true") {
	await InitializeDatabase();

	app.use((req, res, next) => {
		req.CloudSQL = CloudSQL;
		next();
	});
}

if (process.env.CloudStorage_Enabled === "true") {
	const CloudStorage_UserMediaBucket = process.env.CloudStorage_UserMediaBucket;

	// Jika kosong atau ada gs:// di depannya, maka tidak valid
	if (!CloudStorage_UserMediaBucket) {
		throw new Error("Cloud Storage diaktifkan tetapi CloudStorage_UserMediaBucket tidak diisi");
	}

	// Jika ada gs:// di depannya, tetap tidak valid
	if (CloudStorage_UserMediaBucket.startsWith("gs://")) {
		throw new Error("CloudStorage_UserMediaBucket tidak diawali dengan gs://");
	}

	app.use((req, res, next) => {
		req.CloudStorage = CloudStorage;
		req.CloudStorage_UserMediaBucket = CloudStorage.bucket(CloudStorage_UserMediaBucket);
		next();
	});
}

if (process.env.MemoryStoreRedis_Enabled === "true") {
	const MemoryStoreRedis_HostName = process.env.MemoryStoreRedis_HostName;
	const MemoryStoreRedis_Port = process.env.MemoryStoreRedis_Port || "6379";

	if (!MemoryStoreRedis_HostName) {
		throw new Error("MemoryStore diaktifkan tetapi MemoryStoreRedis_HostName tidak diisi");
	}

	if (!MemoryStoreRedis_Port) {
		throw new Error("MemoryStore diaktifkan tetapi MemoryStoreRedis_Port tidak diisi");
	}

	const MemoryStore = MemoryStoreRedis.createClient({
		url: `redis://${MemoryStoreRedis_HostName}:${MemoryStoreRedis_Port}`
	});

	try {
		MemoryStore.connect();
		
		await MemoryStore.SET("ReNoteMemoryStore_ConnectionProbe", "probe", {
			EX: 1,
		});
	}
	catch (error) {
		console.error("Gagal terhubung ke MemoryStore for Redis");
		console.error(error);
		process.exit(1);
	}

	app.use((req, res, next) => {
		req.MemoryStore = MemoryStore;
		next();
	});
}

if (process.env.PUBSUB_Enabled === "true") {
	MLConnector.Initialize();

	app.use((req, res, next) => {
        req.ReNote_MLConnector = MLConnector;
        next();
    });
}

// Otomatis parse JSON yang diterima dari request body
app.use(express.json());

//#region Area rute API yang tidak memerlukan verifikasi Firebase Authentication
app.get("/", RouteHandler(() => {
	return {
		status: 200,
		message: "Selamat Datang PiggyBank's ReNote Backend API"
	};
}));
// #endregion

if (process.env.ENVIRONMENT === "production") {
	// Proses verifikasi Firebase Authentication
	const firebaseAuth = new FirebaseAuth();
	app.use((req, res, next) => { firebaseAuth.VerifyIdToken(req, res, next); });
}
else {
	// Fake/Simulasi Firebase Authentication
	app.use((req, res, next) => {
		req.FirebaseUserData = {
			aud: "1234567890",
			auth_time: 1234567890,
			exp: 1234567890,
			firebase: {
				identities: {
					email: ["user@example.com"],
					email_verified: true
				},
				sign_in_provider: "google.com"
			},
			uid: "1234567890",
			iat: 1234567890,
			iss: "https://securetoken.google.com/piggybank-3b7b4",
			sub: "1234567890"
		};
		
		next();
	});
}


//#region Area rute API yang akan membutuhkan verifikasi Firebase Authentication

import NoteRequestHandler from "./routes/note.js";

app.get("/kumpulan_note", NoteRequestHandler.getAllNote);
app.post("/note", NoteRequestHandler.addNote);
app.get("/note/:id", NoteRequestHandler.getNoteById);
app.put("/note/:id", NoteRequestHandler.updateNote);
app.get("/note/:id/struk", NoteRequestHandler.getFotoStruk);
app.post("/note/:id/struk", file_upload.array("foto"), NoteRequestHandler.uploadFotoStruk);
app.delete("/note/:id", NoteRequestHandler.deleteNote);

import RekeningRequestHandler from "./routes/rekening.js";

app.get("/kumpulan_rekening", RekeningRequestHandler.getAllRekening);
app.post("/rekening", RekeningRequestHandler.addRekening);
app.get("/rekening/:id", RekeningRequestHandler.getRekeningById);
app.put("/rekening/:id", RekeningRequestHandler.updateRekening);
app.delete("/rekening/:id", RekeningRequestHandler.deleteRekening);

//#endregion

app.use(RouteHandler(() => {
	return {
		status: 404,
		message: "Halaman tidak ditemukan"
	}
}));

app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
	console.error(err);
	res.status(500).json({
		status: 500,
		message: "Terjadi kesalahan pada server"
	});
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server berjalan pada http://localhost:${PORT}`);
});