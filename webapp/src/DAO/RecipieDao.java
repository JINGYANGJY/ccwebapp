package DAO;

import POJO.Recipie;
import org.hibernate.query.Query;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;


public class RecipieDao extends Dao {


    public Recipie save(Recipie recipie)  {
        try {
            super.begin();

            getSession().save(recipie);
            super.commit();
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return recipie;
    }


    public boolean delete(Recipie recipie){
        try {
            super.begin();

            getSession().delete(recipie);
            super.commit();
        } catch (Exception e) {
            super.rollback();
            return false;
        } finally {
            super.close();
        }
        return true;
    }

    public Recipie getRecipieInfo(String id) {
        try {
            begin();
            Query q = getSession().createQuery("from Recipie  where id = :id");
            q.setString("id", id);
            Recipie recipie = (Recipie) q.uniqueResult();
            commit();
            return recipie;
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return null;
    }

    public void update(Recipie recipie) {
        try {
            begin();
            getSession().update(recipie);
            commit();
            close();
        } catch (Exception e) {
            super.rollback();
            throw e;
        }
    }

    public void deleteIngredients(String id) {
       try{
           begin();
           List<String> list = getRecipieInfo(id).getIngredients();
           for(String a : list) {
               getSession().delete(a);
           }
           close();
       }catch (Exception e) {
           super.rollback();
           throw e;
       }
    }
}
