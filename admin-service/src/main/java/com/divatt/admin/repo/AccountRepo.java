package com.divatt.admin.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.divatt.admin.entity.AccountEntity;

@Repository
public interface AccountRepo extends MongoRepository<AccountEntity, Long>{

	Page<AccountEntity> findById(long i, Pageable pagingSort);
	
	@Query(value = "{ 'service_charge.date': ?0 }")
	Page<AccountEntity> findByServiceChargeDate(long i, Pageable pagingSort);
	
	@Query(value = "{ 'designer_details.designer_id': ?0 }")
	Page<AccountEntity> findByDesignerId(long i, Pageable pagingSort);

//	@Query(value = "{ 'designer_details': { $elemMatch: { 'designer_id' : ?0 } }}")
//	Page<AccountEntity> findByDesignerId(long i, Pageable pagingSort);
	
//	@Query(value = "{ 'designer_details': { $elemMatch: { 'designer_id' : ?0 } }}")
//	Page<AccountEntity> searchByKeywords(String key, Pageable pagingSort);

	@Query(" {$or :[{'admin_details.admin_id': {$regex:?0,$options:'i'} }, {'admin_details.name': {$regex:?0,$options:'i'} } ]}")
	Page<AccountEntity> AccountSearchByKeywords(String pattern, Pageable pagingSort);

}
