package com.mysite.sitebackend.board.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.mysite.sitebackend.board.dao.ImageFileRepository;
import com.mysite.sitebackend.board.domain.ImageFile;
import lombok.RequiredArgsConstructor;
import marvin.image.MarvinImage;
import org.marvinproject.image.transform.scale.Scale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3 amazonS3;
    private final ImageFileRepository imageFileRepository;

    private String createFileName(String fileName) {
        return UUID.randomUUID().toString().concat(getFileExtension(fileName));
    }

    private String getFileExtension(String fileName) {
        try {
            return fileName.substring(fileName.lastIndexOf("."));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일(" + fileName + ") 입니다.");
        }
    }

    MultipartFile resizeImage(String fileName, String fileFormatName, MultipartFile originalImage, int targetWidth) {
        try {
            BufferedImage image = ImageIO.read(originalImage.getInputStream());
            int originWidth = image.getWidth();
            int originHeight = image.getHeight();

            if(originWidth < targetWidth)
                return originalImage;

            MarvinImage imageMarvin = new MarvinImage(image);

            Scale scale = new Scale();
            scale.load();
            scale.setAttribute("newWidth", targetWidth);
            scale.setAttribute("newHeight", targetWidth * originHeight / originWidth);
            scale.process(imageMarvin.clone(), imageMarvin, null, null, false);

            BufferedImage imageNoAlpha = imageMarvin.getBufferedImageNoAlpha();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(imageNoAlpha, fileFormatName, baos);
            baos.flush();

            return new MockMultipartFile(fileName, baos.toByteArray());

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 리사이즈에 실패했습니다.");
        }
    }

    //이미지 업로드(미디어 서버, DB)
    public boolean uploadImage(List<MultipartFile> multipartFile, Integer boardIndex) {
        List<String> fileNameList = new ArrayList<>();

        multipartFile.forEach(file -> {
            if(Objects.requireNonNull(file.getContentType().contains("image"))) {
                String fileName = createFileName(file.getOriginalFilename());
                String fileFormatName = file.getContentType().substring(file.getContentType().lastIndexOf("/") + 1);

                ImageFile I1 = new ImageFile();
                I1.setFileName(fileName);
                I1.setFileFormatName(fileFormatName);
                I1.setBoardIndex(boardIndex);
                this.imageFileRepository.save(I1);

                MultipartFile resizedFile = resizeImage(fileName, fileFormatName, file, 768);

                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(resizedFile.getSize());
                objectMetadata.setContentType(file.getContentType());

                try(InputStream inputStream = resizedFile.getInputStream()) {
                    amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, objectMetadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead));
                } catch(IOException e) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.");
                }

                fileNameList.add(fileName);
            }
        });
        return true;
    }

    //이미지 불러오기
    public String getImage(Integer boardIndex){
        String URL = "https://jong-first-bucket.s3.ap-northeast-2.amazonaws.com/";
        ImageFile imageFile = this.imageFileRepository.findByBoardIndex(boardIndex);
        return URL +imageFile.getFileName();
    }

    // 이미지 수정
    public boolean patchImage(List<MultipartFile> multipartFile, Integer boardIndex)  {
        List<String> fileNameList = new ArrayList<>();

        Optional<ImageFile> optionalImageFile = Optional.ofNullable(this.imageFileRepository.findByBoardIndex(boardIndex));
        // DB에 저장된 데이터가 있을 경우
        if(optionalImageFile.isPresent()){
            // DB에서 데이터를 지움.
            this.imageFileRepository.deleteByBoardIndex(boardIndex);
            // DB에 저장된 데이터가 있으면서
            // 사용자가 이미지를 보냈을때
            if (multipartFile.get(0).isEmpty()){
                // 새로운 이미지를 등록함.
                // 미디어 서버에서만 지움.
                amazonS3.deleteObject(new DeleteObjectRequest(bucket, optionalImageFile.get().getFileName()));
                return true;
            }
            // DB에 저장된 데이터가 있으면서
            // 사용자가 이미지를 안보냈을때
            else{
                amazonS3.deleteObject(new DeleteObjectRequest(bucket, optionalImageFile.get().getFileName()));
                uploadImage(multipartFile, boardIndex);
                return true;

            }
        }
        // DB에 저장된 데이터가 없을 경우
        else{
            // DB에 저장된 데이터가 없으면서
            // 사용자가 이미지를 보냈을때
            if (multipartFile.get(0).isEmpty()){
                return true;
            }
            // DB에 저장된 데이터가 없으면서
            // 사용자가 이미지를 안보냈을때
            else{
                uploadImage(multipartFile, boardIndex);
                return true;
            }
        }
    }

    //이미지 삭제
    public boolean deleteImage(Integer boardIndex) {
        Optional<ImageFile> optionalImageFile = Optional.ofNullable(this.imageFileRepository.findByBoardIndex(boardIndex));
        if(optionalImageFile.isPresent()){
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, optionalImageFile.get().getFileName()));
            try {
                this.imageFileRepository.deleteByBoardIndex(optionalImageFile.get().getBoardIndex());
            }catch (Exception e){
                System.out.println(e);
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }
}

