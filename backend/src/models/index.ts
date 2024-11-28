import { createModelSQL as createNoteModelSQL } from "./note.js";
import { createModelSQL as createRekeningModelSQL } from "./rekening.js";

export function createModelSQL() {
	return `
		${createNoteModelSQL()}
		${createRekeningModelSQL()}
	`;
}