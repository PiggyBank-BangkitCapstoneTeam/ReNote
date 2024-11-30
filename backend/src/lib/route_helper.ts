import { Request, Response, NextFunction } from "express";

export type RouteContext = {
	REQUEST_STARTTIME: Date;
	REQUEST_ENDTIME?: Date;
	REQUEST_PROCESSING_TIME_MS: number;
	RESPONSE_SIZE_BYTES: number;
}
export type RouteResponseValueType = string | number | boolean | Array<any> | { [key: string]: RouteResponseValueType } | object | undefined;

/** Nilai yang dikembalikan oleh function rute API */
export type RouteResponse = {
	status: number;
	[key: string]: RouteResponseValueType
}

type DecoratedFunctionReturnType = RouteResponse | Buffer | null | undefined | void;
type DecoratedFunction<T> = (req: GenericRequestType<T>, res: Response, next: NextFunction) => DecoratedFunctionReturnType | Promise<DecoratedFunctionReturnType>;

export type GenericRequestType<T> = Omit<Request, "body"> & { body: Partial<T> };

/**
 * Function decorator Express.JS yang serbaguna untuk mempermudah penanganan rute API
 * 
 * @param fn Function yang akan dijalankan ketika rute diakses
 * @returns Function Express.JS yang siap digunakan di app.get, app.post, app.put, app.delete, dll
 * 
 * @callback fn dapat mengembalikan salah satu nilai response valid, atau Raw Buffer
 * @async Fungsi parameter fn dapat bersifat async 
 * @this RouteContext Konteks yang akan digunakan oleh function rute API
 */
const decoratorFunction = <T = any>(fn: DecoratedFunction<T>) => (req: GenericRequestType<T>, res: Response, next: NextFunction) => {
	// Format: [DateTime] METHOD PATH: [String]
	const REQUEST_CONTEXT: RouteContext = {
		REQUEST_STARTTIME: new Date(),
		REQUEST_PROCESSING_TIME_MS: 0,
		RESPONSE_SIZE_BYTES: 0
	}
	REQUEST_CONTEXT.REQUEST_ENDTIME = REQUEST_CONTEXT.REQUEST_STARTTIME;

	// Pastikan function ini digunakan sebagai decorator Express.JS
	if (typeof req == "undefined" || typeof res == "undefined" || typeof next == "undefined") {
		console.error(`[${new Date().toISOString()}] NULL NULL: Jumlah parameter yang diberikan untuk rute salah, butuh 3`);
		return;
	}

	// Atur header Content-Type ke application/json jika belum diset
	if (!res.getHeader("Content-Type")) {
		res.setHeader("Content-Type", "application/json");
	}

	let ExecutionResult: ReturnType<DecoratedFunction<T>>;
	try {
		ExecutionResult = fn.apply(REQUEST_CONTEXT, [req, res, next]);
	}
	catch (error) {
		console.error(`[${REQUEST_CONTEXT.REQUEST_STARTTIME.toISOString()}] HTTP 500 ${req.method} '${req.url}' error: ${error}`);
		res.status(500).json({
			status: 500,
			message: "Terjadi kesalahan saat menjalankan kode backend"
		}).end();
		return;
	}

	if (ExecutionResult instanceof Promise) {
        ExecutionResult
            .then((PromiseExecutionResult) => {
				REQUEST_CONTEXT.REQUEST_ENDTIME = new Date();
				REQUEST_CONTEXT.REQUEST_PROCESSING_TIME_MS = REQUEST_CONTEXT.REQUEST_ENDTIME.getTime() - REQUEST_CONTEXT.REQUEST_STARTTIME.getTime();

				HandleExecutionResult(REQUEST_CONTEXT, PromiseExecutionResult, req, res);
				console.log(`[${REQUEST_CONTEXT.REQUEST_STARTTIME.toISOString()}] HTTP ${res.statusCode} ${req.method} '${req.url}' ${REQUEST_CONTEXT.RESPONSE_SIZE_BYTES} bytes, ${REQUEST_CONTEXT.REQUEST_PROCESSING_TIME_MS} ms`);
            })
            .catch((error) => {
                console.error(`[${REQUEST_CONTEXT.REQUEST_STARTTIME.toISOString()}] HTTP 500 ${req.method} '${req.url}' error: ${error}`);
                res.status(500).json({
                    status: 500,
                    message: "Terjadi kesalahan pada kode backend"
                }).end();
            });
        return;
    }

	REQUEST_CONTEXT.REQUEST_ENDTIME = new Date();
	REQUEST_CONTEXT.REQUEST_PROCESSING_TIME_MS = REQUEST_CONTEXT.REQUEST_ENDTIME.getTime() - REQUEST_CONTEXT.REQUEST_STARTTIME.getTime();
	HandleExecutionResult(REQUEST_CONTEXT, ExecutionResult, req, res);

	console.log(`[${REQUEST_CONTEXT.REQUEST_STARTTIME.toISOString()}] HTTP ${res.statusCode} ${req.method} '${req.url}' ${REQUEST_CONTEXT.RESPONSE_SIZE_BYTES} bytes, ${REQUEST_CONTEXT.REQUEST_PROCESSING_TIME_MS} ms`);
};

function HandleExecutionResult(REQUEST_CONTEXT: RouteContext, ExecutionResult: DecoratedFunctionReturnType, req: Request, res: Response) {
	// Jika tidak ada nilai yang dikembalikan
	if (!ExecutionResult) {
		res.end();
		return;
	}

	// Jika tipe response adalah raw buffer, langsung kirim sebagai body (HTTP 200 OK)
	if (Buffer.isBuffer(ExecutionResult)) {
		res.status(200).end(ExecutionResult);
		return;
	}

	// #region Area handle non-promise/sync execution
	if (ExecutionResult.status < 1) {
		throw new Error("HTTP Status Code harus lebih besar dari 0");
	}

	try {
		let ExecutionResultJSONEncoded = JSON.stringify(ExecutionResult);
		REQUEST_CONTEXT.RESPONSE_SIZE_BYTES = ExecutionResultJSONEncoded.length;

	    res.status(ExecutionResult.status).end(ExecutionResultJSONEncoded);
	}
	catch (error) {
		console.error(`[${REQUEST_CONTEXT.REQUEST_STARTTIME.toISOString()}] HTTP 500 ${req.method} '${req.url}' error: ${error}`);
		res.status(500).json({
			status: 500,
			message: "Terjadi kesalahan saat mengonversi nilai hasil kode backend"
		}).end();
		return;
	}
}

export default decoratorFunction;