DROP TABLE IF EXISTS Recipes;
DROP TABLE IF EXISTS Products;
DROP TABLE IF EXISTS IngredientInRecipes;
DROP TABLE IF EXISTS Storage;
DROP TABLE IF EXISTS StorageUpdates;
DROP TABLE IF EXISTS Pallets;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Customers;

CREATE TABLE Recipes (
cookieName VARCHAR(20),
PRIMARY KEY(cookieName)
);

CREATE TABLE Customers (
customer_id INT AUTO_INCREMENT,
name VARCHAR(20),
address VARCHAR(20),
PRIMARY KEY(customer_id)
);

CREATE TABLE Products (
product_id INT AUTO_INCREMENT,
cookieName VARCHAR(20),
producedTime DATETIME,
PRIMARY KEY(product_id),
FOREIGN KEY(cookieName) REFERENCES Recepies(cookieName)
);

CREATE TABLE IngredientInRecipes (
cookieName VARCHAR(20),
ingredientName VARCHAR(20),
quantity INT,
unit VARCHAR(3),
PRIMARY KEY(cookieName, ingredientName),
FOREIGN KEY(cookieName) REFERENCES Recipes(cookieName),
FOREIGN KEY(ingredientName) REFERENCES Storage(ingredientName)
);

CREATE TABLE Storage (
ingredientName VARCHAR(20),
amount INT,
PRIMARY KEY(ingredientName)
);

CREATE TABLE StorageUpdates (
storage_id INT AUTO_INCREMENT,
ingredientName VARCHAR(20),
amount INT,
updateTime DATETIME,
PRIMARY KEY(storage_id),
FOREIGN KEY(ingredientName) REFERENCES Storage(ingredientName)
);

CREATE TABLE Pallets (
pallet_id INT AUTO_INCREMENT,
product_id INT,
order_id INT,
creationDate DATE,
deliveredDate DATE,
isBlocked BOOLEAN,
location VARCHAR(20),
PRIMARY KEY(pallet_id),
FOREIGN KEY(product_id) REFERENCES Products(product_id),
FOREIGN KEY(order_id) REFERENCES Orders(order_id)
);

CREATE TABLE Orders (
order_id INT AUTO_INCREMENT,
customer_id INT,
PRIMARY KEY(order_id),
FOREIGN KEY(customer_id) REFERENCES Customers(customer_id)
);