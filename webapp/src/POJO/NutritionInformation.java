package POJO;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import javax.persistence.*;

@Entity

@Table( name ="nutritionInformation")
public class NutritionInformation {

    @Id
    @GenericGenerator(name = "generator", strategy = "foreign",
            parameters = @Parameter(name = "property", value = "recipie"))
    @Column(name ="id")
    private String id;
    @Column(name="calories")
    private int calories;
    @Column(name="cholesterol_in_mg")
    private double cholesterolInMg;
    @Column(name ="sodium_in_mg")
    private int sodiumInMg;
    @Column(name="carbohydrates_in_grams")
    private double carbohydratesInGrams;
    @Column(name ="protein_in_grams")
    private double proteinInGrams;

    @OneToOne
    @PrimaryKeyJoinColumn
    private  Recipie recipie;

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

    public String getId() {
        return id;
    }

    public Recipie getRecipie() {
        return recipie;
    }

    public void setRecipie(Recipie recipie) {
        this.recipie = recipie;
    }

    public void setId(String id) {
        this.id = id;
    }
}