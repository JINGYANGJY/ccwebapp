package DAO;

import POJO.OrderedList;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;

import java.util.List;

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

    public List<OrderedList> getOrderedList(String recipieID){
        try {
            begin();
            Query q = getSession().createQuery("from OrderedList where recipie.id= :recipieID");
            q.setString("recipieID", recipieID);
            List<OrderedList> list = q.list();
            commit();
            return list;
        } catch (Exception e) {
            super.rollback();
        } finally {
            super.close();
        }
        return null;
    }

    public void update(OrderedList orderedList) {
        try {
            begin();
            getSession().update(orderedList);
            commit();
            close();
        } catch (Exception e) {
            super.rollback();
            throw e;
        }
    }

    public void delete(OrderedList orderedList) {
        try {
            super.begin();
            getSession().delete(orderedList);
            super.commit();
        } catch (Exception e) {
            super.rollback();
            throw e;
        } finally {
            close();
        }

    }
}
