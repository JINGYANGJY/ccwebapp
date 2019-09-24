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
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Controller
public class CreateUser {
    @Autowired
    UserDao userDao;

    @RequestMapping(value="/v1/auth",method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody ResponseEntity<String>
    getAuth(@RequestBody ObjectNode objectNode){
        String email = objectNode.get("email").asText();
        String password = objectNode.get("password").asText();
        String input = email + ':' + password;
        String auth = Base64.getEncoder().encodeToString(input.getBytes());
        JSONObject jObject = new JSONObject();
        jObject.put("Authorization", "Basic " + auth);

        return ResponseEntity.status(HttpStatus.OK).
                body(jObject.toString());
    }

    @RequestMapping(value="/v1/use",method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody ResponseEntity<String>
    createAccount(@RequestBody ObjectNode objectNode){
        String firstName = objectNode.get("firstName").asText();
        String lastName = objectNode.get("lastName").asText();
        String email = objectNode.get("email").asText();
        String password = objectNode.get("password").asText();
        String pw_hash = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = getUser(email);
        if(user == null){
            Date dNow = new Date( );
            SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");
            user = new User(firstName, lastName, email, pw_hash);
            user.setAccountCreated(ft.format(dNow));
            userDao.register(user);

            JSONObject jObject = new JSONObject();
            jObject.put("firstName", user.getFirstName());
            jObject.put("lastName", user.getLastName());
            jObject.put("email", user.getEmail());

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Email Address Already Exists");
        }
    }


    @GetMapping (value="/v1/getAll")
    public @ResponseBody Iterable<User> getAllUsers(){
        return  userDao.findAll();
    }

    @RequestMapping(value="/v1/getInfo",method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody ResponseEntity<String>
    getUserInfo(@RequestHeader(value="Authorization") String auth, @RequestBody ObjectNode objectNode){
        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];
        if(Authentication(email, password)) {
            User user = userDao.getUserInfo(email);
            JSONObject jObject = new JSONObject();
            jObject.put("firstName", user.getFirstName());
            jObject.put("lastName", user.getLastName());
            jObject.put("email", user.getEmail());
            jObject.put("accountCreated", user.getAccountCreated());
            jObject.put("accountUpdate", user.getAccountUpdate());

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        }
        else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Unable to get any other user info");
        }
    }

    @RequestMapping(value="/v1/update",method = RequestMethod.PUT, consumes = "application/json")
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
                if(!(field.equals("firstName") || field.equals("lastName") || field.equals("password"))) {
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(" Only first name, last name and password can be updated");
                }
            }
            User user = userDao.getUserInfo(email);
            String newFirstName = objectNode.get("firstName") == null || objectNode.get("firstName").asText() == "" ? user.getFirstName() : objectNode.get("firstName").asText();
            String newLastName = objectNode.get("lastName") == null || objectNode.get("lastName").asText() == "" ? user.getLastName() : objectNode.get("lastName").asText();
            String newPassword = objectNode.get("password") == null || objectNode.get("password").asText() == "" ? user.getPassword() : BCrypt.hashpw(objectNode.get("password").asText(), BCrypt.gensalt());
            userDao.updateUser(user, newFirstName, newLastName, newPassword);
            JSONObject jObject = new JSONObject();
            jObject.put("firstName", user.getFirstName());
            jObject.put("lastName", user.getLastName());
            jObject.put("email", user.getEmail());
            jObject.put("accountCreated", user.getAccountCreated());
            jObject.put("accountUpdate", user.getAccountUpdate());

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        }
        else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Unable to get any other user info");
        }
    }

    public User getUser(String email){
        List<User> list = userDao.findAll();
        for(User u : list){
            if(u.getEmail().equalsIgnoreCase(email)){
                return u;
            }
        }
        return null;
    }

    public boolean Authentication(String userName, String password) {
        String stored_hash = getUser(userName).getPassword();
        if (BCrypt.checkpw(password, stored_hash)) {
            return true;
        } else{
            return false;
        }

    }

    //use regex to check if password is strong or not
    private boolean isStrong(String password){
        return password.matches("^(?=.*[A-Z].*[A-Z])(?=.*[!@#$&*])(?=.*[0-9].*[0-9])(?=.*[a-z].*[a-z].*[a-z])");
    }

}
