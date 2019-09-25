package Test;

import Controller.CreateUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

public class CreateUserTest {
    CreateUser createUser;
    String password;
    String nonStrongpassword;

    @Before
    public  void initial(){
        createUser = new CreateUser();
        password ="Yang!123";
        nonStrongpassword="123";
    }


    @Test
    public void checkPassword() {
        String pw_hash = BCrypt.hashpw(password, BCrypt.gensalt());
        Assert.assertTrue(createUser.checkPassword(password,pw_hash));
    }

    @Test
    public void strongPasswordCheck(){
        Assert.assertTrue(createUser.strongPasswordCheck(password));
    }

    @Test
    public void nonStrongPasswordCheck(){
        Assert.assertFalse(createUser.strongPasswordCheck(nonStrongpassword));
    }
}
