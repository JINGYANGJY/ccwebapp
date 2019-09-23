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
import java.util.Date;
import java.util.List;

@Controller
public class CreateUser {
    @Autowired
    UserDao userDao;

//    @RequestMapping(value="/v1/use",method = RequestMethod.POST, consumes = "application/json")
//    public @ResponseBody
//    baseInfo
//    CreatAccount(@RequestBody Register register){
//        User user =new User(register);
//        baseInfo baseInfo = new baseInfo(register);
//        Date dNow = new Date( );
//        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");
//        user.setAccountCreated(ft.format(dNow));
//        userDao.register(user);
//        return baseInfo;
//    }

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
    public @ResponseBody
    Iterable<User> getAllUsers(){
        return  userDao.findAll();
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
            System.out.println("It matches");
            return true;
        } else{
            System.out.println("It does not match");
            return false;
        }
    }

}
