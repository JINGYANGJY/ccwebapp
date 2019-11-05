package com.java.Controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java.DAO.*;
import com.java.POJO.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import static com.java.JavaApplication.statsDClient;

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
    @Autowired
    ImageRepository imageRepository;

    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    updateRecipe(@RequestHeader(value = "Authorization") String auth, @RequestBody ObjectNode objectNode, @PathVariable("id") String id) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.recipe.http.put");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];

        long findQueryStart = System.currentTimeMillis();
        User user = userRepository.findUserByEmail(email);
        recordTime("endpoint.recipe.http.put.query.findUserByEmail", findQueryStart);

        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            recordTime("endpoint.recipe.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        //find recipe which need to be updated
        long findRecipeStart = System.currentTimeMillis();
        Recipe recipe_updated = recipeRepository.findRecipeById(id);
        recordTime("endpoint.recipe.http.put.query.findRecipeById", findRecipeStart);

        if (recipe_updated == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + id + " does not exist");
            recordTime("endpoint.recipe.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        if (!user.getId().equals(recipe_updated.getAuthorId())) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "you're not authorized to update this recipe");
            recordTime("endpoint.recipe.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        String missing_field = checkRequiredInput(objectNode);
        if (missing_field != null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", missing_field + " is missing");
            recordTime("endpoint.recipe.http.put", startTime);
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
            recordTime("endpoint.recipe.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        int cook_time = objectNode.get("cook_time_in_min").asInt();
        int prep_time = objectNode.get("prep_time_in_min").asInt();
        if (cook_time % 5 != 0 || prep_time % 5 != 0) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "cook time and prep time should be multiple of 5");
            recordTime("endpoint.recipe.http.put", startTime);
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
            recordTime("endpoint.recipe.http.put", startTime);
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
            recordTime("endpoint.recipe.http.put", startTime);
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
                recordTime("endpoint.recipe.http.put", startTime);
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
                    recordTime("endpoint.recipe.http.put", startTime);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(object.toString());
                }
                if (position < 1) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "the value of position has to be larger than 1");
                    recordTime("endpoint.recipe.http.put", startTime);
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
                long deleteQueryStart = System.currentTimeMillis();
                orderedListRepository.delete(orderedLists1);
                recordTime("endpoint.recipe.http.put.query.delete.orderList", deleteQueryStart);
            }

            recipe_updated.setSteps(new_orderedLists);
            for (OrderedList order : new_orderedLists) {
                long saveOrderList = System.currentTimeMillis();
                orderedListRepository.save(order);
                recordTime("endpoint.recipe.http.put.query.save.orderList", saveOrderList);
            }

        } catch (Exception e) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "the format of steps is wrong, should be array");
            recordTime("endpoint.recipe.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        //NutritionInforamtion update
        long findNutrition = System.currentTimeMillis();
        NutritionInformation nuInfo = nutritionInformationRepository.findAllById(id);
        recordTime("endpoint.recipe.http.put.query.find.nutritionInformation", findNutrition);
        try {
            ObjectNode nutrition_information = objectNode.with("nutrition_information");
            String field = checkNutritionInput(nutrition_information);
            if (field != null) {
                JSONObject jObject = new JSONObject();
                jObject.put("message", field + " is missing");
                recordTime("endpoint.recipe.http.put", startTime);
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
                recordTime("endpoint.recipe.http.put", startTime);
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
            recordTime("endpoint.recipe.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        nuInfo.setId(id);
        long saveNu = System.currentTimeMillis();
        nutritionInformationRepository.save(nuInfo);
        recordTime("endpoint.recipe.http.put.query.save.nutritionInformation", saveNu);
        JSONObject jObject = recipeParser(recipe_updated);
        recordTime("endpoint.recipe.http.put", startTime);
        return ResponseEntity.status(HttpStatus.OK).
                body(jObject.toString());
    }

    @RequestMapping(value = "/v1/recipe/", method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String> createRecipe(@RequestHeader(value = "Authorization") String auth, @RequestBody ObjectNode objectNode) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.recipe.http.post");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        long findUserStart = System.currentTimeMillis();
        User user = userRepository.findUserByEmail(email);
        recordTime("endpoint.recipe.http.post.query.findUserByEmail", findUserStart);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            recordTime("endpoint.recipe.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        String missing_field = checkRequiredInput(objectNode);
        if (missing_field != null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", missing_field + " is missing");
            recordTime("endpoint.recipe.http.post", startTime);
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
            recordTime("endpoint.recipe.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        int cook_time = objectNode.get("cook_time_in_min").asInt();
        int prep_time = objectNode.get("prep_time_in_min").asInt();
        if (cook_time % 5 != 0 || prep_time % 5 != 0) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "cook time and prep time should be multiple of 5");
            recordTime("endpoint.recipe.http.post", startTime);
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
            recordTime("endpoint.recipe.http.post", startTime);
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
            recordTime("endpoint.recipe.http.post", startTime);
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
                recordTime("endpoint.recipe.http.post", startTime);
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
                    recordTime("endpoint.recipe.http.post", startTime);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(jObject.toString());
                }
                int position = jsonNode.get("position").asInt();
                if (position < 1) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "the value of position has to be larger than 1");
                    recordTime("endpoint.recipe.http.post", startTime);
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
            recordTime("endpoint.recipe.http.post", startTime);
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
                recordTime("endpoint.recipe.http.post", startTime);
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
                recordTime("endpoint.recipe.http.post", startTime);
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
            recordTime("endpoint.recipe.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        long saveRecipeStart = System.currentTimeMillis();
        recipeRepository.save(recipe);
        recordTime("endpoint.recipe.http.post.query.save.recipe", saveRecipeStart);
        for (OrderedList order : orderedLists) {
            long saveOrderList = System.currentTimeMillis();
            orderedListRepository.save(order);
            recordTime("endpoint.recipe.http.post.query.save.orderedList", saveOrderList);
        }
        nutritionInformation.setId(recipe.getId());
        long saveNutri = System.currentTimeMillis();
        nutritionInformationRepository.save(nutritionInformation);
        recordTime("endpoint.recipe.http.post.query.save.nutritionInformation", saveNutri);


        JSONObject jObject = recipeParser(recipe);

        recordTime("endpoint.recipe.http.post", startTime);
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
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.recipe.http.delete");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        String[] URI = request.getRequestURI().split("/");
        if (URI.length == 3) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "recipe id is required");
            recordTime("endpoint.recipe.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        long findRecipeStart = System.currentTimeMillis();
        Recipe recipe = recipeRepository.findRecipeById(URI[3]);
        recordTime("endpoint.recipe.http.delete.query.findRecipeById", findRecipeStart);
        long findUserStart = System.currentTimeMillis();
        User user = userRepository.findUserByEmail(email);
        recordTime("endpoint.recipe.http.delete.query.findUserByEmail", findUserStart);
        if (!Authentication(user, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            recordTime("endpoint.recipe.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        if (recipe == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipe with id " + URI[3] + " does not exist");
            recordTime("endpoint.recipe.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        if (!user.getId().equals(recipe.getAuthorId())) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "you're not authorized to delete this recipe");
            recordTime("endpoint.recipe.http.delete", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        long deleteRecipeStart = System.currentTimeMillis();
        recipeRepository.delete(recipe);
        nutritionInformationRepository.delete(nutritionInformationRepository.findAllById(recipe.getId()));
        recordTime("endpoint.recipe.http.delete.query.delete.recipe", deleteRecipeStart);
        if (recipe.getImage() != null) {
            long s3Start = System.currentTimeMillis();
            ImageController.deleteImage(recipe.getImage());
            recordTime("endpoint.recipe.http.delete.s3.delete", s3Start);
        }
        JSONObject jObject = new JSONObject();
        recordTime("endpoint.recipe.http.delete", startTime);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(jObject.toString());
    }

    //    //get Recipe information
    @RequestMapping(value = "/v1/recipe/{id}", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<String> getRecipe(@PathVariable("id") String id) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.recipe.http.get");

        long findRecipeStart = System.currentTimeMillis();
        Recipe recipe = recipeRepository.findRecipeById(id);
        recordTime("endpoint.recipe.http.get.query.findRecipeById", findRecipeStart);
        if (recipe != null) {
            JSONObject jObject = recipeParser(recipe);

            recordTime("endpoint.recipe.http.get", startTime);
            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        } else {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Unable to get recipe info with id " + id);
            recordTime("endpoint.recipe.http.get", startTime);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
    }

    // get the newest Recipe information
    @RequestMapping(value = "/v1/recipes", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<String> getNewestRecipe() {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.recipe.newest.http.get");

        long findRecipeStart = System.currentTimeMillis();
        Iterable<Recipe> recipeList = recipeRepository.findAll();
        recordTime("endpoint.recipe.newest.http.get.query.findAll", findRecipeStart);
        Iterator a = recipeList.iterator();
        Recipe recipe = new Recipe();
        while (a.hasNext()) {
            Recipe r = (Recipe) a.next();
            if (recipe.getCreatedTs() == null || r.getCreatedTs().compareTo(recipe.getCreatedTs()) > 0) {
                recipe = r;
            }
        }
        if (recipe == new Recipe()) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "No recipe available");
            recordTime("endpoint.recipe.newest.http.get", startTime);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).
                    body(jObject.toString());
        }
        JSONObject jObject = recipeParser(recipe);
        recordTime("endpoint.recipe.newest.http.get", startTime);
        return ResponseEntity.status(HttpStatus.OK).
                body(jObject.toString());
    }

    //
    private JSONObject recipeParser(Recipe recipe) {
        JSONObject jObject = new JSONObject();
        JSONObject imageInfo = new JSONObject();
        if (recipe.getImage() == null) {
            imageInfo.put("id", "");
            imageInfo.put("url", "");
        } else {
            imageInfo.put("id", recipe.getImage().getId());
            imageInfo.put("url", recipe.getImage().getUrl());
        }
        jObject.put("image", imageInfo);
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

    public void recordTime(String name, Long startTime) {
        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime(name, endTime - startTime);
    }
}
