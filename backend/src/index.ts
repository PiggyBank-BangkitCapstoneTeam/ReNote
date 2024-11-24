import express from "express";
import FirebaseAuth from "./lib/firebase-auth.js";
import RouteHandler from "./lib/route_helper.js";
import dotenv from "dotenv";

// Load environment variable dari file .env
dotenv.config();

const app = express();

// Otomatis parse JSON yang diterima dari request body
app.use(express.json());

// Initialize firebase auth
const firebaseAuth = new FirebaseAuth();

//#region Area rute API yang tidak memerlukan verifikasi sesi Firebase Authentication
app.get("/", RouteHandler(() => {
	return {
		status: 200,
		message: "Selamat Datang PiggyBank's ReNote Backend API"
	};
}));
// #endregion

// TODO: Middleware to verify Firebase ID token
// app.use(firebaseAuth.VerifyIdToken);

//#region Area rute API yang akan membutuhkan verifikasi Firebase Authentication

import StrukRequestHandler from "./routes/struk.js";

app.get("/kumpulan_struk", StrukRequestHandler.getAllStruk);
app.post("/struk", StrukRequestHandler.addStruk);
app.get("/struk/:id", StrukRequestHandler.getStrukById);
app.put("/struk/:id", StrukRequestHandler.updateStruk);
app.delete("/struk/:id", StrukRequestHandler.deleteStruk);

//#endregion

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server berjalan pada http://localhost:${PORT}`);
});