package com.example.UrbanismWebSite.repository;

import com.example.UrbanismWebSite.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPhone(String phone);

    @Query("SELECT m.email FROM Member m WHERE m.name = :userName AND m.phone = :userPhone")
    Optional<String> findEmail(String userName, String userPhone);

    @Query("SELECT m FROM Member m WHERE m.email = :userEmail AND m.phone = :userPhone")
    Optional<Member> findPasswd(String userEmail, String userPhone);

    @Query("SELECT COUNT(m) > 0 FROM Member m WHERE m.id = :id AND m.identifier IS NOT NULL")
    boolean existsByIdAndIdentifierIsNotNull(long id);
}
