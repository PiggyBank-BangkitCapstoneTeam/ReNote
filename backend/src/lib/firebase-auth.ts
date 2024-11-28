import FirebaseAdmin from "firebase-admin";
import { Request, Response, NextFunction } from "express";

export default class {
	private FirebaseAdminApp: FirebaseAdmin.app.App;

	constructor() {
		this.FirebaseAdminApp = FirebaseAdmin.initializeApp({
			credential: FirebaseAdmin.credential.applicationDefault(), // Akan menggunakan credential dari environment GOOGLE_APPLICATION_CREDENTIALS
		});
	}

	public VerifyIdToken(request: Request, response: Response, next: NextFunction) {
		// Pastikan ada header Authorization yang diberikan dari client
		if (!request.headers.authorization) {
			response.status(401).send({
				message: "Hak akses ditolak"
			});

			console.log(`${request.method} ${request.url}: 401 Tidak ada token otorisasi`);
			return;
		}

		// Cek apakah token memiliki 2 part (Bearer <token>)
		const authParts = request.headers.authorization.split(" ");
		if (authParts.length !== 2) {
			response.status(401).send({
				message: "Format token otorisasi tidak valid"
			});
			
			console.log(`${request.method} ${request.url}: 401 Token otorisasi tidak valid`);
			return;
		}
		const token = authParts[1];

		// Cek di Firebase apakah token valid
		this.FirebaseAdminApp.auth().verifyIdToken(token)
			.then((decodedToken) => {
				// Simpan data user dari Firebase ke request agar bisa digunakan di rute API
				request.FirebaseUserData = decodedToken;

				// Lanjutkan ke rute API
				next();
			})
			.catch((error) => {
				let errorInfo: Partial<{ code: string, message: string}> = error.errorInfo;

				if (!errorInfo) {
					response.status(401).send({ message: "Hak akses ditolak" }).end();
					console.log(`${request.method} ${request.url}: 401 Token otorisasi ditolak, alasan: ${error}`);
					return;
				}

				if (errorInfo.code === "auth/id-token-expired") {
					response.status(401).send({ message: "Hak akses ditolak, token sudah kedaluwarsa" }).end();
					console.log(`${request.method} ${request.url}: 401 Token otorisasi expired`);
					return;
				}

				response.status(401).send({ message: "Hak akses ditolak" }).end();
				console.log(`${request.method} ${request.url}: 401 Token otorisasi ditolak, alasan: ${errorInfo.message}`);
			});
	}
}