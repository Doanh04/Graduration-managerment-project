package com.graduration.Repository;

import com.graduration.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    boolean existsByUserName(String userName);

    boolean existsByUserNameAndUserIdNot(String userName, String userId);
}
