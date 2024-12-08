import express_core from "express-serve-static-core";
import { DecodedIdToken } from "firebase-admin/lib/auth/token-verifier";
import { Storage, Bucket } from "@google-cloud/storage";
import { createClient } from "redis";

declare module "express-serve-static-core" {
	export interface Request extends express_core.Request {
		FirebaseUserData?: DecodedIdToken;
		CloudSQL?: GCP_CloudSQL;
		CloudStorage?: Storage;
		CloudStorage_UserMediaBucket?: Bucket;
		MemoryStore?: ReturnType<typeof createClient>;
	}
}