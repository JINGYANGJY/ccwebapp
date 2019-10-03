package Test;

import Controller.RecipieController;
import Controller.UserController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

public class UserControllerTest {
    UserController userController;
    RecipieController recipieController;
    String password;
    String nonStrongpassword;

    @Before
    public  void initial(){
        userController = new UserController();
        recipieController = new RecipieController();
        password ="Yang!123";
        nonStrongpassword="123";
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
    public void inputIntegerCheck() { Assert.assertTrue(recipieController.inputIntegerCheck("123"));}

    @Test
    public void nonInputIntegerCheck() {
        Assert.assertFalse(recipieController.inputIntegerCheck("abc"));
        Assert.assertFalse(recipieController.inputIntegerCheck("12.89"));
    }

    @Test
    public void inputFloatCheck() {
        Assert.assertTrue(recipieController.inputFloatCheck("12"));
        Assert.assertTrue(recipieController.inputFloatCheck("12.89"));
    }

    @Test
    public void nonInputFloatCheck() {
        Assert.assertFalse(recipieController.inputFloatCheck("abc"));
    }
}
