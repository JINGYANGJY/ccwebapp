package Controller;

import DAO.NutritionInformationDao;
import DAO.OrderedListDao;
import DAO.RecipieDao;
import DAO.UserDao;
import POJO.NutritionInformation;
import POJO.OrderedList;
import POJO.Recipie;
import POJO.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Controller
public class RecipieController {
    // TODO: 1. while creating or updating a recipie, remember to
    //  check ingredients items are unique or not;
    //  2. while creating or updating a recipie, servings should be
    //  in [1,5] range.

    @Autowired
    UserDao userDao;
    @Autowired
    NutritionInformationDao nutritionInformationDao;
    @Autowired
    OrderedListDao orderedListDao;
    @Autowired
    RecipieDao recipieDao;

    @RequestMapping(value="/v1/recipie/{id}",method = RequestMethod.PUT, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    createAccount(@RequestHeader(value="Authorization") String auth, @RequestBody ObjectNode objectNode, @PathVariable("id") String id){
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        User user = userDao.getUserInfo(email);
        //find recipie which need to be updated
        Recipie recipie_updated = recipieDao.getRecipieInfo(id);
        if(!Authentication(email,password) || !user.getId().equals(recipie_updated.getAuthorId())){
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(" Only Author can update recipie Information");
        } else if(recipie_updated == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND).body(null);
        }

        recipie_updated.setCookTimeInMin(objectNode.get("cook_time_in_min").asInt());
        recipie_updated.setPrepTimeInMin(objectNode.get("prep_time_in_min").asInt());
        recipie_updated.setTitle(objectNode.get("title").asText());
        recipie_updated.setCusine(objectNode.get("cusine").asText());
        recipie_updated.setServings(objectNode.get("servings").asInt());
        //set updated Time
        Date dNow = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipie_updated.setUpdatedTs(ft.format(dNow));

        //update_ingredients
        List<String> ingredients = new ArrayList<>();
        JsonNode str = objectNode.get("ingredients");
        for(JsonNode ingredient:str){
            String str_ingredient = ingredient.toString();
            ingredients.add(str_ingredient.substring(1,str_ingredient.length()-1));
        }
        recipie_updated.setIngredients(ingredients);

        //orderedList
        List<OrderedList> orderedLists = orderedListDao.getOrderedList(id);
        ArrayNode arrayNode = objectNode.withArray("steps");
        List<OrderedList> new_orderedLists = new LinkedList<>();
        //updated former_orderList.size() >= updated_orderList.size()
        if (orderedLists.size()>=arrayNode.size()) {
            int i=0;
            while ( i < orderedLists.size()) {
                if(i<arrayNode.size()){
                    JsonNode jsonNode = arrayNode.get(i);
                    orderedLists.get(i).setPosition(jsonNode.get("position").asInt());
                    orderedLists.get(i).setItems(jsonNode.get("items").asText());
                    orderedListDao.update(orderedLists.get(i));
                }else {
                    orderedListDao.delete(orderedLists.get(i));
                }
                i++;
            }
        } else {
            int i =0;
            while (i <arrayNode.size()){
                if (i<orderedLists.size()) {
                    JsonNode jsonNode = arrayNode.get(i);
                    orderedLists.get(i).setPosition(jsonNode.get("position").asInt());
                    orderedLists.get(i).setItems(jsonNode.get("items").asText());
                    orderedListDao.update(orderedLists.get(i));
                } else {
                    OrderedList orderedList = new OrderedList();
                    orderedList.setPosition(arrayNode.get(i).get("position").asInt());
                    orderedList.setItems(arrayNode.get(i).get("items").asText());
                    orderedList.setRecipie(recipie_updated);
                    new_orderedLists.add(orderedList);
                }
                i++;
            }
            if(new_orderedLists.size()>0){
                recipie_updated.setSteps(new_orderedLists);
            }
        }
        recipieDao.update(recipie_updated);
        if (new_orderedLists.size()>0){
            for(OrderedList ol:new_orderedLists){
                ol.setRecipie(recipie_updated);
                orderedListDao.save(ol);
            }
        }
        //NutritionInforamtion update
        NutritionInformation nuInfo = nutritionInformationDao.get(id);
        JsonNode nutrition_information= objectNode.get("nutrition_information");
        nuInfo.setCalories(nutrition_information.get("calories").asInt());
        nuInfo.setCarbohydratesInGrams(nutrition_information.get("carbohydrates_in_grams").asInt());
        nuInfo.setCholesterolInMg(nutrition_information.get("cholesterol_in_mg").asInt());
        nuInfo.setSodiumInMg(nutrition_information.get("sodium_in_mg").asInt());
        nuInfo.setProteinInGrams(nutrition_information.get("protein_in_grams").asInt());

        recipie_updated.setNutritionInformation(nuInfo);
        nutritionInformationDao.update(nuInfo);

        return ResponseEntity.status(HttpStatus.OK).
                body(id);
    }

