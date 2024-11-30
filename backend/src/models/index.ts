import { PoolConnection } from "mysql2/promise.js";
import { createModelSQL as createNoteModelSQL } from "./note.js";
import { createModelSQL as createRekeningModelSQL } from "./rekening.js";

export async function createModelSQL(conn: PoolConnection) {
	await conn.query(createNoteModelSQL());
	await conn.query(createRekeningModelSQL());
}