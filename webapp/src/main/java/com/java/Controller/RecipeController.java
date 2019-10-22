package com.java.Controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java.DAO.NutritionInformationRepository;
import com.java.DAO.OrderedListRepository;
import com.java.DAO.RecipeRepository;
import com.java.DAO.UserRepository;
import com.java.POJO.NutritionInformation;
import com.java.POJO.OrderedList;
import com.java.POJO.Recipe;
import com.java.POJO.User;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class RecipeController {
    @Autowired
    UserRepository userRepository;
    @Autowired
    RecipeRepository recipeRepository;
    @Autowired
    NutritionInformationRepository nutritionInformationRepository;
    @Autowired
    OrderedListRepository orderedListRepository;


    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    updateRecipe(@RequestHeader(value = "Authorization") String auth, @RequestBody ObjectNode objectNode, @PathVariable("id") String id) {
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        User user = userRepository.findUserByEmail(email);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        //find recipe which need to be updated
        Recipe recipe_updated = recipeRepository.findRecipeById(id);

        if (recipe_updated == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + id + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        if (!user.getId().equals(recipe_updated.getAuthorId())) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "you're not authorized to update this recipe");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        String missing_field = checkRequiredInput(objectNode);
        if (missing_field != null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", missing_field + " is missing");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        boolean isCookMatch = inputIntegerCheck(objectNode.get("cook_time_in_min").asText());
        boolean isPrepMatch = inputIntegerCheck(objectNode.get("prep_time_in_min").asText());
        boolean isServingsMatch = inputIntegerCheck(objectNode.get("servings").asText());
        if (!(isCookMatch && isPrepMatch && isServingsMatch)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the format of cook time, prep time or servings is wrong, should be integer");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        int cook_time = objectNode.get("cook_time_in_min").asInt();
        int prep_time = objectNode.get("prep_time_in_min").asInt();
        if (cook_time % 5 != 0 || prep_time % 5 != 0) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "cook time and prep time should be multiple of 5");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        recipe_updated.setCookTimeInMin(cook_time);
        recipe_updated.setPrepTimeInMin(prep_time);
        recipe_updated.setTotalTimeInMin(cook_time + prep_time);

        recipe_updated.setTitle(objectNode.get("title").asText());
        recipe_updated.setCusine(objectNode.get("cusine").asText());

        int servings = objectNode.get("servings").asInt();
        if (servings < 1 || servings > 5) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the value of servings has to be in the range of [1,5]");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        recipe_updated.setServings(servings);

        //set updated Time
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipe_updated.setUpdatedTs(ft.format(dNow));

        //update_ingredients
        List<String> ingredients = new ArrayList<>();
        JsonNode str = objectNode.get("ingredients");
        if (!str.isArray() || str.size() == 0) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the value of ingredients should be an array with length > 0");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        for (JsonNode ingredient : str) {
            String str_ingredient = ingredient.toString();
            ingredients.add(str_ingredient.substring(1, str_ingredient.length() - 1));
        }
        recipe_updated.setIngredients(ingredients);

        //orderedList
        List<OrderedList> orderedLists = recipe_updated.getSteps();
        try {
            ArrayNode arrayNode = objectNode.withArray("steps");
            if (arrayNode.size() == 0) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", "the steps should not be null");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jObject.toString());
            }
            List<OrderedList> new_orderedLists = new LinkedList<>();
            for (JsonNode node : arrayNode) {
                OrderedList order = new OrderedList();
                int position = node.get("position").asInt();
                boolean isPositionMatch = inputIntegerCheck(node.get("position").asText());
                if (!isPositionMatch) {
                    JSONObject object = new JSONObject();
                    object.put("message", "the format of position is wrong, should be integer");
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(object.toString());
                }
                if (position < 1) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "the value of position has to be larger than 1");
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(jObject.toString());
                }
                order.setPosition(position);
                order.setItems(node.get("items").asText());
                order.setRecipe(recipe_updated);
                new_orderedLists.add(order);
            }

            for (OrderedList orderedLists1 : recipe_updated.getSteps()) {
                orderedListRepository.delete(orderedLists1);
            }


            recipe_updated.setSteps(new_orderedLists);
            for (OrderedList order : new_orderedLists) {
                orderedListRepository.save(order);
            }

        } catch (Exception e) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the format of steps is wrong, should be array");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        //NutritionInforamtion update
        NutritionInformation nuInfo = nutritionInformationRepository.findAllById(id);
        try {
            ObjectNode nutrition_information = objectNode.with("nutrition_information");
            String field = checkNutritionInput(nutrition_information);
            if (field != null) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", field + " is missing");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jObject.toString());
            }

            // input check
            boolean check = inputIntegerCheck(nutrition_information.get("calories").asText()) &&
                    inputFloatCheck(nutrition_information.get("carbohydrates_in_grams").asText()) &&
                    inputFloatCheck(nutrition_information.get("cholesterol_in_mg").asText()) &&
                    inputIntegerCheck(nutrition_information.get("sodium_in_mg").asText()) &&
                    inputFloatCheck(nutrition_information.get("protein_in_grams").asText());
            if (!check) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", "the format of nutrition information is wrong");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jObject.toString());
            }

            nuInfo.setCalories(nutrition_information.get("calories").asInt());
            nuInfo.setCarbohydratesInGrams(nutrition_information.get("carbohydrates_in_grams").asDouble());
            nuInfo.setCholesterolInMg(nutrition_information.get("cholesterol_in_mg").asDouble());
            nuInfo.setSodiumInMg(nutrition_information.get("sodium_in_mg").asInt());
            nuInfo.setProteinInGrams(nutrition_information.get("protein_in_grams").asDouble());
        } catch (Exception e) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the format of nutrition information should be a pojo");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        nuInfo.setId(id);
        nutritionInformationRepository.save(nuInfo);
        JSONObject jObject = recipeParser(recipe_updated);
        return ResponseEntity.status(HttpStatus.OK).
                body(jObject.toString());
    }

    @RequestMapping(value = "/v1/recipe/", method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String> createRecipe(@RequestHeader(value = "Authorization") String auth, @RequestBody ObjectNode objectNode) {

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        User user = userRepository.findUserByEmail(email);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        String missing_field = checkRequiredInput(objectNode);
        if (missing_field != null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", missing_field + " is missing");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        Recipe recipe = new Recipe();

        boolean isCookMatch = inputIntegerCheck(objectNode.get("cook_time_in_min").asText());
        boolean isPrepMatch = inputIntegerCheck(objectNode.get("prep_time_in_min").asText());
        boolean isServingsMatch = inputIntegerCheck(objectNode.get("servings").asText());
        if (!(isCookMatch && isPrepMatch && isServingsMatch)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the format of cook time, prep time or servings is wrong, should be integer");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        int cook_time = objectNode.get("cook_time_in_min").asInt();
        int prep_time = objectNode.get("prep_time_in_min").asInt();
        if (cook_time % 5 != 0 || prep_time % 5 != 0) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "cook time and prep time should be multiple of 5");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        recipe.setCookTimeInMin(cook_time);
        recipe.setPrepTimeInMin(prep_time);
        recipe.setTotalTimeInMin(cook_time + prep_time);

        recipe.setTitle(objectNode.get("title").asText());
        recipe.setCusine(objectNode.get("cusine").asText());

        int servings = objectNode.get("servings").asInt();
        if (servings < 1 || servings > 5) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the value of servings has to be in the range of [1,5]");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        recipe.setServings(servings);

        recipe.setId(UUID.randomUUID().toString());
        //set Author id
        String userId = user.getId();
        recipe.setAuthorId(userId);

        //set ingredients
        List<String> ingredientsList = new ArrayList<>();
        JsonNode str = objectNode.get("ingredients");
        if (!str.isArray() || str.size() == 0) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the value of ingredients should be an array with length > 0");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        for (JsonNode ingredient : str) {
            String str_ingredient = ingredient.toString();
            ingredientsList.add(str_ingredient.substring(1, str_ingredient.length() - 1));
        }
        recipe.setIngredients(ingredientsList);

        //set Time
        Date dNow = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipe.setCreatedTs(ft.format(dNow));
        recipe.setUpdatedTs(ft.format(dNow));

        // set Steps
        List<OrderedList> orderedLists = new LinkedList<>();
        try {
            ArrayNode arrayNode = objectNode.withArray("steps");
            if (arrayNode.size() == 0) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", "the steps should not be null");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jObject.toString());
            }
            for (JsonNode jsonNode : arrayNode) {
                OrderedList orderedList = new OrderedList();
                boolean isPositionMatch = inputIntegerCheck(jsonNode.get("position").asText());
                if (!isPositionMatch) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "the format of position is wrong, should be integer");
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(jObject.toString());
                }
                int position = jsonNode.get("position").asInt();
                if (position < 1) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "the value of position has to be larger than 1");
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(jObject.toString());
                }
                orderedList.setPosition(position);
                orderedList.setItems(jsonNode.get("items").asText());
                orderedList.setRecipe(recipe);
                orderedLists.add(orderedList);
            }
        } catch (Exception e) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the format of steps is wrong, should be array");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        recipe.setSteps(orderedLists);

        //setNutrition
        NutritionInformation nutritionInformation = new NutritionInformation();
        try {
            ObjectNode nutritionInformationObjectNode = objectNode.with("nutrition_information");
            String field = checkNutritionInput(nutritionInformationObjectNode);
            if (field != null) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", field + " is missing");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jObject.toString());
            }

            // input check for nutrition info
            boolean check = inputIntegerCheck(nutritionInformationObjectNode.get("calories").asText()) &&
                    inputFloatCheck(nutritionInformationObjectNode.get("carbohydrates_in_grams").asText()) &&
                    inputFloatCheck(nutritionInformationObjectNode.get("cholesterol_in_mg").asText()) &&
                    inputIntegerCheck(nutritionInformationObjectNode.get("sodium_in_mg").asText()) &&
                    inputFloatCheck(nutritionInformationObjectNode.get("protein_in_grams").asText());
            if (!check) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", "the format of nutrition information is wrong");
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(jObject.toString());
            }
            nutritionInformation.setCalories(nutritionInformationObjectNode.get("calories").asInt());
            nutritionInformation.setCholesterolInMg(nutritionInformationObjectNode.get("cholesterol_in_mg").asInt());
            nutritionInformation.setSodiumInMg(nutritionInformationObjectNode.get("sodium_in_mg").asInt());
            nutritionInformation.setCarbohydratesInGrams(nutritionInformationObjectNode.get("carbohydrates_in_grams").asDouble());
            nutritionInformation.setProteinInGrams(nutritionInformationObjectNode.get("protein_in_grams").asDouble());
        } catch (Exception e) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", e + "the format of nutrition information should be a pojo");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        recipeRepository.save(recipe);
        for (OrderedList order : orderedLists) {
            orderedListRepository.save(order);
        }
        nutritionInformation.setId(recipe.getId());
        nutritionInformationRepository.save(nutritionInformation);


        JSONObject jObject = recipeParser(recipe);

        return ResponseEntity.status(HttpStatus.CREATED).
                body(jObject.toString());
    }

    public boolean Authentication(User user, String password) {
        if (user == null) return false;
        String stored_hash = user.getPassword();
        if (BCrypt.checkpw(password, stored_hash)) {
            return true;
        } else {
            return false;
        }
    }

    @RequestMapping(value = "/v1/recipe/*", method = RequestMethod.DELETE)
    public @ResponseBody
    ResponseEntity<String> deleteRecipe(@RequestHeader(value = "Authorization") String auth, HttpServletRequest request) {
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        String[] URI = request.getRequestURI().split("/");
        if (URI.length == 3) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "recipe id is required");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        Recipe recipe = recipeRepository.findRecipeById(URI[3]);
        User user = userRepository.findUserByEmail(email);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + URI[4] + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        if (!user.getId().equals(recipe.getAuthorId())) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "you're not authorized to delete this recipe");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        recipeRepository.delete(recipe);
        nutritionInformationRepository.delete(nutritionInformationRepository.findAllById(recipe.getId()));
        JSONObject jObject = new JSONObject();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(jObject.toString());
    }

    //    //get Recipe information
    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<String> getRecipe(@PathVariable("id") String id) {
        Recipe recipe = recipeRepository.findRecipeById(id);
        if (recipe != null) {
            JSONObject jObject = recipeParser(recipe);

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        } else {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Unable to get recipe info with id " + id);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
    }

    //
    private JSONObject recipeParser(Recipe recipe) {
        JSONObject jObject = new JSONObject();
        jObject.put("id", recipe.getId());
        jObject.put("created_ts", recipe.getCreatedTs());
        jObject.put("updated_ts", recipe.getUpdatedTs());
        jObject.put("author_id", recipe.getAuthorId());
        jObject.put("cook_time_in_min", recipe.getCookTimeInMin());
        jObject.put("prep_time_in_min", recipe.getPrepTimeInMin());
        jObject.put("total_time_in_min", recipe.getTotalTimeInMin());
        jObject.put("title", recipe.getTitle());
        jObject.put("cusine", recipe.getCusine());
        jObject.put("servings", recipe.getServings());
        jObject.put("ingredients", new JSONArray(recipe.getIngredients()));

        JSONArray steps = new JSONArray();
        for (OrderedList ol : recipeRepository.findRecipeById(recipe.getId()).getSteps()) {
            JSONObject orderedList = new JSONObject();
            orderedList.put("position", ol.getPosition());
            orderedList.put("items", ol.getItems());
            steps.put(orderedList);
        }
        jObject.put("steps", steps);

        JSONObject nuinfo = new JSONObject();
        NutritionInformation n = nutritionInformationRepository.findAllById(recipe.getId());
        nuinfo.put("calories", n.getCalories());
        nuinfo.put("cholesterol_in_mg", n.getCholesterolInMg());
        nuinfo.put("sodium_in_mg", n.getSodiumInMg());
        nuinfo.put("carbohydrates_in_grams", n.getCarbohydratesInGrams());
        nuinfo.put("protein_in_grams", n.getProteinInGrams());
        jObject.put("nutrition_information", nuinfo);

        return jObject;
    }

    public boolean inputIntegerCheck(String input) {
        String intPattern = "^[1-9]\\d*$";
        return Pattern.matches(intPattern, input);
    }

    public boolean inputFloatCheck(String input) {
        String floatPattern = "^([1-9]*[1-9][0-9]*(\\.[0-9]+)?|[0]+\\.[0-9]*[1-9][0-9]*)$";
        return Pattern.matches(floatPattern, input);
    }

    private String checkRequiredInput(ObjectNode objectNode) {
        Iterator<String> fieldNames = objectNode.fieldNames();
        List<String> requiredFields = new ArrayList<String>();
        List<String> inputFields = new ArrayList<String>();
        requiredFields.add("cook_time_in_min");
        requiredFields.add("prep_time_in_min");
        requiredFields.add("title");
        requiredFields.add("cusine");
        requiredFields.add("servings");
        requiredFields.add("ingredients");
        requiredFields.add("steps");
        requiredFields.add("nutrition_information");

        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            inputFields.add(field);
        }
        for (String field : requiredFields) {
            if (!inputFields.contains(field)) {
                return field;
            }
        }
        return null;
    }

    private String checkNutritionInput(ObjectNode objectNode) {
        Iterator<String> fieldNames = objectNode.fieldNames();
        List<String> requiredFields = new ArrayList<String>();
        List<String> inputFields = new ArrayList<String>();
        requiredFields.add("calories");
        requiredFields.add("cholesterol_in_mg");
        requiredFields.add("sodium_in_mg");
        requiredFields.add("carbohydrates_in_grams");
        requiredFields.add("protein_in_grams");

        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            inputFields.add(field);
        }
        for (String field : requiredFields) {
            if (!inputFields.contains(field)) {
                return field;
            }
        }
        return null;
    }
}
