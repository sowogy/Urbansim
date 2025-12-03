package com.example.UrbanismWebSite.repository;

import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @Transactional
    void deleteAllByMember(Member member);
}
