package com.java.DAO;

import com.java.POJO.OrderedList;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderedListRepository extends CrudRepository<OrderedList,Integer> {
    public List<OrderedList> findAllById(String id);
}
