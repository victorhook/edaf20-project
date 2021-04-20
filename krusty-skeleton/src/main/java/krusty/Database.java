package krusty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.JSONPObject;
import spark.Request;
import spark.Response;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.File;
import java.io.FileNotFoundException;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static krusty.Jsonizer.toJson;

public class Database {

	// MySQL db credentials
	private static final String dbUsername = "krustyadmin";
	private static final String dbPassword = "krustykaka123";
	private static final String database = "krustykookie";

	private static final String hostPort = "13337";
	private static final String hostIp = "83.250.66.137";

	// Need to add timezone info for connection.
	private static final String jdbcHost = "jdbc:mysql://" + hostIp + ":" + hostPort + "/" + database + "?serverTimezone=Europe/Stockholm";

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
			e.printStackTrace();
			System.out.println("ERROR: Failed to connect to database!");
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
				"LEFT JOIN orders USING (order_id)\n" +
				"LEFT JOIN customers USING (customer_id)\n"
		);

		// This joiner is used to bind together query conditions.
		StringJoiner conditionJoiner = new StringJoiner(" AND ");

		Map<String, String> conditions = new HashMap<>();
		var params = Map.of(
				"cookie", "cookieName = ?",
				"from", "creationDate > ?",
				"to", "creationDate < ?",
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
				stmt.setBoolean(index, Boolean.parseBoolean(value));
			} else {
				stmt.setString(index, value);
			}
			index++;
		}

		String result = Jsonizer.toJson(stmt.executeQuery(), "pallets");
		return result;
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

	public String reset(Request req, Response res) throws SQLException {
		String[] resetTables = {"Customers", "IngredientInRecipes", "Recipes", "Storage", "Pallets"};
		for(String table : resetTables){
			try (Statement stmt = connection.createStatement()){

				// Truncate the table
				setForeignKeyCheck(false);
				String sql = "TRUNCATE TABLE " + table;
				int result = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
				setForeignKeyCheck(true);


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
		return "status" + ": " + "ok";
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
		int palletId = -1;
		int resultStatus = PALLET_OK;

	 	if (cookieName == null) {
			resultStatus = ERROR;
		} else if (!cookieExists(cookieName)) {
			resultStatus = UNKNOWN_COOKIE;
		}

	 	// Update storage information!
		this.connection.setAutoCommit(false);
		setSafeUpdate(false);

	 	// Check if we can create a new pallet!
		var ingredients= getRecipe(cookieName).ingredients;

		String query =  "UPDATE storage\n" +
						"SET amount = amount - ?\n" +
						"WHERE ingredientName = ? AND " +
						"ingredientName IN \n" +
						"(\n" +
						"SELECT ingredientName\n" +
						"FROM ingredientinrecipes\n" +
						"WHERE cookieName = ?\n" +
						");";


		boolean changeOk = true;
		for (Ingredient ingredient: ingredients) {
			PreparedStatement stmt = this.connection.prepareStatement(query);
			stmt.setInt(1, ingredient.amount);
			stmt.setString(2, ingredient.name);
			stmt.setString(3, cookieName);

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

			String[] columns = new String[] {"cookieName", "creationDate" , "isBlocked" , "location"};
			PreparedStatement stmt = makePreparedStatement("pallets", columns);

			if (stmt == null) {
				resultStatus = ERROR;
			} else {
				stmt.setString(1, cookieName);
				stmt.setDate(2, getTodaysDate());
				stmt.setBoolean(3, false);
				stmt.setString(4, DEFAULT_PALLET_LOCATION);

				resultStatus = stmt.executeUpdate();
				palletId = getLastPalletId();
			}
		}

		setSafeUpdate(true);


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

	/** Helper method to check if a cookie exists */
	private boolean cookieExists(String cookieName) throws SQLException {
		PreparedStatement stmt = connection.prepareStatement("SELECT cookieName FROM Recipes WHERE cookieName = ?;");
		stmt.setString(1, cookieName);
		var res = stmt.executeQuery();
		return res.next();
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

	private void initCustomers(){
		String data = readFile("customers.sql");
		Statement stmt = null;
		try {
			stmt = this.connection.createStatement();
			stmt.execute(data);
		} catch (SQLException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	private void initIngredientInRecipes(){
		String data = readFile("ingredients.sql");
		Statement stmt = null;
		try {
			stmt = this.connection.createStatement();
			stmt.execute(data);
		} catch (SQLException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	private void initRecipes(){
		String data = readFile("recipes.sql");
		Statement stmt = null;
		try {
			stmt = this.connection.createStatement();
			stmt.execute(data);
		} catch (SQLException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

	private void initStorage(){
		String data = readFile("storage.sql");
		Statement stmt = null;
		try {
			stmt = this.connection.createStatement();
			stmt.execute(data);
		} catch (SQLException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

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
