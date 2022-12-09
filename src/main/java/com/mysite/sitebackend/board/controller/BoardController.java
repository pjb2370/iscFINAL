package com.mysite.sitebackend.board.controller;

import com.mysite.sitebackend.board.dto.*;
import com.mysite.sitebackend.board.service.AwsS3Service;
import com.mysite.sitebackend.board.service.BoardService;
import com.mysite.sitebackend.board.vo.BoardInput;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@RequestMapping("/board/{lcategory}/{mcategory}")
@RequiredArgsConstructor
@RestController
@CrossOrigin("*")
public class BoardController {
    private final BoardService boardService;
    private final AwsS3Service awsS3Service;

    @RequestMapping("/")
    public String index(@PathVariable("lcategory") String Lcategory, @PathVariable("mcategory") String Mcategory) {
        System.out.println("Coin" + Lcategory + Mcategory);
        return "Coin" + Lcategory + Mcategory;
    }

    // 전체 검색
    @GetMapping("/searchAll")
    public List<BoardSearchAllDto> search(@RequestParam("value") String value) {
        return this.boardService.searchAll(value);
    }

    // 게시판별 검색
    @GetMapping("/search")
    public List<BoardListDto> search(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, @RequestParam("value") String value) {
        return this.boardService.search(value, lcategory, mcategory);
    }

    // 게시판별 검색
    @GetMapping("/searchAll/comment")
    public List<CommentListDto> searchComment(@RequestParam("value") String value) {
        return this.boardService.searchComment(value);
    }

    // 게시글 작성하기
    @PostMapping("/post")
    public boolean boardPost(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, BoardInput boardInput, @RequestParam(value = "multipartFile", required = false) List<MultipartFile> multipartFile) {

        System.out.println(multipartFile);
        try {
            return this.boardService.boardPost(lcategory, mcategory, boardInput, multipartFile);
        } catch (SQLException e) {
            return false;
        }
    }

    // 이미지 DB, 서버 업로드
    @PostMapping("/post/image")
    public boolean uploadImage(@ApiParam(value = "img 파일들(여러 파일 업로드 가능)", required = true) @RequestPart List<MultipartFile> multipartFile, @RequestParam("boardIndex") Integer boardIndex) {
        return awsS3Service.uploadImage(multipartFile, boardIndex);
    }

    // 댓글 작성하기
    @PostMapping("/post/comment")
    public boolean commentPost(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, @RequestBody BoardInput boardInput) {
        return this.boardService.commentPost(boardInput);
    }

    // 게시글 전체 불러오기
    @GetMapping("/get")
    public List<BoardListDto> get(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory) {
        return this.boardService.findAll(lcategory, mcategory);
    }

    // 게시글 3개만 불러오기
    @GetMapping("/get3")
    public List<BoardListDto> get3(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory) {
        return this.boardService.findThree(lcategory, mcategory);
    }

    // 게시글 5개만 불러오기
    @GetMapping("/get5")
    public List<BoardListDto> get5(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory) {
        return this.boardService.findFive(lcategory, mcategory);
    }

    // 게시글 5개만 불러오기
    @GetMapping("/get5hot")
    public List<BoardListDto> get5hot(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory) {
        return this.boardService.findFivebyhot();
    }

    // 게시글 상세내용 불러오기
    @GetMapping("/getid")
    public BoardDto boardGet(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, @RequestParam("id") Integer id) {
        return this.boardService.findByIdToBoard(lcategory, mcategory, id);
    }

    // 해당 id값의 댓글들 불러오기
    @GetMapping("/getid/comment")
    public List<CommentListDto> commentGet(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, @RequestParam("id") Integer id) {
        return this.boardService.findByIdToComment(lcategory, mcategory, id);
    }

    // 이미지 불러오기
    @GetMapping("/get/image")
    public String getImage(@RequestParam("boardIndex") Integer boardIndex){
        return awsS3Service.getImage(boardIndex);
    }

    // 게시글 수정
    @PatchMapping("/patch")
    public boolean boardPatch(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, @RequestBody BoardInput boardInput) {
        return this.boardService.boardPatch(lcategory, mcategory, boardInput);
    }

    // 댓글 수정
    @PatchMapping("/patch/comment")
    public boolean commentPatch(@RequestBody BoardInput boardInput) {
        return this.boardService.commentPatch(boardInput);
    }

    // 이미지 수정
    @PatchMapping("/patch/image")
    public boolean patchImage(@RequestPart List<MultipartFile> multipartFile, @RequestParam("boardIndex") Integer boardIndex) throws IOException {
        return awsS3Service.patchImage(multipartFile, boardIndex);
    }

    // 게시글 삭제
    @DeleteMapping("/delete")
    public boolean boardDelete(@PathVariable("lcategory") String lcategory, @PathVariable("mcategory") String mcategory, @RequestBody BoardInput boardInput) {
        return this.boardService.boardDelete(lcategory, mcategory, boardInput);
    }

    // 댓글삭제
    @DeleteMapping("/delete/comment")
    public boolean commnetDelete(@RequestBody BoardInput boardInput) {
        return this.boardService.commnetDelete(boardInput);
    }

    // 이미지 삭제
    @DeleteMapping("/delete/image")
    public boolean deleteImage(@RequestParam("boardIndex")Integer boardIndex) {
        return awsS3Service.deleteImage(boardIndex);
    }
}