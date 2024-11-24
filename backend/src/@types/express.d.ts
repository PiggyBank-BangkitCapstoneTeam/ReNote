import express from "express";
import { DecodedIdToken } from "firebase-admin/lib/auth/token-verifier";

declare module "express" {
	export interface Request extends express.Request {
		FirebaseUserData?: DecodedIdToken;
	}
}