package DAO;

import POJO.NutritionInformation;
import POJO.OrderedList;
import POJO.Recipie;
import org.hibernate.query.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    public Recipie updateRecipie(Recipie recipie, int cookTimeInMin, int prepTimeInMin,
                                 int totalTimeInMin, String title, String cusine, int servings,
                                 List<String> ingredients, List<OrderedList> steps,
                                 NutritionInformation nutritionInformation) {
        try {
            recipie.setCookTimeInMin(cookTimeInMin);
            recipie.setPrepTimeInMin(prepTimeInMin);
            recipie.setTotalTimeInMin(totalTimeInMin);
            recipie.setTitle(title);
            recipie.setCusine(cusine);
            recipie.setServings(servings);
            recipie.setIngredients(ingredients);
            recipie.setSteps(steps);
            recipie.setNutritionInformation(nutritionInformation);
            Date dNow = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            recipie.setUpdatedTs(ft.format(ft));

            getSession().update(recipie);
            commit();
            return recipie;
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return null;

    }

}
