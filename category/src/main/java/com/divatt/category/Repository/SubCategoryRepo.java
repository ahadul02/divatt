package com.divatt.category.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.divatt.category.Entity.SubCategoryEntity;


@Repository
public interface SubCategoryRepo extends MongoRepository<SubCategoryEntity,String> {

	Optional<SubCategoryEntity> findByCategoryName(String categoryName);
	
//	@Query(value = "SELECT instObj FROM SubCategoryEntity instObj")
	Page<SubCategoryEntity> findByIsDeleted(Boolean isDeleted,Pageable pagingSort);
	
//	Page<SubCategoryEntity> findAll(Pageable pageable);

	

}