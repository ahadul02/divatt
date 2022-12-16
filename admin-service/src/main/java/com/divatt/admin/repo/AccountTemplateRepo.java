package com.divatt.admin.repo;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import com.divatt.admin.entity.AccountEntity;
import com.divatt.admin.entity.DesignerReturnAmount;
import com.divatt.admin.entity.GovtCharge;
import com.divatt.admin.entity.OrderDetails;
import com.divatt.admin.entity.ServiceCharge;

@Repository
public class AccountTemplateRepo {

	@Autowired
	private MongoTemplate mongoTemplate;

	private static final Logger LOGGER = LoggerFactory.getLogger(AccountTemplateRepo.class);

	public Page<AccountEntity> AccountSearchByKeywords(String keywords, Pageable pagingSort) {

		Query query = new Query();
		Query countQuery = new Query();
		query.with(Sort.by(Sort.Direction.DESC, "datetime"));

		query.addCriteria(new Criteria().orOperator(Criteria.where("service_charge.date").regex(keywords),
				Criteria.where("service_charge.designer_invoice_id").regex(keywords),
				Criteria.where("service_charge.status").regex(keywords),
				Criteria.where("service_charge.remarks").regex(keywords),
				Criteria.where("designer_details.designer_name").regex(keywords),
				Criteria.where("designer_details.gst_in").regex(keywords),
				Criteria.where("designer_details.pan").regex(keywords),
				Criteria.where("designer_details.mobile").regex(keywords),
				Criteria.where("designer_details.address").regex(keywords),

				Criteria.where("order_details").elemMatch(Criteria.where("datetime").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("product_sku").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("size").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("tax_type").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("order_status").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("delivery_datetime").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("remarks").regex(keywords)),
				Criteria.where("order_details").elemMatch(Criteria.where("order_id").regex(keywords)),

				Criteria.where("govt_charge").elemMatch(Criteria.where("designer_invoice_id").regex(keywords)),
				Criteria.where("govt_charge").elemMatch(Criteria.where("status").regex(keywords)),
				Criteria.where("govt_charge").elemMatch(Criteria.where("datetime").regex(keywords)),
				Criteria.where("govt_charge").elemMatch(Criteria.where("remarks").regex(keywords)),

				Criteria.where("designer_return_amount").elemMatch(Criteria.where("datetime").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("status").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("order_id").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("product_sku").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("size").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("tax_type").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("updated_datetime").regex(keywords)),
				Criteria.where("designer_return_amount").elemMatch(Criteria.where("remarks").regex(keywords))

		));

		countQuery.with(pagingSort);
		long total = mongoTemplate.count(countQuery, AccountEntity.class);

		List<AccountEntity> find = mongoTemplate.find(query, AccountEntity.class);
		Page<AccountEntity> dataPageable = new PageImpl<AccountEntity>(find, pagingSort, total);

		return dataPageable;
	}

