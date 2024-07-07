package org.zerock.mallapi.repository.search;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQuery;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.zerock.mallapi.domain.Product;
import org.zerock.mallapi.domain.ProductImage;
import org.zerock.mallapi.domain.QProduct;
import org.zerock.mallapi.domain.QProductImage;
import org.zerock.mallapi.dto.PageRequestDTO;
import org.zerock.mallapi.dto.PageResponseDTO;
import org.zerock.mallapi.dto.ProductDTO;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
public class ProductSearchImpl extends QuerydslRepositorySupport implements ProductSearch {

    public ProductSearchImpl() {
        super(Product.class);
    }

    @Override
    public PageResponseDTO<ProductDTO> searchList(PageRequestDTO pageRequestDTO) {

        log.info("searchList............");

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by("pno").descending());

        QProduct product = QProduct.product;
        QProductImage productImage = QProductImage.productImage;

        JPQLQuery<Product> query = from(product);
        query.leftJoin(product.imageList, productImage).fetchJoin();;
        query.where(productImage.ord.eq(0));

        Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, query);
        List<Tuple> productList = query.select(product, productImage).fetch();
        long count = query.fetchCount();

        List<ProductDTO> dtoList = productList.stream().map(tuple -> {
            Product p = tuple.get(product);
            ProductDTO dto = ProductDTO.builder()
                    .pno(p.getPno())
                    .pname(p.getPname())
                    .price(p.getPrice())
                    .pdesc(p.getPdesc())
                    .delFlag(p.isDelFlag())
                    .uploadFileNames(p.getImageList().stream()
                            .map(ProductImage::getFileName)
                            .collect(Collectors.toList()))
                    .build();
            return dto;
        }).collect(Collectors.toList());

        return PageResponseDTO.<ProductDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(count)
                .build();
    }

}