    @RequestMapping(value = "/v1/recipie/",method = RequestMethod.POST,consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String> createRecipie(@RequestHeader(value="Authorization") String auth ,@RequestBody ObjectNode objectNode){

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];

        if(!Authentication(email,password)){
            JSONObject jObject = new JSONObject();
            jObject.put("message", " email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }

        Recipie recipie = new Recipie();
        recipie.setCookTimeInMin(objectNode.get("cook_time_in_min").asInt());
        recipie.setPrepTimeInMin(objectNode.get("prep_time_in_min").asInt());
        //TODO: total
        recipie.setTotalTimeInMin(objectNode.get("total_time_in_min").asInt());
        recipie.setTitle(objectNode.get("title").asText());
        recipie.setCusine(objectNode.get("cusine").asText());
        recipie.setServings(objectNode.get("servings").asInt());
        recipie.setId(UUID.randomUUID().toString());
        //set Author id
        String userId = userDao.getUserInfo(email).getId();
        recipie.setAuthorId(userId);

        //set ingredients
        List<String> ingredientsList = new ArrayList<>();
        JsonNode str = objectNode.get("ingredients");
        for(JsonNode ingredient : str){
            String str_ingredient = ingredient.toString();
            ingredientsList.add(str_ingredient.substring(1, str_ingredient.length() - 1));
        }
        recipie.setIngredients(ingredientsList);

        //set Time
        Date dNow = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipie.setCreatedTs(ft.format(dNow));
        recipie.setUpdatedTs(ft.format(dNow));

        // set Steps
        List<OrderedList> orderedLists = new LinkedList<>();
        ArrayNode arrayNode = objectNode.withArray("steps");

        for(JsonNode jsonNode : arrayNode){
            OrderedList orderedList = new OrderedList();
            orderedList.setPosition(jsonNode.get("position").asInt());
            orderedList.setItems(jsonNode.get("items").asText());
            orderedList.setRecipie(recipie);
            orderedLists.add(orderedList);
        }
        recipie.setSteps(orderedLists);

        //setNutrition
        NutritionInformation nutritionInformation = new NutritionInformation();
        ObjectNode nutritionInformationObjectNode = objectNode.with("nutrition_information");
        nutritionInformation.setCalories(nutritionInformationObjectNode.get("calories").asInt());
        nutritionInformation.setCholesterolInMg(nutritionInformationObjectNode.get("cholesterol_in_mg").asInt());
        nutritionInformation.setSodiumInMg(nutritionInformationObjectNode.get("sodium_in_mg").asInt());
        nutritionInformation.setCarbohydratesInGrams(nutritionInformationObjectNode.get("carbohydrates_in_grams").asDouble());
        nutritionInformation.setProteinInGrams(nutritionInformationObjectNode.get("protein_in_grams").asDouble());
        nutritionInformation.setRecipie(recipie);

        recipie.setNutritionInformation(nutritionInformation);
        recipieDao.save(recipie);
        for(OrderedList ol : orderedLists){
            orderedListDao.save(ol);
        }
        nutritionInformationDao.save(nutritionInformation);

        JSONObject jObject = new JSONObject();
        jObject.put("id", recipie.getId());
        jObject.put("created_ts", recipie.getCreatedTs());
        jObject.put("updated_ts", recipie.getUpdatedTs());
        jObject.put("author_id", recipie.getAuthorId());
        jObject.put("cook_time_in_min", recipie.getCookTimeInMin());
        jObject.put("prep_time_in_min", recipie.getPrepTimeInMin());
        jObject.put("total_time_in_min", recipie.getTotalTimeInMin());
        jObject.put("title", recipie.getTitle());
        jObject.put("cusine", recipie.getCusine());
        jObject.put("servings", recipie.getServings());
        jObject.put("ingredients", objectNode.get("ingredients"));
        jObject.put("steps", objectNode.get("steps"));
        jObject.put("nutrition_information", objectNode.get("nutrition_information"));

        return ResponseEntity.status(HttpStatus.CREATED).
                body(jObject.toString());
    }

    public boolean Authentication(String userName, String password) {
        if(userDao.getUserInfo(userName)==null) return false;
        String stored_hash = userDao.getUserInfo(userName).getPassword();
        if (BCrypt.checkpw(password, stored_hash)) {
            return true;
        } else{
            return false;
        }
    }

    @RequestMapping(value = "/v1/recipie/*",method = RequestMethod.DELETE)
    public @ResponseBody
    ResponseEntity<String> deleteRecipie(@RequestHeader(value="Authorization") String auth, HttpServletRequest request){
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        String[] URI = request.getRequestURI().split("/");
        if (URI.length == 4) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "recipie id is required");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        Recipie recipie = recipieDao.getRecipieInfo(URI[4]);
        User user = userDao.getUserInfo(email);
        if(!Authentication(email, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "email and password is not matching");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        if(recipie == null){
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Recipie with id " + URI[4] + " does not exist");
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(jObject.toString());
        }
        if(!user.getId().equals(recipie.getAuthorId())) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "you're not authorized to delete this recipie");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
        recipieDao.delete(recipie);
        JSONObject jObject = new JSONObject();
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(jObject.toString());
    }

}
