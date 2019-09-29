package POJO;

import java.util.List;

public class Recipie {
    private String id;
    private String createdTs;
    private String updatedTs;
    private String authorId;
    private int cookTimeInMin;
    private int prepTimeInMin;
    private int totalTimeInMin;
    private String title;
    private String cusine;
    private int servings;
    private List<String> ingredients;
    private List<OrderedList> steps;
    private NutritionInformation nutritionInformation;

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
