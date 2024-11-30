import express_core from "express-serve-static-core";
import { DecodedIdToken } from "firebase-admin/lib/auth/token-verifier";

declare module "express-serve-static-core" {
	export interface Request extends express_core.Request {
		FirebaseUserData?: DecodedIdToken;
		CloudSQL?: GCP_CloudSQL;
	}
}