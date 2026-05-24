package com.sneakershop.backend.repository.login;

import com.sneakershop.backend.entity.login.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 🔥 THÊM: Lấy danh sách tài khoản sắp xếp ID giảm dần
    List<User> findAllByOrderByIdDesc();

    @Query("select count(u) > 0 from User u where lower(trim(u.username)) = lower(trim(:username))")
    boolean existsByUsernameNormalized(@Param("username") String username);

    @Query("select count(u) > 0 from User u where lower(trim(u.username)) = lower(trim(:username)) and u.id <> :id")
    boolean existsByUsernameNormalizedAndIdNot(@Param("username") String username, @Param("id") Long id);

    @Query("select count(u) > 0 from User u where lower(trim(u.email)) = lower(trim(:email))")
    boolean existsByEmailNormalized(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(trim(u.email)) = lower(trim(:email)) and u.id <> :id")
    boolean existsByEmailNormalizedAndIdNot(@Param("email") String email, @Param("id") Long id);

}