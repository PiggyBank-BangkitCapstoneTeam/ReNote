import express from "express";
import FirebaseAuth from "./lib/firebase-auth.js";
import RouteHandler from "./lib/route_helper.js";
import { createModelSQL } from "./models/index.js";
import GCP_CloudSQL from "./lib/gcp-cloudsql.js";
import dotenv from "dotenv";

// Load environment variable dari file .env
dotenv.config();

const app = express();
const CloudSQL = new GCP_CloudSQL();

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

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server berjalan pada http://localhost:${PORT}`);
});