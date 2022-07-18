package com.divatt.designer.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.divatt.designer.entity.CategoryEntity;
import com.divatt.designer.entity.ListProduct;
import com.divatt.designer.entity.OrderDetailsEntity;
import com.divatt.designer.entity.OrderEntity;
import com.divatt.designer.entity.ProductEntity;
import com.divatt.designer.entity.SendMail;
import com.divatt.designer.entity.UserList;
import com.divatt.designer.entity.UserProfile;
import com.divatt.designer.entity.UserProfileInfo;
import com.divatt.designer.entity.UserResponseEntity;
import com.divatt.designer.entity.product.ImagesEntity;
import com.divatt.designer.entity.product.ProductMasterEntity;
import com.divatt.designer.entity.product.StandardSOH;
import com.divatt.designer.entity.profile.DesignerLoginEntity;
import com.divatt.designer.entity.profile.DesignerProfileEntity;
import com.divatt.designer.exception.CustomException;
import com.divatt.designer.helper.CustomFunction;
import com.divatt.designer.repo.DesignerLoginRepo;
import com.divatt.designer.repo.DesignerProfileRepo;
import com.divatt.designer.repo.ProductRepository;
import com.divatt.designer.response.GlobalResponce;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.mashape.unirest.http.JsonNode;

import springfox.documentation.spring.web.json.Json;

@Service
public class ProductService {

	@Autowired
	private ProductRepository productRepo;

	@Autowired
	private SequenceGenerator sequenceGenarator;

	@Autowired
	private CustomFunction customFunction;

	@Autowired
	private DesignerLoginRepo designerLoginRepo;

	@Autowired
	private DesignerProfileRepo designerProfileRepo;

	@Autowired
	private MongoOperations mongoOperations;
	
