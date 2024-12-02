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

export function createModelSQL() {
	return `CREATE TABLE IF NOT EXISTS note (
		id CHAR(32) PRIMARY KEY,
		user_id VARCHAR(64) NOT NULL,
		kategori VARCHAR(64) NOT NULL,
		nominal INT UNSIGNED NOT NULL,
		deskripsi VARCHAR(1024) NOT NULL,
		tanggal VARCHAR(32) NOT NULL,
		photo_id VARCHAR(128)
	)`.replace(/\n/g, " ").replace(/\t/g, "");
}