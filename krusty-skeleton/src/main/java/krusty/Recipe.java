package krusty;

public class Recipe {

    /**
     * This is a helper class to make insertions and updates to the db easier.
     * Each recipe consists of several ingredients.
     */


    public String name;
    public Ingredient ingredients[];

    public Recipe(String name, Ingredient[] ingredients) {
        this.name = name;
        this.ingredients = ingredients;
    }
}
