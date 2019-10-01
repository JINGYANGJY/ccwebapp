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
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.acl.Owner;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class RecipieController {
    // TODO: 1. while creating or updating a recipie, remember to
    //  check ingredients items are unique or not;
    //  2. while creating or updating a recipie, servings should be
    //  in [1,5] range.


//    @RequestMapping(value="/v1/recipie/{id}",method = RequestMethod.PUT, consumes = "application/json")
//    public @ResponseBody
//    ResponseEntity<String>
//    createAccount(@RequestHeader(value="Authorization") String auth, @RequestBody ObjectNode objectNode){
//
//        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
//        String decodedString = new String(decodedBytes);
//        String email = decodedString.split(":")[0];
//        String password = decodedString.split(":")[1];
//
//
//        int cook_time_in_min = objectNode.get("cook_time_in_min").asInt();
//        int prep_time_in_min = objectNode.get("prep_time_in_min").asInt();
//        String title = objectNode.get("title").asText();
//        String cusine = objectNode.get("cusine").asText();
//        int servings = objectNode.get("servings").asInt();
//        JsonNode ingredients= objectNode.get("ingredients");
//        List<String>  ingredientsArray = new ArrayList<>();
//        while(ingredients.iterator().hasNext()){
//            ingredientsArray.add(ingredients.iterator().next().asText());
//        }
//        ArrayNode orderedLists = (ArrayNode) objectNode.get("steps");
//        for(JsonNode orderedList_JsonNode: orderedLists){
//            OrderedList orderedList = new OrderedList();
//            orderedList.setPosition(orderedList_JsonNode.get("position").asInt());
//            orderedList.setItems(orderedList_JsonNode.get("items").asText());
//        }
//        JsonNode nutrition_information= objectNode.get("nutrition_information");
//        NutritionInformation nuInfo = new NutritionInformation();
//        nuInfo.setCalories(nutrition_information.get("calories").asInt());
//        nuInfo.setCarbohydratesInGrams(nutrition_information.get("carbohydrates_in_grams").asInt());
//        nuInfo.setCholesterolInMg(nutrition_information.get("cholesterol_in_mg").asInt());
//        nuInfo.setSodiumInMg(nutrition_information.get("sodium_in_mg").asInt());
//        nuInfo.setProteinInGrams(nutrition_information.get("protein_in_grams").asInt());
//
//    }

//    public boolean checkRecipieOwner(String email, String password, int recipieID) {
//
//    }
//    @RequestMapping(value="/v1/user/self",method = RequestMethod.PUT, consumes = "application/json")
//    public @ResponseBody ResponseEntity<String>
//    updateUserInfo(@RequestHeader(value="Authorization") String auth, @RequestBody ObjectNode objectNode){
//        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
//        String decodedString = new String(decodedBytes);
//        String email = decodedString.split(":")[0];
//        String password = decodedString.split(":")[1];
@Autowired
UserDao userDao;
    @Autowired
    NutritionInformationDao nutritionInformationDao;
    @Autowired
    OrderedListDao orderedListDao;
    @Autowired
    RecipieDao recipieDao;
    // TODO: 1. while creating or updating a recipie, remember to
    //  check ingredients items are unique or not;
    //  2. while creating or updating a recipie, servings should be
    //  in [1,5] range.
    @RequestMapping(value = "/v1/recipie",method = RequestMethod.POST,consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String> createRecipie(@RequestHeader(value="Authorization") String auth ,@RequestBody ObjectNode objectNode){

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];

        if(!Authentication(email,password)){
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(" Only first name, last name and password can be updated");
        }

        Recipie recipie = new Recipie();
        recipie.setCookTimeInMin(objectNode.get("cook_time_in_min").asInt());
        recipie.setPrepTimeInMin(objectNode.get("prep_time_in_min").asInt());
        recipie.setTitle(objectNode.get("title").asText());
        recipie.setCusine(objectNode.get("cusine").asText());
        recipie.setServings(objectNode.get("servings").asInt());
        recipie.setId(UUID.randomUUID().toString());
        //setAu
        String userId = userDao.getUserInfo(email).getId();
        recipie.setAuthorId(userId);


        //setingrediens
        List<String> ingredientsList = new ArrayList<>();
        JsonNode str = objectNode.get("ingredients");
        for(JsonNode ingredient:str){
            String str_ingredient = ingredient.toString();
            ingredientsList.add(str_ingredient.substring(1,str_ingredient.length()-1));
        }
            recipie.setIngredients(ingredientsList);

        //setTime
        Date dNow = new Date( );
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        recipie.setCreatedTs(ft.format(dNow));
        recipie.setUpdatedTs(ft.format(dNow));


        // setSteps
        List<OrderedList> orderedLists = new LinkedList<>();
        ArrayNode arrayNode = objectNode.withArray("steps");

        for(JsonNode jsonNode:arrayNode){
            OrderedList orderedList = new OrderedList();
            orderedList.setPosition(jsonNode.get("position").asInt());
            orderedList.setItems(jsonNode.get("items").asText());
            orderedList.setRecipie(recipie);
            orderedLists.add(orderedList);
        }
        recipie.setSteps(orderedLists);
        recipieDao.save(recipie);

        for(OrderedList ol:orderedLists){
            ol.setRecipie(recipie);
            orderedListDao.save(ol);
        }

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
        nutritionInformationDao.save(nutritionInformation);

        return ResponseEntity.status(HttpStatus.OK).
                body(str.toString());

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
    ResponseEntity<String> createRecipie(@RequestHeader(value="Authorization") String auth, HttpServletRequest request){
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        String[] URI = request.getRequestURI().split("/");
        Recipie recipie = recipieDao.getRecipieInfo(URI[4]);
        User user =userDao.getUserInfo(email);
        if(!Authentication(email,password) || !user.getId().equals(recipie.getAuthorId())){
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("hhaha gun qu yanzheng");
        }else if(recipie==null){
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(URI[4]);
        }else {

            recipieDao.delete(recipie);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("gaoding");
        }

    }








    }
