package Controller;


import Dao.UserDao;
import POJO.Register;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
//@RequestMapping("/v1/user")
public class registerController  {
//    @Autowired
//    @Qualifier("UserDao")
//    UserDao userDao;


    //@RequestMapping(method = RequestMethod.POST)
    @PostMapping("/v1")
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity CreatAccount(@RequestBody Register register){
       // register = new Register("a@gmail.com","aaa","bbb","ss");
        return ResponseEntity.ok(HttpStatus.OK);
    }

//    @RequestMapping(method = RequestMethod.GET)
//    public ModelAndView initializeForm(Model model) {
//        return new ModelAndView("register");
//
//    }




}

