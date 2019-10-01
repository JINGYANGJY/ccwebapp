package DAO;

import POJO.OrderedList;

public class OrderedListDao extends Dao{
    public OrderedList save(OrderedList orderedList)  {
        try {
            super.begin();
            getSession().save(orderedList);
            super.commit();
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return orderedList;
    }
}
