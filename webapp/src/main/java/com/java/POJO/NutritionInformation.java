package com.java.POJO;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

@Entity

@Table( name ="nutritionInformation")
public class NutritionInformation {

    @Id
    @Column(name="id")
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

    public NutritionInformation() {
    }

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

    public void setId(String id) {
        this.id = id;
    }
}