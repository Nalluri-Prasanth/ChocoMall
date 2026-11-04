package com.chocomall.search.engine;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/** The searchable view of a published product. */
@Getter
@Setter
@NoArgsConstructor
@Document(indexName = "chocomall_product")
@Setting(shards = 1, replicas = 0)
public class ProductDoc {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Keyword)
    private String productSn;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String subTitle;

    @Field(type = FieldType.Text)
    private String keywords;

    @Field(type = FieldType.Keyword, index = false)
    private String pic;

    @Field(type = FieldType.Boolean)
    private boolean published;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Long)
    private Long chocolateTypeId;

    @Field(type = FieldType.Keyword)
    private String kind;

    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Double)
    private Double originalPrice;

    @Field(type = FieldType.Integer)
    private Integer sale;

    @Field(type = FieldType.Integer)
    private Integer sort;

    @Field(type = FieldType.Boolean)
    private boolean preorderOnly;

    @Field(type = FieldType.Boolean)
    private boolean buildABox;

    @Field(type = FieldType.Boolean)
    private boolean inStock;

    @Field(type = FieldType.Long)
    private List<Long> flavorIds;

    @Field(type = FieldType.Text)
    private List<String> flavorNames;

    @Field(type = FieldType.Long)
    private List<Long> allergenIds;

    @Field(type = FieldType.Keyword)
    private List<String> allergens;

    /** Only claims that staff verified. */
    @Field(type = FieldType.Keyword)
    private List<String> claims;
}
