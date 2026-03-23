package com.example.UrbanismWebSite.repository;

import com.example.UrbanismWebSite.model.Authority;
import com.example.UrbanismWebSite.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    List<Authority> findByMember(Member member);
    List<Authority> findById(long id);

}
