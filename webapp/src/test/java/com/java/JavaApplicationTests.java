package com.java;

import com.java.Controller.RecipeController;
import com.java.Controller.UserController;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
public class JavaApplicationTests {
    UserController userController = new UserController();;
    RecipeController recipeController = new RecipeController();
    String password ="Yang!123";
    String nonStrongpassword="123";

    @Test
    public void contextLoads() {
    }

    @Test
    public void checkPassword() {
        String pw_hash = BCrypt.hashpw(password, BCrypt.gensalt());
        Assert.assertTrue(userController.checkPassword(password,pw_hash));
    }

    @Test
    public void strongPasswordCheck(){
        Assert.assertTrue(userController.strongPasswordCheck(password));
    }

    @Test
    public void nonStrongPasswordCheck(){
        Assert.assertFalse(userController.strongPasswordCheck(nonStrongpassword));
    }

    @Test
    public void inputIntegerCheck() { Assert.assertTrue(recipeController.inputIntegerCheck("123"));}

    @Test
    public void nonInputIntegerCheck() {
        Assert.assertFalse(recipeController.inputIntegerCheck("abc"));
        Assert.assertFalse(recipeController.inputIntegerCheck("12.89"));
    }

    @Test
    public void inputFloatCheck() {
        Assert.assertTrue(recipeController.inputFloatCheck("12"));
        Assert.assertTrue(recipeController.inputFloatCheck("12.89"));
    }

    @Test
    public void nonInputFloatCheck() {
        Assert.assertFalse(recipeController.inputFloatCheck("abc"));
    }

}
