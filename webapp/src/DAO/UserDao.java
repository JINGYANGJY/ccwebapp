package DAO;

import POJO.User;
import java.util.List;


public class UserDao extends Dao {
    public User register(User u)  {
        try {
            super.begin();
            getSession().save(u);
            super.commit();
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return u;
    }

    public List<User> findAll() {
        try{
            super.begin();
            List<User> users = getSession().createQuery("from User").list();
            super.commit();
            return users;
        }catch (Exception e) {
            super.rollback();
        }finally {
            super.close();
        }
        return null;
    }
}
