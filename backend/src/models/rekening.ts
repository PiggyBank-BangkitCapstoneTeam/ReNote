export type RekeningModel = {
	id: string;
	user_id: string;
	
	name: string;
	uang: number;
}

export type RekeningModelRequestBody = Omit<RekeningModel, "id" | "user_id">

export type RekeningModelUpdateRequestBody = Omit<RekeningModel, "id" | "user_id" | "name">;

export type RekeningModelResponseBody = Omit<RekeningModel, "user_id">;

export function createModelSQL() {
	return `CREATE TABLE IF NOT EXISTS rekening (
		id CHAR(32) PRIMARY KEY,
		user_id VARCHAR(64) NOT NULL,
		name VARCHAR(64) NOT NULL,
		uang INT UNSIGNED NOT NULL
	)`.replace(/\n/g, " ").replace(/\t/g, "");
}