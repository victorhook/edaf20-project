package krusty;

import spark.Request;
import spark.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;


public class Database {

	// MySQL db credentials.
	// Note: We didn't use the schools VPN, so ensure the port is changed from 3306 to 13337.
	private static final String dbUsername = "krustyadmin";
	private static final String dbPassword = "krustykaka123";
	private static final String database = "krustykookie";
	private static final String hostPort = "13337";
	private static final String hostIp = "83.250.66.137";

	// Need to add timezone info for connection.
	private static final String jdbcString = "jdbc:mysql://" + hostIp + ":" + hostPort + "/" + database + "?serverTimezone=Europe/Stockholm";

	// Constants
	private static final String DEFAULT_PALLET_LOCATION = "transit";
	private static final int BAD_RESULT = -1, ERROR = -1, UNKNOWN_COOKIE = -2, PALLET_OK = 1;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");

	private Connection connection;
	private DefaultRecipes recipes;

	public Database() {
		recipes = new DefaultRecipes();
	}

	/** Tries to connect to the database and throws an exception of it fails. */
	public void connect() throws SQLException {
		connection = DriverManager.getConnection(jdbcString, dbUsername, dbPassword);
	}

	/* --- Queries ---- */

	/** Returns all customers. */
	public String getCustomers(Request req, Response res) {
		String result = selectQuery("Customers", "customers", "name", "address");
		return result;
	}

	/** Returns all raw-materials. */
	public String getRawMaterials(Request req, Response res) {
		String result = selectQuery("storage", "raw-materials", "ingredientName", "amount", "unit");
		// Need to do some renaming to match API spec.
		result = result.replace("ingredientName", "name");
		//result = result.toLowerCase(Locale.ROOT);
		return result;
	}

	/** Returns all cookies. */
	public String getCookies(Request req, Response res) {
		String result = selectQuery("recipes", "cookies", "name");
		return result;
	}

	/** Returns all recipes. */
	public String getRecipes(Request req, Response res) {
		String result = selectQuery("ingredientinrecipes", "recipes",
							     "cookie", "ingredientName", "quantity", "unit");
		result = result.replace("ingredientName", "raw_material");
		return result;
	}

	/** Returns all pallets that match a certain criteria. */
	public String getPallets(Request req, Response res) throws SQLException, ParseException {
		// Build base query, joining all tables that we will need.
		StringBuilder query = new StringBuilder(
				"SELECT pallet_id, cookie, production_date, name, blocked\n" +
				"FROM pallets\n" +
				"LEFT JOIN orders USING (order_id)\n" +
				"LEFT JOIN customers USING (customer_id)\n"
		);

		// This joiner is used to bind together query conditions.
		StringJoiner conditionJoiner = new StringJoiner(" AND ");

		Map<String, String> conditions = new HashMap<>();
		var params = Map.of(
				"cookie", "cookie = ?",
				"from", "production_date >= ?",
				"to", "production_date <= ?",
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
		if (conditions.size() > 0)
			query.append("WHERE ");

		// Add all condition strings to the query.
		for (var cond: conditions.keySet()) {
			conditionJoiner.add(cond);
		}

		// Add the conditions to the query string.
		query.append(conditionJoiner.toString() + ";");

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
				// This one needs special care to make sql happy..!
				value = value.equals("yes") ? "true" :" no";
				stmt.setBoolean(index, Boolean.parseBoolean(value));
			} else {
				stmt.setString(index, value);
			}
			index++;
		}

		// Need to translate some of the strings to match the API exactly. THere's probably better ways to do this but it works...
		String result = Jsonizer.toJson(stmt.executeQuery(), "pallets");
		result = result.replace("false", "\"no\"")
			      	   .replace("true", "\"yes\"")
			  		   .replace("null", "\"null\"")
					   .replace("pallet_id", "id");

