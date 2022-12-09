package com.mysite.sitebackend.board.dao;

import com.mysite.sitebackend.board.domain.ImageFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ImageFileRepository extends JpaRepository<ImageFile, Integer> {
    ImageFile findByBoardIndex(Integer boardIndex);
    @Transactional
    void deleteByBoardIndex(Integer boardIndex);
}
