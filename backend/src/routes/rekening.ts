import RouteHandler from "../lib/route_helper.js";
import { RekeningModel, RekeningModelRequestBody, RekeningModelResponseBody, RekeningModelUpdateRequestBody } from "../models/rekening.js";
import { nanoid } from "nanoid";

//TODO: Hapus array ini nanti
let Rekening: RekeningModel[] = [];

const getAllRekening = RouteHandler((req) => {
	const data: RekeningModelResponseBody[] = Rekening.map((rekening) => {
		return {
			id: rekening.id,
			name: rekening.name,
			uang: rekening.uang.toString()
		};
	});

	return {
		status: 200,
		data: data
	};
});

const addRekening = RouteHandler<RekeningModelRequestBody>((req) => {
	if (!req.FirebaseUserData) {
		return {
			status: 401,
			message: "Anda tidak diizinkan menambahkan rekening"
		};
	}

	// Logic to add struk to the database
	let name = req.body.name;
	let uang = req.body.uang;

	if (!name) {
		return {
			status: 400,
			message: "Nama rekening tidak boleh kosong"
		};
	}

	if (!uang) {
		return {
			status: 400,
			message: "Jumlah uang dalam rekening tidak boleh kosong"
		};
	}

	Rekening.push({
		id: nanoid(),
		user_id: req.FirebaseUserData.uid,
		name: name,
		uang: BigInt(uang)
	});

	return {
		status: 201,
		message: "Rekening berhasil ditambahkan"
	};
});

const getRekeningById = RouteHandler((req) => {
	let id = req.params.id;
	let rekening = Rekening.find((rekening) => rekening.id === id);

	if (!rekening) {
		return {
			status: 404,
			message: "Rekening tidak ditemukan"
		};
	}

	const data: RekeningModelResponseBody = {
		id: rekening.id,
		name: rekening.name,
		uang: rekening.uang.toString()
	};

	return {
		status: 200,
		data: data
	};
});

const updateRekening = RouteHandler<RekeningModelUpdateRequestBody>((req) => {
	let id = req.params.id;
	let rekening = Rekening.find((rekening) => rekening.id === id);

	if (!rekening) {
		return {
			status: 404,
			message: "Rekening tidak ditemukan"
		};
	}

	let uang = req.body.uang;

	if (!uang) {
		return {
			status: 400,
			message: "Jumlah uang dalam rekening tidak boleh kosong"
		};
	}

	rekening.uang = BigInt(uang);

	return {
		status: 200,
		message: "Rekening berhasil diupdate"
	};
});

const deleteRekening = RouteHandler((req) => {
	let id = req.params.id;
	let rekeningIndex = Rekening.findIndex((rekening) => rekening.id === id);

	if (rekeningIndex === -1) {
		return {
			status: 404,
			message: "Rekening tidak ditemukan"
		};
	}

	Rekening.splice(rekeningIndex, 1);

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