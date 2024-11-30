import mysql from "mysql2/promise";

declare module "mysql2/promise" {
	export type QueryResult = mysql.OkPacket | mysql.ResultSetHeader | mysql.ResultSetHeader[] | mysql.RowDataPacket[] | mysql.RowDataPacket[][] | mysql.OkPacket[] | mysql.ProcedureCallPacket;

	export interface PoolConnection extends mysql.PoolConnection {
		query<T extends Object>(sql: string): Promise<[T[], FieldPacket[]]>;
		query<T extends Object>(sql: string, values: any): Promise<[T[], FieldPacket[]]>;


		//#region Original types
		query<T extends QueryResult>(sql: string): Promise<[T, FieldPacket[]]>;
		query<T extends QueryResult>(sql: string, values: any): Promise<[T, FieldPacket[]]>;
		query<T extends QueryResult>(options: QueryOptions): Promise<[T, FieldPacket[]]>;
		query<T extends QueryResult>(options: QueryOptions, values: any): Promise<[T, FieldPacket[]]>;

		execute<T extends QueryResult>(sql: string): Promise<[T, FieldPacket[]]>;
		execute<T extends QueryResult>(sql: string, values: any): Promise<[T, FieldPacket[]]>;
		execute<T extends QueryResult>(options: QueryOptions): Promise<[T, FieldPacket[]]>;
		execute<T extends QueryResult>(options: QueryOptions, values: any): Promise<[T, FieldPacket[]]>;
		//#endregion
	}
}