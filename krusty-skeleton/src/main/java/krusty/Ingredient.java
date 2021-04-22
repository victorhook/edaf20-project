package krusty;

public class Ingredient {

    /**
     * Helper class to represent an ingredient.
     */

    public String name, unit;
    public int amount;

    public Ingredient(String name, int amount, String unit) {
        this.name = name;
        this.amount = amount;
        this.unit = unit;
    }
}
