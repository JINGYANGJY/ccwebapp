package DAO;

import POJO.NutritionInformation;
import org.hibernate.HibernateException;
import org.hibernate.query.Query;

public class NutritionInformationDao extends Dao {

    public NutritionInformation save(NutritionInformation nutritionInformation)  {
        try {
            super.begin();
            getSession().save(nutritionInformation);
            super.commit();
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return nutritionInformation;
    }

    public NutritionInformation get(String recipieID){
        try {
            begin();
            Query q = getSession().createQuery("from NutritionInformation where id = :recipieID");
            q.setString("recipieID", recipieID);
            NutritionInformation nutritionInformation = (NutritionInformation) q.uniqueResult();
            commit();
            return nutritionInformation;
        } catch (HibernateException e) {
            rollback();
        } finally {
            close();
        }
        return null;
    }

    public void update(NutritionInformation nutritionInformation) {
        try {
            begin();
            getSession().update(nutritionInformation);
            commit();
            close();
        } catch (Exception e) {
            super.rollback();
            throw e;
        }
    }
}
