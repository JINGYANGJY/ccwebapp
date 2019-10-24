package com.java.DAO;

import com.java.POJO.Recipe;
import org.springframework.data.repository.CrudRepository;

public interface RecipeRepository extends CrudRepository<Recipe,String> {
     Recipe findRecipeById(String id);
}
