import { ResultSetHeader } from "mysql2";
import RouteHandler from "../lib/route_helper.js";
import { RekeningModel, RekeningModelRequestBody, RekeningModelResponseBody, RekeningModelUpdateRequestBody } from "../models/rekening.js";
import { customAlphabet } from "nanoid";

const nanoid = customAlphabet("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 32);

const getAllRekening = RouteHandler(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.query<RekeningModel>(
		"SELECT * FROM rekening WHERE user_id = ?",
		[req.FirebaseUserData.uid]
	);
	conn.release();

	const data = result.map((rekening) => {
		return {
			id: rekening.id,
			name: rekening.name,
			uang: rekening.uang
		};
	});

	return {
		status: 200,
		data: data
	};
});

const addRekening = RouteHandler<RekeningModelRequestBody>(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	let name = req.body.name;
	let uang = req.body.uang;

	if (!name) {
		return {
			status: 400,
			message: "Nama rekening harus ada pada request body"
		};
	}

	if (typeof uang === "undefined") {
		return {
			status: 400,
			message: "Jumlah uang dalam rekening harus ada pada request body"
		};
	}

	if (uang < 0) {
		return {
			status: 400,
			message: "Jumlah uang dalam rekening harus lebih besar atau sama dengan 0"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.execute<ResultSetHeader>(
		"INSERT INTO rekening (id, user_id, name, uang) VALUES (?, ?, ?, ?)",
		[nanoid(), req.FirebaseUserData.uid, name, uang]
	);
	conn.release();

	if (result.affectedRows === 0) {
		return {
			status: 500,
			message: "Gagal menambahkan rekening"
		};
	}

	return {
		status: 201,
		message: "Rekening berhasil ditambahkan"
	};
});

const getRekeningById = RouteHandler(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	let id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID rekening harus ada pada parameter URL"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.query<RekeningModel>(
		"SELECT * FROM rekening WHERE id = ? AND user_id = ?",
		[id, req.FirebaseUserData.uid]
	);
	conn.release();

	if (result.length === 0) {
		return {
			status: 404,
			message: "Rekening tidak ditemukan"
		};
	}

	const rekening = result[0];

	const data: RekeningModelResponseBody = {
		id: rekening.id,
		name: rekening.name,
		uang: rekening.uang
	};

	return {
		status: 200,
		data: data
	};
});

const updateRekening = RouteHandler<RekeningModelUpdateRequestBody>(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	let id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID rekening harus ada pada parameter URL"
		};
	}

	let uang = req.body.uang;

	if (!uang) {
		return {
			status: 400,
			message: "Jumlah uang dalam rekening harus ada pada request body"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.execute<ResultSetHeader>(
		"UPDATE rekening SET uang = ? WHERE id = ? AND user_id = ?",
		[uang, id, req.FirebaseUserData.uid]
	);
	conn.release();

	if (result.affectedRows === 0) {
		return {
			status: 404,
			message: "Rekening tidak ditemukan"
		};
	}

	return {
		status: 200,
		message: "Rekening berhasil diupdate"
	};
});

const deleteRekening = RouteHandler(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	let id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID rekening harus ada pada parameter URL"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.execute<ResultSetHeader>(
		"DELETE FROM rekening WHERE id = ? AND user_id = ?",
		[id, req.FirebaseUserData.uid]
	);
	conn.release();

	if (result.affectedRows === 0) {
		return {
			status: 404,
			message: "Rekening tidak ditemukan"
		};
	}

	return {
		status: 200,
		message: "Rekening berhasil dihapus"
	};
});

export default {
	getAllRekening,
	addRekening,
	getRekeningById,
	updateRekening,
	deleteRekening
};