		return result;
	}

	/** Resets the database to match the default values. */
	public String reset(Request req, Response res) throws SQLException {
		String[] resetTables = {"Customers", "IngredientInRecipes", "Recipes", "Storage", "Pallets"};
		setForeignKeyCheck(false);

		for(String table : resetTables){
			try (Statement stmt = connection.createStatement()){

				// Truncate the table
				String sql = "TRUNCATE TABLE " + table;
				int result = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);


				if ("Customers".equals(table)){
					initCustomers();
				}
				else if ("IngredientInRecipes".equals(table)){
					initIngredientInRecipes();
				}
				else if ("Recipes".equals(table)){
					initRecipes();
				}
				else if ("Storage".equals(table)){
					initStorage();
				}
			}
		}

		setForeignKeyCheck(true);
		return "{\n\t\"status\": \"ok\"\n}";
	}

	/** Creates a new pallet with a given cookie. */
	public String createPallet(Request req, Response res) throws SQLException {
		String cookie = req.queryParams("cookie");
		int palletId = -1;
		int resultStatus = PALLET_OK;

		if (cookie == null) {
			resultStatus = ERROR;
		} else if (!cookieExists(cookie)) {
			resultStatus = UNKNOWN_COOKIE;
		}

		// Update storage information!
		this.connection.setAutoCommit(false);
		setSafeUpdate(false);

		// Check if we can create a new pallet!
		var ingredients= getRecipe(cookie).ingredients;

		String query =  "UPDATE storage\n" +
				"SET amount = amount - ?\n" +
				"WHERE ingredientName = ? AND " +
				"ingredientName IN \n" +
				"(\n" +
				"SELECT ingredientName\n" +
				"FROM ingredientinrecipes\n" +
				"WHERE cookie = ?\n" +
				");";


		boolean changeOk = true;
		for (Ingredient ingredient: ingredients) {
			PreparedStatement stmt = this.connection.prepareStatement(query);

			int palletAmount = 54 * ingredient.amount;

			stmt.setInt(1, palletAmount);
			stmt.setString(2, ingredient.name);
			stmt.setString(3, cookie);

			int result = stmt.executeUpdate();
			changeOk = result > 0;

			if (!changeOk)
				break;
		}

		// If there's enough ingredients in storage, commit the change!
		if (!changeOk)
			resultStatus = ERROR;
		else
			this.connection.commit();

		this.connection.setAutoCommit(true);

		if (resultStatus == PALLET_OK) {

			String[] columns = new String[] {"cookie", "production_date" , "blocked" , "location"};
			PreparedStatement stmt = makePreparedStatement("pallets", columns);

			if (stmt == null) {
				resultStatus = ERROR;
			} else {
				stmt.setString(1, cookie);
				stmt.setString(2, getCurrentDateTime());
				stmt.setBoolean(3, false);
				stmt.setString(4, DEFAULT_PALLET_LOCATION);

				resultStatus = stmt.executeUpdate();
				palletId = getLastPalletId();
			}
		}

		setSafeUpdate(true);

		if (resultStatus == PALLET_OK) {
			// Need to update Pallet 36 x 10 x 15 =

			return  "{\n\t\"status\": \"ok\" ," +
					"\n\t\"id\": " + palletId + "\n}";
		} else if (resultStatus == ERROR) {
			return "{\n\t\"status\": \"error\"\n}";
		} else {
			return "{\n\t\"status\": \"unknown cookie\"\n}";
		}

	}


	/* --- HELPER METHODS --- */

	private void initCustomers() throws SQLException {
		String data = readFile("customers.sql");
		this.connection.createStatement().execute(data);
	}

	private void initIngredientInRecipes() throws SQLException {
		String data = readFile("ingredients.sql");
		this.connection.createStatement().execute(data);
	}

	private void initRecipes() throws SQLException {
		String data = readFile("recipes.sql");
		this.connection.createStatement().execute(data);
	}

	private void initStorage() throws SQLException {
		String data = readFile("storage.sql");
		this.connection.createStatement().execute(data);
	}

	private void setForeignKeyCheck(boolean on) throws SQLException {
		connection.createStatement().executeQuery(
				"SET FOREIGN_KEY_CHECKS = " + (on ? "1" : "0") + ";"
		);
	}

	private void setSafeUpdate(boolean on) throws SQLException {
		connection.createStatement().executeQuery(
				"SET SQL_SAFE_UPDATES = " + (on ? "1" : "0") + ";"
		);
	}

	/** Convert raw string date to SQL string date object. */
	private java.sql.Date toSqlDate(String date) throws ParseException {
		Date parsed = DATE_FORMAT.parse(date);
		return new java.sql.Date(parsed.getTime());
	}

	/** Returns the id of the last-produced pallet. */
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

	/** Returns a recipe object from a recipe string. */
	private Recipe getRecipe(String recipe) {
		for (Recipe rec: recipes.recipes) {
			if (rec.name.equals(recipe)) {
				return rec;
			}
		}
		return null;
	}

	/** Returns the current time as a string, formatted to sql-friendly format. */
	private String getCurrentDateTime() {
		return DATE_TIME_FORMAT.format(LocalDateTime.now());
	}

	/** Returns true if a given cookie exists */
	private boolean cookieExists(String cookie) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT name FROM Recipes WHERE name = ?;");
		stmt.setString(1, cookie);
		var res = stmt.executeQuery();
		return res.next();
	}

	/** Creates a base PreparedStatement for insertions, given the table and the columns to insert into */
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

	/** Create a base select-query. */
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

	/** Reads a given file from disk and returns the content of the file as a string. */
	private String readFile(String file) {
		try {
			String path = "src/main/resources/" + file;
			return new String(Files.readAllBytes(Paths.get(path)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

}
