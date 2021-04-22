INSERT INTO Recipes(cookie) VALUES
('Nut ring'),
('Nut cookie'),
('Amneris'),
('Tango'),
('Almond delight'),
('Berliner')
;

INSERT INTO Storage(ingredientName, amount, unit) VALUES
('Bread crumbs', '500000', 'g'),
('Butter', '500000', 'g'),
('Chocolate', '500000', 'g'),
('Chopped almonds', '500000', 'g'),
('Cinnamon', '500000', 'g'),
('Egg whites', '500000', 'ml'),
('Eggs', '500000', 'g'),
('Fine-ground nuts', '500000', 'g'),
('Flour', '500000', 'g'),
('Ground, roasted nuts', '500000', 'g'),
('Icing sugar', '500000', 'g'),
('Marzipan', '500000', 'g'),
('Potato starch', '500000', 'g'),
('Roasted, chopped nuts', '500000', 'g'),
('Sodium bicarbonate', '500000', 'g'),
('Sugar', '500000', 'g'),
('Vanilla sugar', '500000', 'g'),
('Vanilla', '500000', 'g'),
('Wheat flour', '500000', 'g')
;

INSERT INTO IngredientInRecipes(cookie, ingredientName, Quantity, unit) VALUES
('Nut ring', 'Flour', '450', 'g'),
('Nut ring', 'Butter', '450', 'g'),
('Nut ring', 'Icing sugar', '190', 'g'),
('Nut ring', 'Roasted, chopped nuts', '225', 'g'),

('Nut cookie', 'Fine-ground nuts', '750', 'g'),
('Nut cookie', 'Ground, roasted nuts', '625', 'g'),
('Nut cookie', 'Bread crumbs', '125', 'g'),
('Nut cookie', 'Sugar', '375', 'g'),
('Nut cookie', 'Egg Whites', '350', 'ml'),
('Nut cookie', 'Chocolate', '50', 'g'),

('Amneris', 'Marzipan', '750', 'g'),
('Amneris', 'Butter', '250', 'g'),
('Amneris', 'Eggs', '250', 'g'),
('Amneris', 'Potato starch', '25', 'g'),
('Amneris', 'Wheat flour', '25', 'g'),

('Tango', 'Butter', '200', 'g'),
('Tango', 'Sugar', '250', 'g'),
('Tango', 'Flour', '300', 'g'),
('Tango', 'Sodium bicarbonate', '4', 'g'),
('Tango', 'Vanilla', '2', 'g'),

('Almond delight', 'Butter', '400', 'g'),
('Almond delight', 'Sugar', '270', 'g'),
('Almond delight', 'Chopped almonds', '279', 'g'),
('Almond delight', 'Flour', '400', 'g'),
('Almond delight', 'Cinnamon', '10', 'g'),

('Berliner', 'Flour', '350', 'g'),
('Berliner', 'Butter', '250', 'g'),
('Berliner', 'Icing sugar', '100', 'g'),
('Berliner', 'Eggs', '50', 'g'),
('Berliner', 'Vanilla sugar', '5', 'g'),
('Berliner', 'Chocolate', '50', 'g')
;

INSERT INTO Customers(name, address) VALUES
('Bjudkakor AB', 'Ystad'),
('Finkakor AB', 'Helsingborg'),
('Gästkakor AB', 'Hässleholm'),
('Kaffebröd AB', 'Landskrona'),
('Kalaskakor AB', 'Trelleborg'),
('Partykakor AB', 'Kristianstad'),
('Skånekakor AB', 'Perstorp'),
('Småbröd AB', 'Malmö')
;
