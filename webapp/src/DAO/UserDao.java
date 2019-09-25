package DAO;

import POJO.User;
import org.hibernate.HibernateException;
import org.hibernate.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    public User getUserInfo(String email) {
        try {
            begin();
            Query q = getSession().createQuery("from User where email = :email");
            q.setString("email", email);
            User u = (User) q.uniqueResult();
            commit();
            return u;
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return null;
    }

    public User updateUser(User user, String firstName, String lastName, String password) {
        try {
            begin();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            Date dNow = new Date( );
            SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            user.setAccountUpdate(ft.format(dNow));
            getSession().update(user);
            commit();
            return user;
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return null;
    }
}
