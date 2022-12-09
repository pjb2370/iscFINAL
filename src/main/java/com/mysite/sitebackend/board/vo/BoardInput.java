package com.mysite.sitebackend.board.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoardInput {
    private Integer id;
    private String subject;
    private String contents;
    private String author;
}
