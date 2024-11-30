import { ResultSetHeader } from "mysql2";
import RouteHandler from "../lib/route_helper.js";
import { NoteModel, NoteModelRequestBody, NoteModelResponseBody, NoteModelUpdateRequestBody } from "../models/note.js";
import { customAlphabet } from "nanoid";

const nanoid = customAlphabet("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 32);

const getAllNote = RouteHandler(async(req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.query<NoteModel>("SELECT * FROM note WHERE user_id = ?", [req.FirebaseUserData.uid]);
	conn.release();

	const data: NoteModelResponseBody[] = result.map((note) => {
		return {
			id: note.id,
			kategori: note.kategori,
			nominal: note.nominal,
			deskripsi: note.deskripsi,
			tanggal: note.tanggal
		};
	});

	return {
		status: 200,
		data: data
	};
});

const addNote = RouteHandler<NoteModelRequestBody>(async(req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	let kategori = req.body.kategori;
	let nominal = req.body.nominal;
	let deskripsi = req.body.deskripsi;
	let tanggal = req.body.tanggal;

	if (!kategori) {
		return {
			status: 400,
			message: "Kategori note harus ada pada request body"
		};
	}

	if (typeof nominal === "undefined") {
		return {
			status: 400,
			message: "Nominal note harus ada pada request body"
		};
	}

	if (nominal < 0) {
		return {
			status: 400,
			message: "Nominal note harus lebih besar atau sama dengan 0"
		}
	}

	if (!deskripsi) {
		return {
			status: 400,
			message: "Deskripsi note harus ada pada request body"
		};
	}

	if (!tanggal) {
		return {
			status: 400,
			message: "Tanggal note harus ada pada request body"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.execute<ResultSetHeader>(
		"INSERT INTO note (id, user_id, kategori, nominal, deskripsi, tanggal) VALUES (?, ?, ?, ?, ?, ?)",
		[nanoid(), req.FirebaseUserData.uid, kategori, nominal, deskripsi, tanggal]
	);
	conn.release();

	if (result.affectedRows === 0) {
		return {
			status: 500,
			message: "Gagal menambahkan note"
		};
	}

	return {
		status: 201,
		message: "Note berhasil ditambahkan"
	};
});

const getNoteById = RouteHandler(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	const id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID rekening harus ada pada parameter URL"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.query<NoteModel>("SELECT * FROM note WHERE id = ? AND user_id = ?", [id, req.FirebaseUserData.uid]);
	conn.release();

	if (result.length === 0) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

	const note = result[0];

	const mappedNote: NoteModelResponseBody = {
		id: note.id,
		kategori: note.kategori,
		nominal: note.nominal,
		deskripsi: note.deskripsi,
		tanggal: note.tanggal
	};

	return {
		status: 200,
		data: mappedNote
	};
});

const updateNote = RouteHandler<NoteModelUpdateRequestBody>(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	const id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID note harus ada pada parameter URL"
		};
	}

	let nominal = req.body.nominal;
	let deskripsi = req.body.deskripsi;

	if (typeof nominal === "undefined") {
		return {
			status: 400,
			message: "Nominal note harus ada pada request body"
		};
	}

	if (nominal < 0) {
		return {
			status: 400,
			message: "Nominal note harus lebih besar atau sama dengan 0"
		};
	}

	if (!deskripsi) {
		return {
			status: 400,
			message: "Deskripsi note harus ada pada request body"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.execute<ResultSetHeader>(
		"UPDATE note SET nominal = ?, deskripsi = ? WHERE id = ? AND user_id = ?",
		[nominal, deskripsi, id, req.FirebaseUserData.uid]
	);
	conn.release();

	if (result.affectedRows === 0) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

	return {
		status: 200,
		message: "Note berhasil diubah"
	};
});

const deleteNote = RouteHandler(async (req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return { status: 500, message: "Koneksi database tidak tersedia" };
	}

	const id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID note harus ada pada parameter URL"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.execute<ResultSetHeader>(
		"DELETE FROM note WHERE id = ? AND user_id = ?", 
		[id, req.FirebaseUserData.uid]
	);
	conn.release();

	if (result.affectedRows === 0) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

	return {
		status: 200,
		message: "Note berhasil dihapus"
	};
});

export default {
	getAllNote,
	addNote,
	getNoteById,
	updateNote,
	deleteNote
};