package com.divatt.admin.repo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.divatt.admin.entity.category.SubCategoryEntity;




@Repository
public interface SubCategoryRepo extends MongoRepository<SubCategoryEntity,Integer> {

	Optional<SubCategoryEntity> findByCategoryName(String categoryName);
	
//	Page<SubCategoryEntity> findByIsDeleted(Boolean isDeleted,Pageable pagingSort);
	Page<SubCategoryEntity> findByIsDeletedAndParentIdNot(Boolean isDeleted,String parentId,Pageable pagingSort);
//	
//	@Query("{'id' : ?0 , 'parentId' : ?1}")
//	Page<SubCategoryEntity> findEmployeeByNameAndBand(String id, String parentId);
//	
	@Query(value = "{ $or: [ { 'categoryName' : {$regex:?0,$options:'i'} }, { 'categoryDescription' : {$regex:?0,$options:'i'} },{ 'isActive' : {$regex:?0,$options:'i'} },{ 'categoryImage' : {$regex:?0,$options:'i'} },{ 'createdOn' : {$regex:?0,$options:'i'} } ],$and: [ { 'isDeleted' : ?1 }]}")
    Page<SubCategoryEntity> SearchAndfindByIsDeletedAndParentIdNot(String sortKey, Boolean isDeleted, String parentId,Pageable pageable);

	

}