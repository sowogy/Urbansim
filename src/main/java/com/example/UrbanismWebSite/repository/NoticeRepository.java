package com.example.UrbanismWebSite.repository;

import com.example.UrbanismWebSite.model.Member;
import com.example.UrbanismWebSite.model.Notice;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @Transactional
    void deleteAllByMember(Member member);
}

