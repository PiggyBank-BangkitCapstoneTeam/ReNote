export type RekeningModel = {
	id: string;
	user_id: string;
	
	name: string;
	uang: bigint; // Tim ML menggunakan long di Kotlin, jadi tim CC pakai BigInt di JavaScript
}

export type RekeningModelRequestBody = Omit<RekeningModel, "id" | "user_id" | "uang"> & {
	uang: string; // string karena BigInt tidak bisa langsung di-parse dari/ke JSON
};

export type RekeningModelUpdateRequestBody = {
	uang: string; // string karena BigInt tidak bisa langsung di-parse dari/ke JSON
};

export type RekeningModelResponseBody = Omit<RekeningModel, "user_id" | "uang"> & {
	uang: string; // string karena BigInt tidak bisa langsung di-parse dari/ke JSON
};

export function createModelSQL() {
	return `CREATE TABLE IF NOT EXISTS rekening (
		id INT AUTO_INCREMENT PRIMARY KEY,
		user_id VARCHAR(64) NOT NULL,
		name VARCHAR(64) NOT NULL,
		uang UNSIGNED BIGINT NOT NULL
	)`;
}