package com.example.UrbanismWebSite.repository;

import com.example.UrbanismWebSite.model.Article;
import com.example.UrbanismWebSite.model.Member;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    @Transactional
    void deleteAllByMember(Member member);

    //오늘 날짜를 기준으로 년도, 학기가 일치하는 데이터만 추출
    @Query("SELECT a FROM Article a " +
            "WHERE a.created BETWEEN :stDate AND :endDate")
    Page<Article> findByDate(@Param("stDate") LocalDateTime stDate,
                                      @Param("endDate")LocalDateTime endDate,
                                      Pageable pageable);
}
