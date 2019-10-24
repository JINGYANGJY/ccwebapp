package com.java.DAO;

import com.java.POJO.Image;
import org.springframework.data.repository.CrudRepository;

public interface ImageRepository extends CrudRepository<Image, String> {
    public Image findImageById(String id);

}
