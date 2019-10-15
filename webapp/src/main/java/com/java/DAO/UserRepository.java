package com.java.DAO;

import com.java.POJO.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {
    User findUserByEmail(String email);
}

