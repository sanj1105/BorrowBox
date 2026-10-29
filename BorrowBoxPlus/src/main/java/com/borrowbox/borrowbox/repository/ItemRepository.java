package com.borrowbox.borrowbox.repository;

import com.borrowbox.borrowbox.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByCategory(String category);

    List<Item> findByNameContainingIgnoreCase(String keyword);

    // Pass "" (not null) for "no keyword" / "no category"
    @Query("""
           select i from Item i join fetch i.owner
           where lower(i.name) like lower(concat('%', :keyword, '%'))
             and (:category = '' or i.category = :category)
             and (:availableOnly = false or i.available = true)
           order by i.name
           """)
    List<Item> search(@Param("keyword") String keyword,
                      @Param("category") String category,
                      @Param("availableOnly") boolean availableOnly);

    @Query("select distinct i.category from Item i order by i.category")
    List<String> findAllCategories();
    @Query("select i from Item i join fetch i.owner order by i.name")
    List<Item> findAllWithOwner();
}