	@Autowired
	private EmailThreadClass emailThreadClass;

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);

	public Map<String, Object> allList(int page, int limit, String sort, String sortName, Boolean isDeleted,
			String keyword, Optional<String> sortBy) {
		try {
			LOGGER.info("Inside - ProductService.allList()");
			List<ProductMasterEntity> productdata = productRepo.findAll();

			List<Integer> productId = productdata.stream().map(e -> e.getDesignerId()).collect(Collectors.toList());

			List<DesignerProfileEntity> profileData = new ArrayList<DesignerProfileEntity>();
			for (int i = 0; i < productId.size(); i++) {
				profileData.add(designerProfileRepo.findBydesignerId(Long.valueOf(productId.get(i))).get());
			}
			LinkedList<ListProduct> allData = new LinkedList<ListProduct>();
			ListProduct listProduct = new ListProduct();
			for (int i = 0; i < productId.size(); i++) {
				listProduct.setDesignerProfileEntity(profileData.get(i));
				listProduct.setProductMasterEntity(productdata.get(i));
				allData.add(listProduct);
			}

			if (allData.isEmpty()) {
				throw new CustomException("Product not found!");
			} else {

				int CountData = (int) allData.size();
				if (limit == 0) {
					limit = CountData;
				}

				Pageable pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
				Page<ListProduct> findAll = productRepo.findByDesignerIdIn(allData, pagingSort);

				int totalPage = findAll.getTotalPages() - 1;
				if (totalPage < 0) {
					totalPage = 0;
				}

				Map<String, Object> response = new HashMap<>();
				response.put("data", findAll.getContent());
				response.put("currentPage", findAll.getNumber());
				response.put("total", CountData);
				response.put("totalPage", totalPage);
				response.put("perPage", findAll.getSize());
				response.put("perPageElement", findAll.getNumberOfElements());

				if (findAll.getSize() <= 0) {
					throw new CustomException("Product not found!");
				} else {
					return response;
				}
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponce addData(ProductMasterEntity productData) {
		try {
			LOGGER.info("Inside-ProductService.addData()");
			Query query = new Query();
			query.addCriteria(Criteria.where("designer_id").is(productData.getDesignerId()));
			List<DesignerProfileEntity> designerProfileInfo = mongoOperations.find(query, DesignerProfileEntity.class);
			if (!designerProfileInfo.isEmpty()) {
				Query query2=new Query();
				query2.addCriteria(Criteria.where("profile_status").is("COMPLETED").and("_id").is(productData.getDesignerId()));
				List<DesignerLoginEntity> list=mongoOperations.find(query2, DesignerLoginEntity.class);
				if(!list.isEmpty())
				{
				Query query1 = new Query();
				query1.addCriteria(Criteria.where("designerId").is(productData.getDesignerId()).and("productName")
						.is(productData.getProductName()));
				List<UserProfile>userProfiles= new ArrayList<UserProfile>();
				List<ProductMasterEntity> productInfo = mongoOperations.find(query1, ProductMasterEntity.class);
				if (productInfo.isEmpty()) {
					RestTemplate restTemplate = new RestTemplate();
					ResponseEntity<String> categoryResponse = restTemplate.getForEntity(
							"http://localhost:8084/dev/category/view/" + productData.getCategoryId(), String.class);

					ResponseEntity<String> subcategoryResponse = restTemplate.getForEntity(
							"http://localhost:8084/dev/subcategory/view/" + productData.getSubCategoryId(),
							String.class);
					Query query3= new Query();
					productRepo.save(customFunction.filterDataEntity(productData));
					query3.addCriteria(Criteria.where("productName").is(productData.getProductName()));
					ProductMasterEntity newProductData=mongoOperations.findOne(query3, ProductMasterEntity.class);
					RestTemplate followerData= new RestTemplate();
					List<UserProfileInfo> userInfoList= new ArrayList<UserProfileInfo>();
					List<Long>userId= new ArrayList<Long>();
					
					ResponseEntity<String> forEntity = followerData.getForEntity("http://localhost:8082/dev/user/followedUserList/"+productData.getDesignerId(), String.class);
					String data=forEntity.getBody();
					//System.out.println(data);
					JSONArray jsonArray= new JSONArray(data);
					//System.out.println();
					Query query4= new Query();
					query4.addCriteria(Criteria.where("designer_name").is(productData.getProductName()));
					ProductMasterEntity newProductMasterEntity=mongoOperations.findOne(query4, ProductMasterEntity.class);
					for(int i=0;i<jsonArray.length();i++)
					{
						ObjectMapper objectMapper = new ObjectMapper();
						UserProfile readValue = objectMapper.readValue(jsonArray.get(i).toString(), UserProfile.class);
						RestTemplate userResponse= new RestTemplate();
						ResponseEntity<UserProfileInfo> userInfo= userResponse.getForEntity("http://localhost:8082/dev/user/getUserId/"+readValue.getUserId(), UserProfileInfo.class);
						//System.out.println(userInfo);
						userInfoList.add(userInfo.getBody());
					}
<<<<<<< HEAD
					emailThreadClass.emailThreadRun(userId);
					System.out.println("Main Class");
					for(int i=0;i<userId.size();i++)
					{
						ResponseEntity<UserProfileInfo> userProfileList= restTemplate.getForEntity("http://localhost:8080/dev/auth/info/USER/"+userId.get(i), UserProfileInfo.class);
						
					}
					productRepo.save(customFunction.filterDataEntity(productData));
=======
>>>>>>> 64bb9867e7a171bf6f37e84cac862d09f02bf4fc
					
					//char emailData[];
					ImagesEntity[] images=productData.getImages();
					String image1=images[0].getName();
					System.out.println(images[0].getName());
					String productImage="__ProductImage__";
					String productName="__ProductName__";
					String productDesc="__ProductDesc__";
					String productPrice="__ProductPrice__";
					String productDiscount="__ProductDiscount__";
					String productLink="__ProductLink__";
					//System.out.println(images[0].toString());
					Path filePath= Path.of("D:\\packageservice\\projects\\Divatt\\divatt-backend-updated-4-05-2022\\divatt-backend\\designer-service\\src\\main\\resources\\templates\\emailTemplate.txt");
					String ETBody = Files.readString(filePath);
				//	System.out.println(ETBody);
					String ETreplace = ETBody.replace(productImage,image1);
					String ETreplace1 = ETreplace.replace(productName,productData.getProductName());
					String ETreplace2 = ETreplace1.replace(productDesc,productData.getProductDescription());
					String ETreplace3 = ETreplace2.replace(productPrice,productData.getPrice().getIndPrice().getDealPrice().toString());
					String ETreplace4 = ETreplace3.replace(productDiscount,productData.getPrice().getIndPrice().getDiscountValue().toString());
					String ETreplace5 = ETreplace4.replace(productLink,"http://65.1.190.195/divatt/product-detail/"+newProductMasterEntity.getProductId().toString());
					
					//System.out.println(ETreplace4);
					for(int i=0;i<userInfoList.size();i++)
					{
						System.out.println(userInfoList.get(i).getEmail());
						SendMail sendMail= new SendMail();
						sendMail.setBody(ETreplace5);
						sendMail.setSenderMailId(userInfoList.get(i).getEmail());
						//System.out.println(userInfoList.get(i).getEmail());
						sendMail.setSubject("New Product Available");
						sendMail.setEnableHtml(true);
						sendMail.setFile(null);
						RestTemplate mailLink= new RestTemplate();
						ResponseEntity<String> mailStatus=mailLink.postForEntity("http://65.1.190.195:8080/dev/auth/sendMail", sendMail, String.class);
						System.out.println(mailStatus.getBody());
					}
					//productRepo.save(customFunction.filterDataEntity(productData));
					return new GlobalResponce("Success!!", "Product added successfully", 200);
				} else {
					return new GlobalResponce("Error!!", "Product already added", 400);
				}
			}
				else
				{
					return new GlobalResponce("Error!!", "Designer doucument is not appoved", 400);
				}
			}
				else {
				return new GlobalResponce("Error!!", "Designerid does not exist!!", 400);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public ProductEntity productDetails(Integer productId) {
		try {
			LOGGER.info("Inside-ProductService.productDetails()");
			if (productRepo.existsById(productId)) {
				LOGGER.info("Inside - ProductService.productDetails()");
				 ProductMasterEntity masterEntity= productRepo.findById(productId).get();
				 RestTemplate restTemplate= new RestTemplate();
				 ResponseEntity<Object> categoryEntity=restTemplate.getForEntity("http://localhost:8084/dev/category/view/"+masterEntity.getCategoryId(), Object.class);
				 ResponseEntity<Object> subCategoryEntity=restTemplate.getForEntity("http://localhost:8084/dev/subcategory/view/"+masterEntity.getSubCategoryId(), Object.class);
				 ProductEntity productData=customFunction.productFilter(masterEntity);
				 productData.setCategoryObject(categoryEntity.getBody());
				 productData.setSubCategoryObject(subCategoryEntity.getBody());
				 return productData;
				 
			} else {
				throw new CustomException("Product not found");
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponce changeStatus(Integer productId) {
		try {
			LOGGER.info("Inside - ProductService.changeStatus()");
			if (productRepo.existsById(productId)) {
				Boolean status;
				Optional<ProductMasterEntity> productData = productRepo.findById(productId);
				ProductMasterEntity productEntity = productData.get();
				if (productEntity.getIsActive().equals(true)) {
					status = false;
					productEntity.setIsActive(status);
					productEntity.setUpdatedBy(productEntity.getDesignerId().toString());
					productEntity.setUpdatedOn(new Date());
					productRepo.save(productEntity);
					return new GlobalResponce("Success", "Status Inactive successfully", 200);
				} else {
					status = true;
					productEntity.setIsActive(status);
					productEntity.setUpdatedBy(productEntity.getDesignerId().toString());
					productEntity.setUpdatedOn(new Date());
					productRepo.save(productEntity);
					return new GlobalResponce("Success", "Status Active successfully", 200);
				}

			} else {
				return new GlobalResponce("Bad request", "Product does not exist", 400);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponce updateProduct(Integer productId, ProductMasterEntity productMasterEntity) {
		try {
			LOGGER.info("Inside-ProductService.updateProduct()");
			if (productRepo.existsById(productId)) {
				Query query = new Query();
				query.addCriteria(Criteria.where("designerId").is(productMasterEntity.getDesignerId()));
				List<ProductMasterEntity> productInfo = mongoOperations.find(query, ProductMasterEntity.class);
				if (productInfo.isEmpty()) {
					throw new CustomException("Designer id can to be change");
				}
				productRepo.save(customFunction.updateFunction(productMasterEntity, productId));
				return new GlobalResponce("Success", "Product updated successfully", 200);
			} else {
				throw new CustomException("Product not found");
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponce deleteProduct(Integer productId) {

		try {
			LOGGER.info("Inside - ProductService.deleteProduct()");
			if (productRepo.existsById(productId)) {
				Boolean isDelete = false;
				Optional<ProductMasterEntity> productData = productRepo.findById(productId);
				ProductMasterEntity productEntity = productData.get();
				if (productEntity.getIsDeleted().equals(false)) {
					isDelete = true;
				} else {
					return new GlobalResponce("Bad request!!", "Product allReady deleted", 400);
				}
				productEntity.setIsDeleted(isDelete);
				productEntity.setUpdatedBy(productEntity.getDesignerId().toString());
				productEntity.setUpdatedOn(new Date());
				productRepo.save(productEntity);
				return new GlobalResponce("Success", "Deleted successfully", 200);
			} else {
				return new GlobalResponce("Bad request", "Product does not exist", 400);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	public Map<String, Object> getProductDetails(int page, int limit, String sort, String sortName, Boolean isDeleted,
			String keyword, Optional<String> sortBy) {
		try {
			int CountData = (int) productRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<ProductMasterEntity> findAll = null;

			if (keyword.isEmpty()) {
				findAll = productRepo.findByIsDeleted(isDeleted, pagingSort);
			} else {
				findAll = productRepo.Search(keyword, isDeleted, pagingSort);

			}

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}

			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());

			if (findAll.getSize() <= 1) {
				throw new CustomException("Product not found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	public Map<String, Object> allWishlistProductData(List<Integer> productIdList, Optional<String> sortBy, int page,
			String sort, String sortName, Boolean isDeleted, int limit) {
		try {
			LOGGER.info("Inside-ProductService.allWishlistProductData()");
			if (productIdList.isEmpty()) {
				throw new CustomException("Product not found!");
			} else {
				List<ProductMasterEntity> list = productRepo.findByProductIdIn(productIdList);

				int CountData = (int) list.size();
				if (limit == 0) {
					limit = CountData;
				}

				Pageable pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
				Page<ProductMasterEntity> findAll = productRepo.findByProductIdIn(productIdList, pagingSort);

				int totalPage = findAll.getTotalPages() - 1;
				if (totalPage < 0) {
					totalPage = 0;
				}

				Map<String, Object> response = new HashMap<>();
				response.put("data", findAll.getContent());
				response.put("currentPage", findAll.getNumber());
				response.put("total", CountData);
				response.put("totalPage", totalPage);
				response.put("perPage", findAll.getSize());
				response.put("perPageElement", findAll.getNumberOfElements());

				if (findAll.getSize() <= 0) {
					throw new CustomException("Product not found!");
				} else {
					return response;
				}
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public ResponseEntity<?> allCartProductData(List<Integer> productIdList) {
		try {
			LOGGER.info("Inside-ProductService.allWishlistProductData()");
			if (productIdList.isEmpty()) {
				throw new CustomException("Product not found!");
			} else {
				List<ProductMasterEntity> list = productRepo.findByProductIdIn(productIdList);
				
				if (list.size() <= 0) {
					throw new CustomException("Product not found!");
				} else {
					return ResponseEntity.ok(list);
				}
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public List<ProductMasterEntity> designerIdList(Integer designerId1) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("designerId").is(designerId1));
			List<ProductMasterEntity> productList = mongoOperations.find(query, ProductMasterEntity.class);

			return productList;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public Map<String, Object> designerIdListPage(Integer designerId, Optional<String> sortBy, int page, String sort,
			String sortName, Boolean isDeleted, int limit, String keyword) {
		try {
			int CountData = (int) productRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<ProductMasterEntity> findAll = null;

			if (keyword.isEmpty()) {
				findAll = productRepo.findByIsDeletedAndDesignerId(isDeleted, designerId, pagingSort);
			} else {
				findAll = productRepo.listDesignerProductsearch(keyword, isDeleted, designerId, pagingSort);

			}

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}

			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());

			if (findAll.getSize() <= 1) {
				throw new CustomException("Product not found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public List<ProductMasterEntity> getApproval() {
		try {
			return this.productRepo.findAll();
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public ResponseEntity<?> getListProduct() {
		long count = sequenceGenarator.getCurrentSequence(ProductMasterEntity.SEQUENCE_NAME);
		Random rd = new Random();

		List<ProductMasterEntity> findAll = productRepo.findByIsDeletedAndAdminStatusAndIsActive(false, "Approved",
				true);
		if (findAll.size() <= 15) {
			return ResponseEntity.ok(findAll);
		}
		List<ProductMasterEntity> productMasterEntity = new ArrayList<>();
		Boolean flag = true;
		while (flag) {
			int nextInt = rd.nextInt((int) count);
			for (ProductMasterEntity obj : findAll) {
				if (obj.getProductId() == nextInt) {
					productMasterEntity.add(obj);
				}
				if (productMasterEntity.size() > 14)
					flag = false;
			}
		}
		return ResponseEntity.ok(productMasterEntity);

	}

	public Map<String, Object> getProductDetailsPerStatus(String status, int page, int limit, String sort,
			String sortName, Boolean isDeleted, String keyword, Optional<String> sortBy) {

		try {
			int CountData = (int) productRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<ProductMasterEntity> findAll = null;
			Integer all = 0;
			Integer pending = 0;
			Integer approved = 0;
			Integer rejected = 0;

			all = productRepo.countByIsDeleted(isDeleted);
			pending = productRepo.countByIsDeletedAndAdminStatus(isDeleted, "Pending");
			approved = productRepo.countByIsDeletedAndAdminStatus(isDeleted, "Approved");
			rejected = productRepo.countByIsDeletedAndAdminStatus(isDeleted, "Rejected");

			if (keyword.isEmpty()) {

				if (status.equals("all")) {

					findAll = productRepo.findByIsDeleted(isDeleted, pagingSort);

				} else if (status.equals("pending")) {

					findAll = productRepo.findByIsDeletedAndAdminStatus(isDeleted, "Pending", pagingSort);

				} else if (status.equals("approved")) {

					findAll = productRepo.findByIsDeletedAndAdminStatus(isDeleted, "Approved", pagingSort);

				} else if (status.equals("rejected")) {

					findAll = productRepo.findByIsDeletedAndAdminStatus(isDeleted, "Rejected", pagingSort);
				}
			} else {

				if (status.equals("all")) {

					findAll = productRepo.SearchAndfindByIsDeleted(keyword, isDeleted,pagingSort);	

				} else if (status.equals("pending")) {

					findAll = productRepo.SearchAndfindByIsDeletedAndAdminStatus(keyword, isDeleted, "Pending",
							pagingSort);

				} else if (status.equals("approved")) {

					findAll = productRepo.SearchAppAndfindByIsDeletedAndAdminStatus(keyword, isDeleted, "Approved",
							pagingSort);

				} else if (status.equals("rejected")) {

					findAll = productRepo.SearchAndfindByIsDeletedAndAdminStatus(keyword, isDeleted, "Rejected",
							pagingSort);

				}

			}

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}

			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());
			response.put("all", all);
			response.put("pending", pending);
			response.put("approved", approved);
			response.put("rejected", rejected);

			if (findAll.getSize() <= 1) {
				throw new CustomException("Product not found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	public Map<String, Object> getDesignerProductListService(Integer page, Integer limit, Optional<String> sortBy,
			String sort, String sortName, String keyword, Boolean isDeleted) {
		try {
			int CountData = (int) productRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<ProductMasterEntity> findAll = null;

			if (keyword.isEmpty()) {
				findAll = productRepo.findByIsDeletedAndAdminStatus(isDeleted, "Approved", pagingSort);
			} else {
				findAll = productRepo.DesignerSearchfindByIsDeletedAndAdminStatus(keyword, isDeleted, "Approved",
						pagingSort);

			}

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}

			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());

			if (findAll.getSize() <= 1) {
				throw new CustomException("Product not found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public Map<String, Object> getPerDesignerProductListService(Integer page, Integer limit, Optional<String> sortBy,
			String sort, String sortName, String keyword, Boolean isDeleted, Integer designerId) {
		try {
			int CountData = (int) productRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<ProductMasterEntity> findAll = null;

			if (keyword.isEmpty()) {
				findAll = productRepo.findByIsDeletedAndAdminStatusAndDesignerId(isDeleted, "Approved", designerId,
						pagingSort);
			} else {
				findAll = productRepo.DesignerSearchfindByIsDeletedAndAdminStatusAndDesignerId(keyword, isDeleted,
						"Approved", designerId, pagingSort);

			}

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}

			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());

			if (findAll.getSize() <= 1) {
				throw new CustomException("Product not found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public List<ProductMasterEntity> UserDesignerProductList(Integer Id) {
		try {
			Query query = new Query();
			query.addCriteria(Criteria.where("designerId").is(Id).and("isActive").is(true).and("adminStatus").is("Approved"));
			List<ProductMasterEntity> productList = mongoOperations.find(query, ProductMasterEntity.class);

			if (productList.isEmpty()) {
				throw new CustomException("Product not found");
			}
			long count = sequenceGenarator.getCurrentSequence(ProductMasterEntity.SEQUENCE_NAME);
			Random rd = new Random();
			if (productList.size() < 15) {
				return productList;
			}
			List<ProductMasterEntity> productMasterEntity = new ArrayList<>();
			Boolean flag = true;
			while (flag) {
				int nextInt = rd.nextInt((int) count);
				for (ProductMasterEntity obj : productList) {
					if (obj.getProductId() == nextInt) {
						productMasterEntity.add(obj);
					}
					if (productMasterEntity.size() > 14)
						flag = false;
				}

			}
			return productMasterEntity;
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public ResponseEntity<?> getPerDesignerProductService(Integer designerId) {
		try {
			long count = sequenceGenarator.getCurrentSequence(ProductMasterEntity.SEQUENCE_NAME);
			Random rd = new Random();

			List<ProductMasterEntity> findAll = productRepo
					.findByDesignerIdAndIsDeletedAndAdminStatusAndIsActive(designerId, false, "Approved", true);
			if (findAll.size() <= 15) {
				return ResponseEntity.ok(findAll);
			}
			List<ProductMasterEntity> productMasterEntity = new ArrayList<>();
			Boolean flag = true;
			while (flag) {
				int nextInt = rd.nextInt((int) count);
				for (ProductMasterEntity obj : findAll) {
					if (obj.getProductId() == nextInt) {
						productMasterEntity.add(obj);
					}
					if (productMasterEntity.size() > 14)
						flag = false;
				}
			}
			return ResponseEntity.ok(productMasterEntity);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public ResponseEntity<?> ProductListByIdService(List<Integer> productIdList) {
		try {
//			System.out.println(productIdList);
			LOGGER.info("Inside-ProductService.ProductListByIdService()");
			if (productIdList.isEmpty()) {
				throw new CustomException("Product not found!");
			} else {
				List<ProductMasterEntity> list = productRepo.findByProductIdIn(productIdList);

				
				if (list.size() <= 0) {
					throw new CustomException("Product not found!");
				} else {
					return ResponseEntity.ok(list);
				}
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	

	public GlobalResponce adminApproval(Integer productId, ProductMasterEntity masterEntity) {
		try {
			masterEntity.setProductId(productId);
			productRepo.save(masterEntity);
			return new GlobalResponce("Successfull", "Product approved", 200);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponce multiDelete(List<Integer> productIdList) {
		try {
			List<ProductMasterEntity> productList=new ArrayList<ProductMasterEntity>();
			for(int i=0;i<productIdList.size();i++)
			{
				ProductMasterEntity productData=productRepo.findById(productIdList.get(i)).get();
				productList.add(productData);
			}
			List<ProductMasterEntity> collect = productList.stream().filter(e->e.getIsDeleted().equals(false)).collect(Collectors.toList());
			for(int i=0;i<collect.size();i++)
			{
				collect.get(i).setIsDeleted(true);
				collect.get(i).setIsActive(false);
			}
			productRepo.saveAll(collect);
			//System.out.println(collect);
			return new GlobalResponce("Success", "The products are deleted successfully", 200);
			
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}


	public GlobalResponce stockClearenceService(List<OrderEntity> orderEntities)
	{
		try {
			for (int i=0;i<orderEntities.size();i++) {
				int productId=orderEntities.get(i).getProductId();
				int productQty=orderEntities.get(i).getUnits();
				String productSize=orderEntities.get(i).getSize();
				List<StandardSOH> updatedSOH=new ArrayList<StandardSOH>();
				ProductMasterEntity productMasterEntity= productRepo.findById(productId).get();
				List<StandardSOH> standardSOHs=productMasterEntity.getStanderedSOH();
				for(int a=0;a<standardSOHs.size();a++)
				{
					StandardSOH standardSOH=new StandardSOH();
					//System.out.println(standardSOHs.get(a));
					//System.out.println(productQty);
					if(standardSOHs.get(a).getSizeType().equals(productSize)) {
						standardSOH.setSoh(standardSOHs.get(a).getSoh().intValue()-productQty);
						standardSOH.setOos(standardSOHs.get(a).getOos());
						standardSOH.setSizeType(productSize);
						standardSOH.setNotify(standardSOHs.get(a).getNotify());
						updatedSOH.add(standardSOH);
					}
					else{
						updatedSOH.add(standardSOHs.get(i));
					}
				}
				ProductMasterEntity masterEntity= productRepo.findById(productId).get();
				masterEntity.setStanderedSOH(standardSOHs);
				System.out.println(masterEntity);
				productRepo.save(masterEntity);
			}
			return new GlobalResponce("Success", "Stock cleared successfully",200);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public List<ProductMasterEntity> productListCategorySubcategory(String categoryName,
			String subcategoryName) {
		try {
		RestTemplate restTemplate= new RestTemplate();
		ResponseEntity<CategoryEntity> categoryEntity= restTemplate.getForEntity("http://localhost:8085/dev/category/", CategoryEntity.class);
		return null;	
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public List<ProductMasterEntity> viewProductByCategorySubcategoryService(String categoryName,
			String subCategoryName) {
		try {
			
			RestTemplate restTemplate= new RestTemplate();
			ResponseEntity<UserResponseEntity> userResponseEntity= restTemplate.getForEntity("http://localhost:8085/dev/category/viewByName/"+categoryName+"/"+subCategoryName, UserResponseEntity.class);
			System.out.println(userResponseEntity.getBody());
			int categoryIdvalue=userResponseEntity.getBody().getCategoryEntity().getId();
			if(userResponseEntity.getBody().getSubCategoryEntity().getParentId().equals("0"))
			{
				Query query= new Query();
				query.addCriteria(Criteria.where("categoryId").is(categoryIdvalue)
						.and("isDeleted").is(false).and("isActive").is(true)
						.and("adminStatus").is("Approved"));
				List<ProductMasterEntity> productMasterEntities=mongoOperations.find(query, ProductMasterEntity.class);
				return productMasterEntities;
			}
			int subcategoryIdvalue= userResponseEntity.getBody().getSubCategoryEntity().getId();
			Query query= new Query();
			query.addCriteria(Criteria.where("categoryId").is(categoryIdvalue)
					.and("subCategoryId").is(subcategoryIdvalue)
					.and("isDeleted").is(false).and("isActive").is(true)
					.and("adminStatus").is("Approved"));
			List<ProductMasterEntity> productMasterEntities=mongoOperations.find(query, ProductMasterEntity.class);
			return productMasterEntities;
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
//	public UserProfile userProfileConvert(Object obj) {
//
//	    if (obj instanceof UserProfile) {
//
//	    	UserProfile entity = (UserProfile) obj;
//	        // use entity instance as you need..
//	    	
//	    }
//	}

	public Map<String, Object> getProductReminderService(Integer page, Integer limit, Optional<String> sortBy,
			String sort, String sortName, String keyword, Boolean isDeleted) {
		try {
			int CountData = (int) productRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<ProductMasterEntity> findAll = null;
			List<ProductMasterEntity> findAlls = null;


			if (keyword.isEmpty()) {
				
				LocalDate date = LocalDate.now();

				findAll = productRepo.findNotify(date,pagingSort);
//				findAll = productRepo.findByStanderedSOHNotifySohGreaterThan(pagingSort);
				
			} else {
				findAll = productRepo.DesignerSearchfindByIsDeletedAndAdminStatus(keyword, isDeleted,
						"Approved", pagingSort);

			}

			int totalPage = findAll.getTotalPages() - 1;
			if (totalPage < 0) {
				totalPage = 0;
			}

			Map<String, Object> response = new HashMap<>();
			response.put("data", findAll.getContent());
			response.put("currentPage", findAll.getNumber());
			response.put("total", findAll.getTotalElements());
			response.put("totalPage", totalPage);
			response.put("perPage", findAll.getSize());
			response.put("perPageElement", findAll.getNumberOfElements());

			if (findAll.getSize() <= 1) {
				throw new CustomException("Product not found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
}
