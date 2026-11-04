package com.chocomall.model.repository;

import com.chocomall.model.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByProductSn(String productSn);

    List<Product> findByIdIn(Collection<Long> ids);

    List<Product> findByPublishedTrueAndNewStatusTrueOrderBySortDesc(Pageable pageable);

    List<Product> findByPublishedTrueAndRecommendStatusTrueOrderBySortDesc(Pageable pageable);

    List<Product> findByPublishedTrueAndCategoryIdInOrderBySortDesc(Collection<Long> categoryIds, Pageable pageable);

    List<Product> findByPublishedTrueAndBrandId(Long brandId);

    List<Product> findByPublishedTrueOrderBySaleDesc(Pageable pageable);

    long countByCategoryId(Long categoryId);

    long countByChocolateTypeId(Long chocolateTypeId);

    @Query("select count(p) from Product p join p.flavorIds f where f = :fid")
    long countByFlavor(@Param("fid") Long flavorId);

    @Query("select count(p) from Product p join p.allergenIds a where a = :aid")
    long countByAllergen(@Param("aid") Long allergenId);

    @Query("select p.stock from Product p where p.id = :id")
    Integer stockOf(@Param("id") Long id);

    long countByBrandId(Long brandId);

    @Query("select p from Product p where p.stock - p.lockStock <= p.lowStock order by p.stock - p.lockStock")
    List<Product> findLowStock();

    @Modifying
    @Query("update Product p set p.lockStock = p.lockStock + :units where p.id = :id and p.stock - p.lockStock >= :units")
    int lockStock(@Param("id") Long id, @Param("units") int units);

    @Modifying
    @Query("update Product p set p.lockStock = p.lockStock - :units where p.id = :id and p.lockStock >= :units")
    int releaseStock(@Param("id") Long id, @Param("units") int units);

    @Modifying
    @Query("update Product p set p.stock = p.stock - :units, p.lockStock = p.lockStock - :units, p.sale = p.sale + :sold where p.id = :id and p.lockStock >= :units and p.stock >= :units")
    int confirmSale(@Param("id") Long id, @Param("units") int units, @Param("sold") int sold);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :units where p.id = :id")
    int addStock(@Param("id") Long id, @Param("units") int units);

    @Modifying
    @Query("update Product p set p.stock = :stock where p.id = :id")
    int setStock(@Param("id") Long id, @Param("stock") int stock);
}
