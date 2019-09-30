package DAO;

import POJO.NutritionInformation;

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
}
