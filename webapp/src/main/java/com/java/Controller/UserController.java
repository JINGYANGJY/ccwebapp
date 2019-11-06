package com.java.Controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java.DAO.UserRepository;
import com.java.POJO.User;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import static com.java.JavaApplication.statsDClient;
import static com.java.JavaApplication.LOGGER;

@RestController
public class UserController {

    @Autowired
    private UserRepository userRepository;
    //create a User
    @RequestMapping(value = "/v1/user", method = RequestMethod.POST, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    createAccount(@RequestBody ObjectNode objectNode) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.user.http.post");
        LOGGER.info("user.post: Create user");

        if (objectNode.get("first_name") == null || objectNode.get("last_name") == null ||
                objectNode.get("email_address") == null || objectNode.get("password") == null) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Required info Cannot be null");
            recordTime("endpoint.user.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
        String firstName = objectNode.get("first_name").asText();
        String lastName = objectNode.get("last_name").asText();
        String email = objectNode.get("email_address").asText();
        String password = objectNode.get("password").asText();
        String passwordPattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";//Strong password
        boolean isPassMatch = Pattern.matches(passwordPattern, password);

        String emailPattern = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'*+/=?`{|}~^-]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
        boolean isEmailMatch = Pattern.matches(emailPattern, email);

        if (!isEmailMatch) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Please use correct email");
            recordTime("endpoint.user.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        if (!isPassMatch) {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Please use Strong Password");
            recordTime("endpoint.user.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }

        String pw_hash = BCrypt.hashpw(password, BCrypt.gensalt());

        long findQueryStart = System.currentTimeMillis();
        User user = userRepository.findUserByEmail(email);
        recordTime("endpoint.user.http.post.query.findUserByEmail", findQueryStart);

        if (user == null) {
            Date dNow = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            user = new User(firstName, lastName, email, pw_hash);
            user.setAccountCreated(ft.format(dNow));
            user.setAccountUpdate(ft.format(dNow));
            user.setId(UUID.randomUUID().toString());

            long saveQueryStart = System.currentTimeMillis();
            userRepository.save(user);
            recordTime("endpoint.user.http.post.query.save", saveQueryStart);

            JSONObject jObject = new JSONObject();
            jObject.put("id", user.getId());
            jObject.put("first_name", user.getFirstName());
            jObject.put("last_name", user.getLastName());
            jObject.put("email_address", user.getEmail());
            jObject.put("account_created", user.getAccountCreated());
            jObject.put("account_updated", user.getAccountUpdate());

            recordTime("endpoint.user.http.post", startTime);

            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        } else {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Email Address Already Exists");
            recordTime("endpoint.user.http.post", startTime);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(jObject.toString());
        }
    }

    //get User Information
    @RequestMapping(value = "/v1/user/self", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<String> getUserInfo(@RequestHeader(value = "Authorization") String auth) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.user.http.get");
        LOGGER.info("user.get: Get user info");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];

        long findQueryStart = System.currentTimeMillis();
        User userAu = userRepository.findUserByEmail(email);
        recordTime("endpoint.user.http.get.query.findUserByEmail", findQueryStart);

        if (Authentication(userAu, password)) {
            JSONObject jObject = new JSONObject();
            jObject.put("id", userAu.getId());
            jObject.put("first_name", userAu.getFirstName());
            jObject.put("last_name", userAu.getLastName());
            jObject.put("email_address", userAu.getEmail());
            jObject.put("account_created", userAu.getAccountCreated());
            jObject.put("account_updated", userAu.getAccountUpdate());

            recordTime("endpoint.user.http.get", startTime);
            return ResponseEntity.status(HttpStatus.OK).
                    body(jObject.toString());
        } else {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Unable to get any other user info");
            recordTime("endpoint.user.http.get", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
    }

    //update user information
    @RequestMapping(value = "/v1/user/self", method = RequestMethod.PUT, consumes = "application/json")
    public @ResponseBody
    ResponseEntity<String>
    updateUserInfo(@RequestHeader(value = "Authorization") String auth, @RequestBody ObjectNode objectNode) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("endpoint.user.http.put");
        LOGGER.info("user.put: Update user info");

        byte[] decodedBytes = Base64.getDecoder().decode(auth.split("Basic ")[1]);
        String decodedString = new String(decodedBytes);
        String email = decodedString.split(":")[0];
        String password = decodedString.split(":")[1];

        long findQueryStart = System.currentTimeMillis();
        User userAu = userRepository.findUserByEmail(email);
        recordTime("endpoint.user.http.put.query.findUserByEmail", findQueryStart);

        if (Authentication(userAu, password)) {
            Iterator<String> fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!(field.equals("first_name") || field.equals("last_name") || field.equals("password"))) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "Only first name, last name and password can be updated");
                    recordTime("endpoint.user.http.put", startTime);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(jObject.toString());
                }
            }
            String newFirstName = objectNode.get("first_name") == null || objectNode.get("first_name").asText() == "" ? userAu.getFirstName() : objectNode.get("first_name").asText();
            String newLastName = objectNode.get("last_name") == null || objectNode.get("last_name").asText() == "" ? userAu.getLastName() : objectNode.get("last_name").asText();
            String newPassword;

            if (objectNode.get("password") == null || objectNode.get("password").asText() == "") {
                newPassword = userAu.getPassword();
            } else {
                newPassword = objectNode.get("password").asText();
                String pattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";//Strong password

                boolean isMatch = Pattern.matches(pattern, newPassword);

                if (!isMatch) {
                    JSONObject jObject = new JSONObject();
                    jObject.put("message", "Please use Strong Password");
                    recordTime("endpoint.user.http.put", startTime);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(jObject.toString());
                }
                newPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            }
            userAu.setPassword(newPassword);
            userAu.setEmail(email);
            userAu.setFirstName(newFirstName);
            userAu.setLastName(newLastName);
            long saveQueryStart = System.currentTimeMillis();
            userRepository.save(userAu);
            recordTime("endpoint.user.http.put.query.save", saveQueryStart);

            JSONObject jObject = new JSONObject();
            recordTime("endpoint.user.http.put", startTime);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).
                    body(jObject.toString());
        } else {
            JSONObject jObject = new JSONObject();
            jObject.put("message", "Unable to update any other user info");
            recordTime("endpoint.user.http.put", startTime);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(jObject.toString());
        }
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

    public boolean checkPassword(String password, String stored_hash) {
        if (BCrypt.checkpw(password, stored_hash)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean strongPasswordCheck(String password) {
        String pattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$";//Strong password
        boolean isMatch = Pattern.matches(pattern, password);
        if (!isMatch) {
            return false;
        }
        return true;
    }

    public void recordTime(String name, Long startTime) {
        long endTime = System.currentTimeMillis();
        statsDClient.recordExecutionTime(name, endTime - startTime);
    }
}
