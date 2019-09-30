package POJO;

import java.util.ArrayList;
import java.util.List;


import org.hibernate.annotations.GenericGenerator;
import org.springframework.stereotype.Component;

import javax.persistence.*;

@Entity
@Table(name="recipie")
public class Recipie {


    @Id
    @GeneratedValue(generator = "generator")
    @GenericGenerator(name="generator",strategy = "uuid2")
    @Column(name="id")
    private String id;
    @Column (name = "created_ts")
    private String createdTs;
    @Column (name = "updated_ts")
    private String updatedTs;

    @Column (name = "author_id")
    private String authorId;

    @Column (name = "cook_time_in_min")
    private int cookTimeInMin;
    @Column (name ="prep_time_in_min")
    private int prepTimeInMin;
    @Column (name ="total_time_in_min")
    private int totalTimeInMin;
    @Column (name ="title")
    private String title;
    @Column (name = "cusine")
    private String cusine;
    @Column (name = "servings")
    private int servings;

    @ElementCollection
    @CollectionTable(name="ingredients", joinColumns=@JoinColumn(name="id"))
    @Column(name="ingredient")
    private List<String> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipie")
    private List<OrderedList> steps = new ArrayList<>();

    @OneToOne(mappedBy = "recipie")
    private NutritionInformation nutritionInformation;



    public Recipie() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatedTs() {
        return createdTs;
    }

    public void setCreatedTs(String createdTs) {
        this.createdTs = createdTs;
    }

    public String getUpdatedTs() {
        return updatedTs;
    }

    public void setUpdatedTs(String updatedTs) {
        this.updatedTs = updatedTs;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public int getCookTimeInMin() {
        return cookTimeInMin;
    }

    public void setCookTimeInMin(int cookTimeInMin) {
        this.cookTimeInMin = cookTimeInMin;
    }

    public int getPrepTimeInMin() {
        return prepTimeInMin;
    }

    public void setPrepTimeInMin(int prepTimeInMin) {
        this.prepTimeInMin = prepTimeInMin;
    }

    public int getTotalTimeInMin() {
        return totalTimeInMin;
    }

    public void setTotalTimeInMin(int totalTimeInMin) {
        this.totalTimeInMin = totalTimeInMin;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCusine() {
        return cusine;
    }

    public void setCusine(String cusine) {
        this.cusine = cusine;
    }

    public int getServings() {
        return servings;
    }

    public void setServings(int servings) {
        this.servings = servings;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public List<OrderedList> getSteps() {
        return steps;
    }

    public void setSteps(List<OrderedList> steps) {
        this.steps = steps;
    }

    public NutritionInformation getNutritionInformation() {
        return nutritionInformation;
    }

    public void setNutritionInformation(NutritionInformation nutritionInformation) {
        this.nutritionInformation = nutritionInformation;
    }
}
