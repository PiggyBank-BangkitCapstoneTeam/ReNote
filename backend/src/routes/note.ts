import RouteHandler from "../lib/route_helper.js";
import { NoteModel, NoteModelRequestBody, NoteModelResponseBody, NoteModelUpdateRequestBody } from "../models/note.js";
import { nanoid } from "nanoid";

//TODO: Hapus array ini nanti
let Notes: NoteModel[] = [];

const getAllNote = RouteHandler((req) => {
	const data: NoteModelResponseBody[] = Notes.map((note) => {
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

const addNote = RouteHandler<NoteModelRequestBody>((req) => {
	if (!req.FirebaseUserData) {
		return {
			status: 401,
			message: "Anda tidak diizinkan menambahkan catatan"
		};
	}

	let kategori = req.body.kategori;
	let nominal = req.body.nominal;
	let deskripsi = req.body.deskripsi;
	let tanggal = req.body.tanggal;

	if (!kategori) {
		return {
			status: 400,
			message: "Kategori note tidak boleh kosong"
		};
	}

	if (!nominal) {
		return {
			status: 400,
			message: "Nominal note tidak boleh kosong"
		};
	}

	if (!deskripsi) {
		return {
			status: 400,
			message: "Deskripsi note tidak boleh kosong"
		};
	}

	if (!tanggal) {
		return {
			status: 400,
			message: "Tanggal note tidak boleh kosong"
		};
	}

	Notes.push({
		id: nanoid(),
		user_id: req.FirebaseUserData.uid,
		kategori: kategori,
		nominal: nominal,
		deskripsi: deskripsi,
		tanggal: tanggal
	});

	return {
		status: 201,
		message: "Note berhasil ditambahkan"
	};
});

const getNoteById = RouteHandler((req) => {
	const id = req.params.id;
	const note = Notes.find((note) => note.id === id);

	if (!note) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

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

const updateNote = RouteHandler<NoteModelUpdateRequestBody>((req) => {
	if (!req.FirebaseUserData) {
		return {
			status: 401,
			message: "Anda tidak diizinkan mengubah catatan"
		};
	}

	const id = req.params.id;
	const note = Notes.find((note) => note.id === id);

	if (!note) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

	// Logic to update note to the database
	let nominal = req.body.nominal;
	let deskripsi = req.body.deskripsi;

	if (!nominal) {
		return {
			status: 400,
			message: "Nominal note tidak boleh kosong"
		};
	}

	if (!deskripsi) {
		return {
			status: 400,
			message: "Deskripsi note tidak boleh kosong"
		};
	}

	note.nominal = nominal;
	note.deskripsi = deskripsi;

	return {
		status: 200,
		message: "Note berhasil diubah"
	};
});

const deleteNote = RouteHandler((req) => {
	if (!req.FirebaseUserData) {
		return {
			status: 401,
			message: "Anda tidak diizinkan menghapus catatan"
		};
	}

	const id = req.params.id;
	const noteIndex = Notes.findIndex((note) => note.id === id);

	if (noteIndex === -1) {
		return {
			status: 404,
			message: "Note tidak ditemukan"
		};
	}

	Notes.splice(noteIndex, 1);

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