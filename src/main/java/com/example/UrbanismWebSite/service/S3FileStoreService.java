package com.example.UrbanismWebSite.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.example.UrbanismWebSite.dto.ArticleDTO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class S3FileStoreService {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3FileStoreService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    // 허용 확장자
    private static final Set<String> ALLOWED =
            Set.of("ppt", "pptx", "pdf", "hwp", "hwpx", "doc", "docx", "zip");

    // 파일당 최대 20MB
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    @Transactional(rollbackOn = {AmazonS3Exception.class})
    public List<S3StoredFile> storeAll(List<MultipartFile> files, String category) throws Exception {
        if (files == null || files.isEmpty()) return List.of();

        List<S3StoredFile> result = new ArrayList<>();
        for (MultipartFile mf : files) {
            if (mf.isEmpty()) continue;

            String original = StringUtils.cleanPath(Objects.requireNonNull(mf.getOriginalFilename()));
            String ext = getExtension(original).toLowerCase(Locale.ROOT);

            // 확장자 및 용량 검증
            if (!ALLOWED.contains(ext)) {
                throw new IllegalArgumentException("허용되지 않는 확장자: " + original);
            }
            if (mf.getSize() > MAX_SIZE) {
                throw new IllegalArgumentException("용량 초과(20MB): " + original);
            }
            String year = String.valueOf(LocalDate.now().getYear());
            String month = String.valueOf(LocalDate.now().getMonth());
            String day = String.valueOf(LocalDate.now().getDayOfMonth());

            // S3에 저장될 파일명 생성 (카테고리/년/월/일/UUID.확장자)
            String storedName = category + "/" + year + "/" + month + "/" + day + "/" + UUID.randomUUID() + "." + ext;

            // S3에 업로드 (v1 방식)
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(mf.getContentType());
            metadata.setContentLength(mf.getSize());

            //PutObjectRequest를 사용하여 'public-read' 권한 부여
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucket,
                    storedName,
                    mf.getInputStream(),
                    metadata
            );

            amazonS3.putObject(putObjectRequest);

            // S3 객체 URL 가져오기 (v1 방식)
            String publicUrl = amazonS3.getUrl(bucket, storedName).toString();

            S3StoredFile meta = new S3StoredFile(
                    original,
                    storedName,
                    publicUrl,
                    mf.getContentType(),
                    mf.getSize()
            );
            result.add(meta);
        }
        return result;
    }

    /**
     * S3에서 파일을 다운로드
     * S3Object만 전달 후 컨트롤러에서 처리
     */
    public S3Object downloadFile(String fileUrl){
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new IllegalArgumentException("파일명이 유효하지 않습니다.");
        }
        // S3에서 객체 가져오기 (v1 방식)
        return amazonS3.getObject(new GetObjectRequest(bucket, fileUrl));
    }

    /**
     * S3에서 파일을 삭제
     */
    @Transactional(rollbackOn = {AmazonS3Exception.class})
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            // S3 객체 삭제
            amazonS3.deleteObject(bucket, fileUrl);
        } catch (Exception e) {
            throw new AmazonS3Exception("파일 삭제 중 에러 발생", e);
        }
    }

    private static String getExtension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return (i == -1) ? "" : filename.substring(i + 1);
    }

    /** 업로드된 파일의 메타데이터 */
    public record S3StoredFile(
            String originalName,
            String storedName,
            String publicUrl,
            String contentType,
            long size
    ) {}

    /** URL에서 파일 경로만 추출 */
    public String findPath(String url){
        try{
            //URL에서 S3 버킷에 파일이 저장되는 경로만 추출
            URL fileUrl = new URL(url);
            String path = fileUrl.getPath();

            //Path가 '/'로 시작하는 경우 맨 앞 '/'를 없앰
            //S3는 폴더/파일명으로 경로 인식
            if(path.startsWith("/")){
                return path.substring(1);
            }
            return path;
        }catch(MalformedURLException e){
            System.err.println("잘못된 URL 주소입니다 \n" + e);
            return null;
        }
    }

    public ResponseEntity<StreamingResponseBody> fileDownload(ArticleDTO articleDTO){
        // S3 에서 객체 로드
        String path = findPath(articleDTO.getFileUrl());
        S3Object s3Object = downloadFile(path);
        S3ObjectInputStream s3is = s3Object.getObjectContent();
        ObjectMetadata metadata = s3Object.getObjectMetadata();

        // 헤더 구성
        HttpHeaders headers = new HttpHeaders();

        // Content-Type이 비어있는 경우
        String contentType = metadata.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        headers.setContentType(MediaType.parseMediaType(contentType));

        if (metadata.getContentLength() > 0) {
            headers.setContentLength(metadata.getContentLength());
        }

        // 파일명: 원본명 사용 (확장자 포함)
        String originalFileName = articleDTO.getFileTitle();

        //첨부 다운로드 헤더
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(originalFileName, StandardCharsets.UTF_8) // RFC5987
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body((StreamingResponseBody) out -> {
                    try (S3ObjectInputStream in = s3is; S3Object ignored = s3Object) {
                        in.transferTo(out);
                    }
                });
    }
}
