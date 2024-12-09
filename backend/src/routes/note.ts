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

	if (typeof nominal !== "number") {
		return {
			status: 400,
			message: "Nominal note harus ada pada request body dan berupa angka"
		};
	}

	if (nominal === 0) {
		return {
			status: 400,
			message: "Nominal note tidak boleh kosong"
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

	if (typeof nominal !== "number") {
		return {
			status: 400,
			message: "Nominal note harus ada pada request body dan berupa angka"
		};
	}

	if (nominal === 0) {
		return {
			status: 400,
			message: "Nominal note tidak boleh kosong"
		}
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

const getFotoStruk = RouteHandler(async(req, res) => {
	if (!req.FirebaseUserData) {
        // Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
        throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
    }

	if (!req.CloudSQL) {
		return {
			status: 500,
			message: "Koneksi Cloud Storage ke bucket user media tidak tersedia"
		};
	}

	if (!req.CloudStorage_UserMediaBucket) {
		return {
			status: 500,
			message: "Koneksi Cloud Storage ke bucket user media tidak tersedia"
		};
	}

	const id = req.params.id;
	if (!id) {
		return {
			status: 400,
			message: "ID note harus ada pada parameter URL"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.query<Pick<NoteModel, "photo_id">>(
        "SELECT photo_id FROM note WHERE id =? AND user_id =?",
        [id, req.FirebaseUserData.uid]
    );
	conn.release();

	if (result.length === 0) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

	const photo_id = result[0].photo_id;

	if (!photo_id) {
		return {
			status: 404,
			message: "Foto struk tidak ditemukan"
		};
	}

	const file = req.CloudStorage_UserMediaBucket.file(photo_id);

	return {
		status: 200,
		data: {
			id: photo_id,
			url: file.publicUrl()
		}
	};
});

const uploadFotoStruk = RouteHandler(async(req) => {
	if (!req.FirebaseUserData) {
		// Seharusnya tidak sampai ke sini dikarenakan sudah dihandle oleh Firebase Authentication
		throw new Error("lib/firebase-auth.ts tidak berjalan dengan baik");
	}

	if (!req.CloudSQL) {
		return {
			status: 500,
			message: "Koneksi database tidak tersedia"
		};
	}

	if (!req.CloudStorage_UserMediaBucket) {
		return {
			status: 500,
			message: "Koneksi Cloud Storage ke bucket user media tidak tersedia"
		};
	}

	let photos = req.files;

	if (!photos) {
		return {
			status: 400,
			message: "File foto harus ada pada request body (sebagai form-data dengan key 'foto')"
		};
	}

	// Jika sebuah objek multipart, ambil key 'foto'
	if (!Array.isArray(photos)) {
		photos = photos["foto"];
	}

	// Jika arraynya kosong
	if (photos.length !== 1) {
		return {
			status: 400,
			message: "Hanya satu foto struk yang dapat diupload"
		};
	}

	const photo = photos[0];

	// Batasi hanya boleh upload gambar
	if (!photo.mimetype.startsWith("image/")) {
		return {
			status: 400,
			message: "File yang diupload harus berupa gambar"
		};
	}

	const id = req.params.id;

	if (!id) {
		return {
			status: 400,
			message: "id harus ada pada request body"
		};
	}

	const conn = await req.CloudSQL.GetConnection();
	const [result] = await conn.query<Pick<NoteModel, "photo_id">>("SELECT photo_id FROM note WHERE id = ? AND user_id = ?", [id, req.FirebaseUserData.uid]);

	if (result.length === 0) {
		return {
			status: 404,
			message: "Note tidak ditemukan, tidak dapat menambahkan foto struk"
		};
	}
	const old_photo_id = result[0].photo_id;
	if (old_photo_id) {
		const old_file = req.CloudStorage_UserMediaBucket.file(old_photo_id);
		if (await old_file.exists()) {
			await old_file.delete();
		}
	}

	const photo_id = nanoid();
	const file = req.CloudStorage_UserMediaBucket.file(photo_id);


	await file.save(photo.buffer, {
		metadata: {
			contentType: photo.mimetype,
			"Cache-Control": "private, max-age=43200"
		}
	});

	const [result2] = await conn.execute<ResultSetHeader>("UPDATE note SET photo_id = ? WHERE id = ? AND user_id = ?", [photo_id, id, req.FirebaseUserData.uid]);
	conn.release();

	if (result2.affectedRows === 0) {
		return {
			status: 500,
			message: "Gagal generate URL foto struk"
		};
	}

	return {
		status: 200,
		data: {
			id: photo_id,
			url: file.publicUrl()
		}
	};
});

export default {
	getAllNote,
	addNote,
	getNoteById,
	updateNote,
	getFotoStruk,
	uploadFotoStruk,
	deleteNote
};