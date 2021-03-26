package krusty;

public class Recipe {

    public String name;
    public Ingredient ingredients[];

    public Recipe(String name, Ingredient[] ingredients) {
        this.name = name;
        this.ingredients = ingredients;
    }
}
