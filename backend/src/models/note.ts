export type NoteModel = {
	id: string;
	user_id: string;

	kategori: string;
	nominal: number;
	deskripsi: string;
	tanggal: string;
	photo_id?: string;
}

export type NoteModelResponseBody = Omit<NoteModel, "user_id">;

export type NoteModelRequestBody = Omit<NoteModel, "id" | "user_id">;
export type NoteModelUpdateRequestBody = Pick<NoteModel, "nominal" | "deskripsi">;

export type InternalMLScanRequest = {
	note_id: string;
	photo_id: string;
}
export type InternalMLScanResult = {
	success?: boolean;
	note_id?: string;
	photo_id?: string;
	result?: {
		/** Satu baris item dalam string
		*
		* @example
		* "COCA COLA  1.0M"
		* "KATSU 3  2.0M"
		*/
		item?: string[];

		/** Jumlah angka dalam string 
		*
		* @example "412.400"
		* @note Disarankan di parse jadi Integer agar tidak ada masalah dalam perhitungan
		*/
		total?: string;

		/** Waktu scan image
        *
        * @example "12/05/2024"
        */
		date_time?: string;

		/** Nama toko
        *
        * @example "WARUNG PASTA"
        */
		shop?: string;
	}
};

export function createModelSQL() {
	return `CREATE TABLE IF NOT EXISTS note (
		id CHAR(32) PRIMARY KEY,
		user_id VARCHAR(64) NOT NULL,
		kategori VARCHAR(64) NOT NULL,
		nominal INT NOT NULL,
		deskripsi VARCHAR(1024) NOT NULL,
		tanggal VARCHAR(32) NOT NULL,
		photo_id VARCHAR(128)
	)`.replace(/\n/g, " ").replace(/\t/g, "");
}