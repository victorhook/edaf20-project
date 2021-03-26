package krusty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.JSONPObject;
import spark.Request;
import spark.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
	private static final int INVALID_COOKIE_NAME = -1, BAD_RESULT = -1, ERROR = -1, UNKNOWN_COOKIE = -2, PALLET_OK = 1;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private Connection connection;
	private DefaultRecipes recipes;

	public Database() {
		recipes = new DefaultRecipes();
	}

	public void connect() {
		try {
			connection = DriverManager.getConnection(jdbcHost, dbUsername, dbPassword);
			System.out.println("Connected to database");
		} catch (SQLException e) {
			System.out.println("ERROR: Failed to connect do database!");
			System.out.println(e);
		}
	}

	public static void main(String[] args) throws IOException {
		new Database();
	}

	/**
	 * DONE:
	 * - customers
	 * - raw_materials
	 * - cookies
	 * - recipes
	 *
	 * NOT DONE:
	 * - pallets (get )
	 * - reset
	 */
	// TODO: Implement and change output in all methods below!

	public String getCustomers(Request req, Response res) {
		String result = selectQuery("Customers", "customers", "name", "address");
		return result;
	}

	public String getRawMaterials(Request req, Response res) {
		String result = selectQuery("storage", "raw-materials", "ingredientName", "amount", "unit");
		// Need to do some renaming to match API spec.
		result = result.replace("ingredientName", "name");
		//result = result.toLowerCase(Locale.ROOT);
		return result;
	}

	public String getCookies(Request req, Response res) {
		String result = selectQuery("recipes", "cookies", "cookieName");
		// Need to match API
		result = result.replace("cookieName", "name");
		return result;
	}

	public String getRecipes(Request req, Response res) {
		String result = selectQuery("ingredientinrecipes", "recipes",
							"cookieName", "ingredientName", "quantity", "unit");
		result = result.replace("ingredientName", "raw_material");
		result = result.replace("cookieName", "cookie");
		return result;
	}

	public String getPallets(Request req, Response res) throws SQLException, ParseException {
		// Build base query, joining all tables that we will need.
		StringBuilder query = new StringBuilder(
				"SELECT pallet_id, cookieName, creationDate, name, isBlocked\n" +
				"FROM pallets\n" +
				"JOIN products USING (product_id)\n" +
				"JOIN orders USING (order_id)\n" +
				"JOIN customers USING (customer_id)\n"
		);

		// This joiner is used to bind together query conditions.
		StringJoiner condition = new StringJoiner(" AND ");

		Map<String, String> conditions = new HashMap<>();
		var params = Map.of(
				"id", "pallet_id = ?",
				"cookie", "cookieName = ?",
				"from", "creationDate > ?",
				"to", "creationDate < ?",
				"customer", "name = ?",
				"blocked", "blocked = ?"
		);

		// Check all the parameters and if they exists in the params, add them to tha condition list.
		for (var entry: params.entrySet()) {
			String param = req.queryParams(entry.getKey());
			if (param != null) {
				conditions.put(entry.getValue(), param);
			}
		}

		// If we have any query conditions, we add WHERE; otherwise not.
		if (params.size() > 0)
			query.append("WHERE ");

		// Add all condition strings to the query.
		for (var cond: conditions.keySet()) {
			condition.add(cond);
		}

		// Add the conditions to the query string.
		query.append(condition.toString() + ";");

		// Create statement
	 	PreparedStatement stmt = this.connection.prepareStatement(query.toString());

		// Set all the variables for the statement.
		int index = 1;
		for (var entry: conditions.entrySet()) {
			// For every condition given, set the value for the prepared statement.
			// Some of this is hardcoded due to specific types.
			String queryPart = entry.getKey();
			String value = entry.getValue();

			if (queryPart.contains("id")) {
				stmt.setInt(index, Integer.parseInt(value));
			} else if (queryPart.contains("Date")) {
				stmt.setDate(index, toSqlDate(value));
			} else if (queryPart.contains("blocked")) {
				stmt.setBoolean(index, Boolean.parseBoolean(value));
			} else {
				stmt.setString(index, value);
			}
			index++;
		}

		System.out.println(stmt + "\n");
		String result = Jsonizer.toJson(stmt.executeQuery(), "pallets");
		return result;
	}

	public String reset(Request req, Response res) {
		return "{}";
	}

	/** Helper method to convert raw string date to SQL string date object. */
	private java.sql.Date toSqlDate(String date) throws ParseException {
		Date parsed = DATE_FORMAT.parse(date);
		return new java.sql.Date(parsed.getTime());
	}

	private int getLastPalletId() throws SQLException {
		ResultSet result = this.connection.createStatement().executeQuery(
				"SELECT pallet_id " +
					"FROM pallets " +
					"ORDER BY pallet_id DESC LIMIT 1;"

		);

		if (result.next()) {
			return result.getInt(1);
		} else {
			return BAD_RESULT;
		}
	}

	private void updateStorage(String name, int amount) throws SQLException {
		String query = "UPDATE storage SET ? = ? WHERE ";
		PreparedStatement stmt = this.connection.prepareStatement(query);
		stmt.setString(1, name);
		stmt.setInt(2, amount);
		stmt.executeUpdate();
	}

	private Recipe getRecipe(String recipe) {
		for (Recipe rec: recipes.recipes) {
			if (rec.name.equals(recipe)) {
				return rec;
			}
		}
		return null;
	}

	/** POST /pallets?cookie=Amneris */
	public String createPallet(Request req, Response res) throws SQLException {
	 	String cookieName = req.queryParams("cookie");
		int cookieId = -1;
		int palletId = -1;
		int resultStatus = PALLET_OK;

	 	if (cookieName == null) {
			resultStatus = ERROR;
		}

	 	if (resultStatus != ERROR) {
			cookieId = getProductIdFromCookie(cookieName);
			if (cookieId == INVALID_COOKIE_NAME) {
				resultStatus = UNKNOWN_COOKIE;
			}
		}

	 	// Update storage information!
		this.connection.setAutoCommit(false);

	 	// Check if we can create a new pallet!
		var ingredients= getRecipe(cookieName).ingredients;
		String query =  "UPDATE storage\n" +
						"SET amount = amount - 100\n" +
						"WHERE ingredientName IN\n" +
						"(\n" +
						"SELECT ingredientName\n" +
						"FROM ingredientinrecipes\n" +
						"WHERE cookieName = ?\n" +
						");";

		PreparedStatement stmt= this.connection.prepareStatement(query);
		stmt.setString(1, cookieName);
		int rowsChanged = stmt.executeUpdate();

		// If all rows changed, we know that we can create the pallet!
		if (rowsChanged == ingredients.length) {
			this.connection.commit();
		} else {
			resultStatus = ERROR;
		}

	 	if (resultStatus != ERROR && resultStatus != UNKNOWN_COOKIE) {

			String[] columns = new String[] {"product_id", "creationDate" , "isBlocked" , "location"};
			stmt = makePreparedStatement("pallets", columns);

			if (stmt == null) {
				resultStatus = ERROR;
			} else {
				stmt.setInt(1, cookieId);
				stmt.setDate(2, getTodaysDate());
				stmt.setBoolean(3, false);
				stmt.setString(4, DEFAULT_PALLET_LOCATION);

				resultStatus = stmt.executeUpdate();
				palletId = getLastPalletId();
			}
		}

	 	if (resultStatus == PALLET_OK) {
	 		// Need to update Pallet 36 x 10 x 15 =

	 		return  "{\n\t\"status\": \"ok\" " +
					"\n\t\"id\": " + palletId + "\n}";
		} else if (resultStatus == ERROR) {
			return "{\n\t\"status\": \"error\"\n}";
		} else {
			return "{\n\t\"status\": \"unknown cookie\"\n}";
		}

	}

	private int getProductIdFromCookie(String name) {
		int id = INVALID_COOKIE_NAME;

		try {
			String query = "SELECT product_id FROM products WHERE cookieName = ?;";
			var stmt = connection.prepareStatement(query);
			stmt.setString(1, name);
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
