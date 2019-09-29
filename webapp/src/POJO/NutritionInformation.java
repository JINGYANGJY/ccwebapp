package POJO;

public class NutritionInformation {
    private int id;
    private int calories;
    private double cholesterolInMg;
    private int sodiumInMg;
    private double carbohydratesInGrams;
    private double proteinInGrams;

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getCholesterolInMg() {
        return cholesterolInMg;
    }

    public void setCholesterolInMg(double cholesterolInMg) {
        this.cholesterolInMg = cholesterolInMg;
    }

    public int getSodiumInMg() {
        return sodiumInMg;
    }

    public void setSodiumInMg(int sodiumInMg) {
        this.sodiumInMg = sodiumInMg;
    }

    public double getCarbohydratesInGrams() {
        return carbohydratesInGrams;
    }

    public void setCarbohydratesInGrams(double carbohydratesInGrams) {
        this.carbohydratesInGrams = carbohydratesInGrams;
    }

    public double getProteinInGrams() {
        return proteinInGrams;
    }

    public void setProteinInGrams(double proteinInGrams) {
        this.proteinInGrams = proteinInGrams;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