	public AccountEntity update(long accountId, AccountEntity findByRows) {

		AccountEntity findOne = mongoTemplate.findOne(Query.query(Criteria.where("_id").is(accountId)), AccountEntity.class);

		findOne.setDatetime(findByRows.getDatetime());

		findOne.getService_charge().setCgst(findByRows.getService_charge().getCgst());
		findOne.getService_charge().setSgst(findByRows.getService_charge().getSgst());
		findOne.getService_charge().setIgst(findByRows.getService_charge().getIgst());
		findOne.getService_charge().setFee(findByRows.getService_charge().getFee());
		findOne.getService_charge().setTcs(findByRows.getService_charge().getTcs());
		findOne.getService_charge().setTotal_amount(findByRows.getService_charge().getTotal_amount());
		findOne.getService_charge().setTotal_tax(findByRows.getService_charge().getTotal_tax());
		findOne.getService_charge().setRate(findByRows.getService_charge().getRate());
		findOne.getService_charge().setTcs_rate(findByRows.getService_charge().getTcs_rate());
		findOne.getService_charge().setUnits(findByRows.getService_charge().getUnits());
		findOne.getService_charge().setDate(findByRows.getService_charge().getDate());
		findOne.getService_charge().setDesigner_invoice_id(findByRows.getService_charge().getDesigner_invoice_id());
		findOne.getService_charge().setRemarks(findByRows.getService_charge().getRemarks());
		findOne.getService_charge().setStatus(findByRows.getService_charge().getStatus());

		findOne.getAdmin_details().setAddress(findByRows.getAdmin_details().getAddress());
		findOne.getAdmin_details().setGst_in(findByRows.getAdmin_details().getGst_in());
		findOne.getAdmin_details().setMobile(findByRows.getAdmin_details().getMobile());
		findOne.getAdmin_details().setName(findByRows.getAdmin_details().getName());
		findOne.getAdmin_details().setPan(findByRows.getAdmin_details().getPan());
		findOne.getAdmin_details().setAdmin_id(findByRows.getAdmin_details().getAdmin_id());

		findOne.getDesigner_details().setAddress(findByRows.getDesigner_details().getAddress());
		findOne.getDesigner_details().setGst_in(findByRows.getDesigner_details().getGst_in());
		findOne.getDesigner_details().setMobile(findByRows.getDesigner_details().getMobile());
		findOne.getDesigner_details().setDesigner_name(findByRows.getDesigner_details().getDesigner_name());
		findOne.getDesigner_details().setPan(findByRows.getDesigner_details().getPan());
		findOne.getDesigner_details().setDesigner_id(findByRows.getDesigner_details().getDesigner_id());

		GovtCharge govtCharge = new GovtCharge();
		ArrayList<GovtCharge> govtChargeList = new ArrayList<>();

		findByRows.getGovt_charge().forEach(value -> {
			govtCharge.setCgst(value.getCgst());
			govtCharge.setSgst(value.getSgst());
			govtCharge.setIgst(value.getIgst());
			govtCharge.setFee(value.getFee());
			govtCharge.setTcs(value.getTcs());
			govtCharge.setTotal_amount(value.getTotal_amount());
			govtCharge.setTotal_tax(value.getTotal_tax());
			govtCharge.setRate(value.getRate());
			govtCharge.setTcs_rate(value.getTcs_rate());
			govtCharge.setUnits(value.getUnits());
			govtCharge.setDatetime(value.getDatetime());
			govtCharge.setDesigner_invoice_id(value.getDesigner_invoice_id());
			govtCharge.setDatetime(value.getDatetime());
			govtCharge.setRemarks(value.getRemarks());
			govtCharge.setStatus(value.getStatus());
			govtCharge.setUpdated_by(value.getUpdated_by());
			govtCharge.setUpdated_datetime(value.getUpdated_datetime());

			govtChargeList.add(govtCharge);
		});
		findOne.setGovt_charge(govtChargeList);

		OrderDetails orderDetails = new OrderDetails();
		ArrayList<OrderDetails> orderDetailsList = new ArrayList<>();

		findByRows.getOrder_details().forEach(value -> {

			orderDetails.setDatetime(value.getDatetime());
			orderDetails.setDelivery_datetime(value.getDelivery_datetime());
			orderDetails.setInvoice_id(value.getInvoice_id());
			orderDetails.setOrder_id(value.getOrder_id());
			orderDetails.setOrder_status(value.getOrder_status());
			orderDetails.setProduct_sku(value.getProduct_sku());
			orderDetails.setRemarks(value.getRemarks());
			orderDetails.setSize(value.getSize());
			orderDetails.setTax_type(value.getTax_type());
			orderDetails.setDesigner_id(value.getDesigner_id());
			orderDetails.setDiscount(value.getDiscount());
			orderDetails.setHsn_amount(value.getHsn_amount());
			orderDetails.setHsn_cgst(value.getHsn_cgst());
			orderDetails.setHsn_igst(value.getHsn_igst());
			orderDetails.setHsn_sgst(value.getHsn_sgst());
			orderDetails.setHsn_rate(value.getHsn_rate());
			orderDetails.setMrp(value.getMrp());
			orderDetails.setProduct_id(value.getProduct_id());
			orderDetails.setSales_price(value.getSales_price());
			orderDetails.setTotal_tax(value.getTotal_tax());
			orderDetails.setUnits(value.getUnits());
			orderDetails.setUser_id(value.getUser_id());

			orderDetailsList.add(orderDetails);
		});
		findOne.setOrder_details(orderDetailsList);

		DesignerReturnAmount designerReturnAmount = new DesignerReturnAmount();
		ArrayList<DesignerReturnAmount> DesignerReturnAmountList = new ArrayList<>();

		findByRows.getDesigner_return_amount().forEach(value -> {

			designerReturnAmount.setDatetime(value.getDatetime());
			designerReturnAmount.setOrderd(value.getOrderd());
			designerReturnAmount.setProduct_sku(value.getProduct_sku());
			designerReturnAmount.setRemarks(value.getRemarks());
			designerReturnAmount.setSize(value.getSize());
			designerReturnAmount.setStatus(value.getStatus());
			designerReturnAmount.setTax_type(value.getTax_type());
			designerReturnAmount.setUpdated_by(value.getUpdated_by());
			designerReturnAmount.setUpdated_datetime(value.getUpdated_datetime());
			designerReturnAmount.setDesigner_id(value.getDesigner_id());
			designerReturnAmount.setDiscount(value.getDiscount());
			designerReturnAmount.setHsn_amount(value.getHsn_amount());
			designerReturnAmount.setHsn_cgst(value.getHsn_cgst());
			designerReturnAmount.setHsn_igst(value.getHsn_igst());
			designerReturnAmount.setHsn_sgst(value.getHsn_sgst());
			designerReturnAmount.setHsn_rate(value.getHsn_rate());
			designerReturnAmount.setMrp(value.getMrp());
			designerReturnAmount.setNet_payable_designer(value.getNet_payable_designer());
			designerReturnAmount.setProduct_id(value.getProduct_id());
			designerReturnAmount.setSales_price(value.getSales_price());
			designerReturnAmount.setTcs(value.getTcs());
			designerReturnAmount.setTotal_amount_received(value.getTotal_amount_received());
			designerReturnAmount.setTotal_tax_amount(value.getTotal_tax_amount());
			designerReturnAmount.setUnits(value.getUnits());

			DesignerReturnAmountList.add(designerReturnAmount);
		});
		findOne.setDesigner_return_amount(DesignerReturnAmountList);

		return mongoTemplate.save(findOne);

	}

}