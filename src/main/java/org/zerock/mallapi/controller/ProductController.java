package org.zerock.mallapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.mallapi.dto.PageRequestDTO;
import org.zerock.mallapi.dto.PageResponseDTO;
import org.zerock.mallapi.dto.ProductDTO;
import org.zerock.mallapi.service.ProductService;
import org.zerock.mallapi.util.CustomFileUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CustomFileUtil fileUtil;

    @GetMapping("/{pno}")
    public ProductDTO read(@PathVariable("pno") Long pno) {
        return productService.get(pno);
    }

    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFileGET(@PathVariable String fileName) {
        return fileUtil.getFile(fileName);
    }

    @GetMapping("/list")
    public PageResponseDTO<ProductDTO> list(PageRequestDTO pageRequestDTO) {
        return productService.getList(pageRequestDTO);
    }

    @PostMapping("/")
    public Map<String, Long> register(ProductDTO productDTO) {
        List<MultipartFile> files = productDTO.getFiles();
        List<String> uploadFileNames = fileUtil.saveFiles(files);
        productDTO.setUploadFileNames(uploadFileNames);
        log.info(uploadFileNames);
        Long pno = productService.register(productDTO);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Map.of("RESULT", pno);
    }

    @PutMapping("/{pno}")
    public Map<String, String> modify(@PathVariable(name="pno") Long pno, ProductDTO productDTO) {

        productDTO.setPno(pno);

        // 수정하고자 하는 상품 DTO
        ProductDTO oldProductDTO = productService.get(pno);

        // 기존의 파일 이름 리스트
        List<String> oldFileNames = oldProductDTO.getUploadFileNames();

        // 수정을 위해 업로드해야 하는 파일 리스트
        List<MultipartFile> files = productDTO.getFiles();

        // 새로 업로드되어서 만들어진 파일 이름 리스트
        List<String> currentUploadFileNames = fileUtil.saveFiles(files);

        // 화면에서 변화 없이 계속 유지된 파일 이름 리스트
        List<String> uploadedFileNames = productDTO.getUploadFileNames();

        // 유지되는 파일들 + 새로 업로드된 파일들
        if (currentUploadFileNames != null && !currentUploadFileNames.isEmpty()) {
            uploadedFileNames.addAll(currentUploadFileNames);
        }

        productService.modify(productDTO);

        if (oldFileNames != null && !oldFileNames.isEmpty()) {
            // 예전 파일들 중에서 지워져야 하는 파일 이름 리스트
            List<String> removeFiles = oldFileNames
                    .stream()
                    .filter(fileName -> !uploadedFileNames.contains(fileName))
                    .collect(Collectors.toList());

            // 실제 파일 삭제
            fileUtil.deleteFiles(removeFiles);
        }
        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/{pno}")
    public Map<String, String> remove(@PathVariable(name="pno") Long pno) {
        List<String> oldFileNames = productService.get(pno).getUploadFileNames();
        productService.remove(pno);
        fileUtil.deleteFiles(oldFileNames);
        return Map.of("RESULT", "SUCCESS");
    }

}
