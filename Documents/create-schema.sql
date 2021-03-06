SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS Recipes;
DROP TABLE IF EXISTS IngredientInRecipes;
DROP TABLE IF EXISTS Storage;
DROP TABLE IF EXISTS StorageUpdates;
DROP TABLE IF EXISTS Pallets;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Customers;

CREATE TABLE Recipes (
    name VARCHAR(20),
    PRIMARY KEY(name)
);

CREATE TABLE Customers (
    customer_id INT AUTO_INCREMENT,
    name VARCHAR(20),
    address VARCHAR(20),
    PRIMARY KEY(customer_id)
);


CREATE TABLE IngredientInRecipes (
    cookie VARCHAR(20),
    ingredientName VARCHAR(30),
    quantity INT,
    unit VARCHAR(3),
    PRIMARY KEY(cookie, ingredientName),
    FOREIGN KEY(cookie) REFERENCES Recipes(name),
    FOREIGN KEY(ingredientName) REFERENCES Storage(ingredientName)
);

CREATE TABLE Storage (
    ingredientName VARCHAR(30),
    amount INT,
    unit VARCHAR(3),
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
    cookie VARCHAR(20),
    order_id INT,
    production_date DATETIME,
    deliveredDate DATETIME,
    blocked BOOLEAN,
    location VARCHAR(20),
    PRIMARY KEY(pallet_id),
    FOREIGN KEY(cookie) REFERENCES Recipes(name),
    FOREIGN KEY(order_id) REFERENCES Orders(order_id)
);

CREATE TABLE Orders (
    order_id INT AUTO_INCREMENT,
    customer_id INT,
    PRIMARY KEY(order_id),
    FOREIGN KEY(customer_id) REFERENCES Customers(customer_id)
);

SET SQL_SAFE_UPDATES = 1;
SET FOREIGN_KEY_CHECKS = 1;
