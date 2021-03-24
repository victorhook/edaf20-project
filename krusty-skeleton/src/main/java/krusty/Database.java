package krusty;

import spark.Request;
import spark.Response;

import java.sql.*;
import java.util.*;
import java.util.Date;

import static krusty.Jsonizer.toJson;

public class Database {
	/**
	 * Modify it to fit your environment and then use this string when connecting to your database!
	 */
	private static final String jdbcString = "jdbc:mysql://localhost/krusty";

	// For use with MySQL or PostgreSQL
	private static final String dbUsername = "krustyadmin";
	private static final String dbPassword = "krustykaka123";
	private static final String database = "krustykookie";

	private static final String hostPort = "13337";
	private static final String hostIp = "83.250.66.137";
	private static final String jdbcHost = "jdbc:mysql://" + hostIp + ":" + hostPort + "/" + database;

	private static final String DEFAULT_PALLET_LOCATION = "transit";
	private static final int INVALID_COOKIE_NAME = -1;

	private Connection connection;

	public void connect() {
		try {
			connection = DriverManager.getConnection(jdbcHost, dbUsername, dbPassword);
			System.out.println("Connected to database");
		} catch (SQLException e) {
			System.out.println("ERROR: Failed to connect do database!");
			System.out.println(e);
		}
	}

	public static void main(String[] args) {
		new Database().connect();
	}

	// TODO: Implement and change output in all methods below!

	public String getCustomers(Request req, Response res) {
		//String result = selectQuery("Customers", "name", "address");
		//insertQuery("customers", Map.of("name", "hubet", "address", "furutorpsgatan 73"));
		return "{}";
	}

	public String getRawMaterials(Request req, Response res) {
		String result = selectQuery("storage", "raw-materials", "ingredientName", "amount", "Unit");
		// Need to do some renaming to match API spec.
		result = result.replace("ingredientName", "name");
		result = result.toLowerCase(Locale.ROOT);
		return result;
	}

	public String getCookies(Request req, Response res) {
		return selectQuery("recipes", "cookies", "cookieName");
	}

	public String getRecipes(Request req, Response res) {
		String result = selectQuery("ingredientinrecipes", "recipes",
							"cookieName", "ingredientName", "quantity", "unit");
		result = result.replace("ingredientName", "raw_material");
		result = result.replace("cookieName", "cookie");
		return result;
	}

	public String getPallets(Request req, Response res) {
		return "{\"pallets\":[]}";
	}

	public String reset(Request req, Response res) {
		return "{}";
	}

	/** POST /pallets?cookie=Amneris */
	public String createPallet(Request req, Response res) throws SQLException {
	 	String result;
	 	String cookieName = req.queryParams("cookie");

	 	if (cookieName == null) {
	 		// TODO: Handle this
		}

		int cookieId = getProductIdFromCookie(cookieName);
		if (cookieId == INVALID_COOKIE_NAME) {
			result = "{status: unknown cookie}";
		}

		String[] columns = new String[]{"product_id", "creationDate" , "isBlocked" , "location"};
		PreparedStatement stmt = makePreparedStatement("pallet", columns);

	 	if (stmt == null) {
			// TODO: HANDLE ERROR?
		} else {
	 		stmt.setInt(0, cookieId);
			stmt.setDate(1, getTodaysDate());
			stmt.setBoolean(2, false);
			stmt.setString(3, DEFAULT_PALLET_LOCATION);
			ResultSet resultSet = stmt.executeQuery();
			if (resultSet.next()) {
				System.out.println(resultSet);
				//result = String.format("{status: ok, id: %d}", palletId);
			}
		}

		return "{}";
	}

	private int getProductIdFromCookie(String name) {
		int id = INVALID_COOKIE_NAME;

		try {
			String query = "SELECT product_id FROM products WHERE cookieName = ?;";
			var stmt = connection.prepareStatement(query);
			stmt.setString(0, name);
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				id = result.getInt("product_id");
			}
		} catch (SQLException e) {
			System.out.println("ERROR Getting product id from cookie!");
		}

		return id;
	}

	/** Helper method to get todays date. */
	private java.sql.Date getTodaysDate() {
		return new java.sql.Date(System.currentTimeMillis());
	}

	/** Helper method to perform an insert query. */
	private PreparedStatement makePreparedStatement(String table, String[] columns) {
		PreparedStatement stmt = null;
		try {
			StringBuilder query = new StringBuilder("INSERT INTO ");
			query.append(table);

			StringJoiner keys = new StringJoiner(", ");
			StringJoiner values = new StringJoiner(", ");

			// Join all keys and their values to a string
			for (String column: columns) {
				keys.add(column);
				values.add("?");		// Prepare statement -> to prevent SQL injection.
			}

			// Add the keys to the query
			query.append(table + " (");
			query.append(keys.toString() + ")\n");
			query.append("VALUES (" + values.toString() + ");");

			stmt = this.connection.prepareStatement(query.toString());

		} catch (SQLException e) {
			System.out.printf("Error executing query: \n%s", e);
		}

		return stmt;
	}

	/** Helper method to perform a select query. */
	private String selectQuery(String table, String jsonName, String... columns) {
		// Default value is an empty object, in case an error occurs when querying!
		String jsonResult = "{}";

		try {
			Statement stmt = this.connection.createStatement();
			StringBuilder query = new StringBuilder("SELECT ");
			StringJoiner args = new StringJoiner(", ");
			for (String column: columns) {
				args.add(column);
			}
			query.append(args.toString());
			query.append("\nFROM " + table + ";");

			ResultSet result = stmt.executeQuery(query.toString());
			jsonResult = Jsonizer.toJson(result, jsonName);
		} catch (SQLException e) {
			System.out.printf("Error executing query: \n%s", e);
		}

		return jsonResult;
	}

}
