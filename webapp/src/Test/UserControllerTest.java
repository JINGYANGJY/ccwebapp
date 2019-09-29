package Test;

import Controller.UserController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

public class UserControllerTest {
    UserController userController;
    String password;
    String nonStrongpassword;

    @Before
    public  void initial(){
        userController = new UserController();
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
}
