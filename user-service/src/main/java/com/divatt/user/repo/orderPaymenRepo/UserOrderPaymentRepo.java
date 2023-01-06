package com.divatt.user.repo.orderPaymenRepo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.divatt.user.entity.orderPayment.OrderPaymentEntity;

public interface UserOrderPaymentRepo extends MongoRepository<OrderPaymentEntity, Integer> {

	@Query(value = "{ $or: [ { 'order_id' : {$regex:?0,$options:'i'} }, { 'user_id' : {$regex:?0,$options:'i'} },"
			+ "{ 'payment_mode' : {$regex:?0,$options:'i'} },{ 'payment_details.razorpay_payment_id' : {$regex:?0,$options:'i'} },"
			+ "{ 'payment_details.razorpay_order_id' : {$regex:?0,$options:'i'} },{ 'payment_response.fee' : {$regex:?0,$options:'i'} },"
			+ "{ 'payment_response.contact' : {$regex:?0,$options:'i'} },{ 'payment_response.email' : {$regex:?0,$options:'i'} },"
			+ "{ 'payment_response.amount' : {$regex:?0,$options:'i'} },{ 'payment_response.method' : {$regex:?0,$options:'i'} },"
			+ "{ 'payment_response.status' : {$regex:?0,$options:'i'} },{ 'payment_status' : {$regex:?0,$options:'i'} },"
			+ "{ 'created_on' : {$regex:?0,$options:'i'} } ]}")
	Page<OrderPaymentEntity> Search(String sortKey, Pageable pageable);

	Optional<OrderPaymentEntity> findByOrderId(String orderId);
	
	@Query(value = "{'order_id' : ?0 }")
	List<OrderPaymentEntity> findByOrderIdList(String orderId);

	Optional<OrderPaymentEntity> findByUserId(Integer userId);

	@Query(value = "{ 'payment_details.razorpay_payment_id' : {$regex:?0,$options:'i'} }, { 'payment_details.razorpay_order_id' : {$regex:?0,$options:'i'}}")
	Optional<OrderPaymentEntity> findPaymentId(String PayID, String OrID);

}
