package Controller;

import DAO.UserDao;
import POJO.User;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class CreateUser {
    @Autowired
    UserDao userDao;

    //create a User
    @RequestMapping(value="/v1/user",method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody ResponseEntity<String>
    createAccount(@RequestBody ObjectNode objectNode){
        if(objectNode.get("first_name")==null||objectNode.get("last_name")==null||
                objectNode.get("email_address")==null||objectNode.get("password")==null)
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("Required info Cannot be null");
        String firstName = objectNode.get("first_name").asText();
        String lastName = objectNode.get("last_name").asText();
        String email = objectNode.get("email_address").asText();
        String password = objectNode.get("password").asText();
        String passwordPattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";//Strong password
        boolean isPassMatch = Pattern.matches(passwordPattern, password);

        String emailPattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        boolean isEmailMatch = Pattern.matches(emailPattern, email);

        if(!isEmailMatch) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Please use correct email");
        }

        if(!isPassMatch) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Please use Strong Password");
        }

        String pw_hash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = userDao.getUserInfo(email);
        if(user == null){
            Date dNow = new Date( );
            SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            user = new User(firstName, lastName, email, pw_hash);
            user.setAccountCreated(ft.format(dNow));
            user.setAccountUpdate(ft.format(dNow));
            user.setId(UUID.randomUUID().toString());
            userDao.register(user);
            JSONObject jObject = new JSONObject();
            jObject.put("id", user.getId());
            jObject.put("first_name", user.getFirstName());
            jObject.put("last_name", user.getLastName());
            jObject.put("email_address", user.getEmail());
            jObject.put("account_created", user.getAccountCreated());
            jObject.put("account_updated", user.getAccountUpdate());

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Email Address Already Exists");
        }
    }

    //get User Information
    @RequestMapping(value="/v1/user/self", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<String> getUserInfo(@RequestHeader(value="Authorization") String auth){
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        if(Authentication(email, password)) {
            User user = userDao.getUserInfo(email);
            JSONObject jObject = new JSONObject();
            jObject.put("id", user.getId());
            jObject.put("first_name", user.getFirstName());
            jObject.put("last_name", user.getLastName());
            jObject.put("email_address", user.getEmail());
            jObject.put("account_created", user.getAccountCreated());
            jObject.put("account_updated", user.getAccountUpdate());

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        }
        else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Unable to get any other user info");
        }
    }

    //update user information
    @RequestMapping(value="/v1/user/self",method = RequestMethod.PUT, consumes = "application/json")
    public @ResponseBody ResponseEntity<String>
    updateUserInfo(@RequestHeader(value="Authorization") String auth, @RequestBody ObjectNode objectNode){
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];

        if(Authentication(email, password)) {
            Iterator<String> fieldNames = objectNode.fieldNames();
            while(fieldNames.hasNext()) {
                String field = fieldNames.next();
                if(!(field.equals("first_name") || field.equals("last_name") || field.equals("password"))) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(" Only first name, last name and password can be updated");
                }
            }
            User user = userDao.getUserInfo(email);
            String newFirstName = objectNode.get("first_name") == null || objectNode.get("first_name").asText() == "" ? user.getFirstName() : objectNode.get("first_name").asText();
            String newLastName = objectNode.get("last_name") == null || objectNode.get("last_name").asText() == "" ? user.getLastName() : objectNode.get("last_name").asText();
            String newPassword;

            if(objectNode.get("password") == null || objectNode.get("password").asText() == "") {
                newPassword = user.getPassword();
            } else {
                newPassword = objectNode.get("password").asText();
                String pattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";//Strong password

                boolean isMatch = Pattern.matches(pattern, newPassword);

                if(!isMatch) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body("Please use Strong Password");
                }
                newPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            }
            userDao.updateUser(user, newFirstName, newLastName, newPassword);

            return ResponseEntity.status(HttpStatus.NO_CONTENT).
                    body("");
        }
        else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Unable to update any other user info");
        }
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

    public boolean checkPassword(String password, String stored_hash){
        if (BCrypt.checkpw(password, stored_hash)) {
            return true;
        } else{
            return false;
        }
    }

    public boolean strongPasswordCheck(String password){
        String pattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";//Strong password
        boolean isMatch = Pattern.matches(pattern, password);
        if(!isMatch) {
            return false;
        }
        return true;
    }
}
