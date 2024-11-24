import RouteHandler from "../lib/route_helper.js";

//TODO: Hapus array ini nanti
let Struk: { id: number, nama: string }[] = [];

const getAllStruk = RouteHandler(() => {
	// Logic to get all struk from the database

    return {
        status: 200,
        data: Struk
    };
});

const addStruk = RouteHandler((req) => {
	// Logic to add struk to the database
	let nama_item = req.body.nama;

	if (!nama_item) {
		return {
			status: 400,
			message: "Nama item tidak boleh kosong"
		};
	}

	const newStrukId = Struk.length + 1;

	Struk.push({ id: newStrukId, nama: nama_item });

	return {
		status: 201,
		message: "Struk berhasil ditambahkan"
	};
});

const getStrukById = RouteHandler((req) => {
	// Logic to get struk by id from the database

    const struk = Struk.find((s) => s.id === parseInt(req.params.id));

    if (!struk) {
        return {
            status: 404,
            message: "Struk tidak ditemukan"
        };
    }

    return {
        status: 200,
        data: struk
    };
});

const updateStruk = RouteHandler((req) => {
	// Logic to update struk by id from the database

    const strukIndex = Struk.findIndex((s) => s.id === parseInt(req.params.id));

    if (strukIndex === -1) {
        return {
            status: 404,
            message: "Struk tidak ditemukan"
        };
    }

	let nama = req.body.nama;
	if (!nama) {
		return {
            status: 400,
            message: "Nama item tidak boleh kosong"
        };
	}

    Struk[strukIndex] = {
		id: Struk[strukIndex].id,
        nama: nama
	};

    return {
        status: 200,
        message: "Struk berhasil diperbarui"
    };
});

const deleteStruk = RouteHandler((req) => {
	// Logic to delete struk by id from the database

    const strukIndex = Struk.findIndex((s) => s.id === parseInt(req.params.id));

    if (strukIndex === -1) {
        return {
            status: 404,
            message: "Struk tidak ditemukan"
        };
    }

	Struk.splice(strukIndex, 1);

    return {
        status: 204,
        message: "Struk berhasil dihapus"
    };
});

export default {
	getAllStruk,
	addStruk,
	getStrukById,
	updateStruk,
	deleteStruk
};