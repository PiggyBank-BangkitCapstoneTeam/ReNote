import express from "express";
import { DecodedIdToken } from "firebase-admin/lib/auth/token-verifier";
import GCP_CloudSQL from "../lib/gcp-cloudsql";

declare module "express" {
	export interface Express extends Application {
		request: Request;
		response: Response;
	}

	export interface Request extends express.Request {
		FirebaseUserData?: DecodedIdToken;
		CloudSQL?: GCP_CloudSQL;
	}
}