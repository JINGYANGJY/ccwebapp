package com.java.DAO;

import com.java.POJO.NutritionInformation;
import org.springframework.data.repository.CrudRepository;

public interface NutritionInformationRepository extends CrudRepository<NutritionInformation,String> {
    public NutritionInformation findAllById(String id);
}
