package krusty;

import java.util.Arrays;
import java.util.List;

public class DefaultRecipes {

    List<Recipe> recipes;

    public DefaultRecipes() {
        this.recipes = Arrays.asList(
            new Recipe( "Nut ring",
                new Ingredient[] {
                      new Ingredient("Flour", 450, "g"),
                      new Ingredient("Butter", 450, "g"),
                      new Ingredient("Icing sugar", 190, "g"),
                      new Ingredient("Roasted, chopped nuts", 225, "g")
                }
            ),
            new Recipe("Nut cookie",
                new Ingredient[] {
                        new Ingredient("Fine-ground nuts", 750, "g"),
                        new Ingredient("Ground, roasted nuts", 625, "g"),
                        new Ingredient("Bread crumbs", 125, "g"),
                        new Ingredient("Sugar", 375, "g"),
                        new Ingredient("Egg Whites", 350, "ml"),
                        new Ingredient("Chocolate", 50, "g")
                }
            ),
            new Recipe("Amneris",
                new Ingredient[]{
                        new Ingredient("Marzipan", 750, "g"),
                        new Ingredient("Butter", 250, "g"),
                        new Ingredient("Eggs", 250, "g"),
                        new Ingredient("Potato starch", 25, "g"),
                        new Ingredient("Wheat flour", 25, "g")
                }
            ),
            new Recipe("Tango",
                new Ingredient[] {
                        new Ingredient("Butter", 200, "g"),
                        new Ingredient("Sugar", 250, "g"),
                        new Ingredient("Flour", 300, "g"),
                        new Ingredient("Sodium bicarbonate", 4, "g"),
                        new Ingredient("Vanilla", 2, "g")
                }
            ),
            new Recipe("Almond delight",
                new Ingredient[]{
                        new Ingredient("Butter", 400, "g"),
                        new Ingredient("Sugar", 270, "g"),
                        new Ingredient("Chopped almonds", 279, "g"),
                        new Ingredient("Flour", 400, "g"),
                        new Ingredient("Cinnamon", 10, "g")
                }
            ),
            new Recipe("Berliner",
                new Ingredient[] {
                        new Ingredient("Flour", 350, "g"),
                        new Ingredient("Butter", 250, "g"),
                        new Ingredient("Icing sugar", 100, "g"),
                        new Ingredient("Eggs", 50, "g"),
                        new Ingredient("Vanilla sugar", 5, "g"),
                        new Ingredient("Chocolate", 50, "g")
                }
            )
        );
    }
}
