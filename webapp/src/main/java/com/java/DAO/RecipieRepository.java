package com.java.DAO;

import com.java.POJO.Recipie;
import org.springframework.data.repository.CrudRepository;

public interface RecipieRepository extends CrudRepository<Recipie,String> {
     Recipie findRecipieById(String id);
}
