package com.divatt.user.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.divatt.user.entity.WishlistEntity;



@Repository
public interface WishlistRepo extends MongoRepository<WishlistEntity,Integer>{
	
	Optional<WishlistEntity> findByProductId(Integer ProductId);

}