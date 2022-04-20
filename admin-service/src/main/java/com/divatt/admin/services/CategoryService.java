package com.divatt.admin.services;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.divatt.admin.entity.GlobalResponse;
import com.divatt.admin.entity.category.CategoryEntity;
import com.divatt.admin.exception.CustomException;
import com.divatt.admin.repo.CategoryRepo;

@Service
public class CategoryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CategoryService.class);

	@Autowired
	CategoryRepo categoryRepo;

	@Autowired
	SequenceGenerator sequenceGenerator;

	public GlobalResponse postCategoryDetails(@RequestBody CategoryEntity categoryEntity) {
		LOGGER.info("Inside - CategoryService.postCategoryDetails()");

		try {

			Optional<CategoryEntity> findByCategoryName = categoryRepo
					.findByCategoryName(categoryEntity.getCategoryName());
			if (findByCategoryName.isPresent()) {
				throw new CustomException("Category already exist!");
			} else {
				CategoryEntity filterCatDetails = new CategoryEntity();

				filterCatDetails.setId(sequenceGenerator.getNextSequence(CategoryEntity.SEQUENCE_NAME));
				filterCatDetails.setCategoryName(categoryEntity.getCategoryName());
				filterCatDetails.setCategoryDescription(categoryEntity.getCategoryDescription());
				filterCatDetails.setCategoryImage(categoryEntity.getCategoryImage());
				filterCatDetails.setCreatedBy(categoryEntity.getCreatedBy());
				filterCatDetails.setCreatedOn(new Date());
				filterCatDetails.setLevel(categoryEntity.getLevel());
				filterCatDetails.setParentId("0");
				filterCatDetails.setIsActive(true);
				filterCatDetails.setIsDeleted(false);

				categoryRepo.save(filterCatDetails);
				return new GlobalResponse("SUCCESS", "Category added succesfully", 200);
			}

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	public Map<String, Object> getCategoryDetails(int page, int limit, String sort, String sortName, Boolean isDeleted,
			String keyword, Optional<String> sortBy) {
		try {
			int CountData = (int) categoryRepo.count();
			Pageable pagingSort = null;
			if (limit == 0) {
				limit = CountData;
			}

			if (sort.equals("ASC")) {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.ASC, sortBy.orElse(sortName));
			} else {
				pagingSort = PageRequest.of(page, limit, Sort.Direction.DESC, sortBy.orElse(sortName));
			}

			Page<CategoryEntity> findAll = null;

			if (keyword.isEmpty()) {
				findAll = categoryRepo.findByIsDeletedAndParentId(isDeleted, "0", pagingSort);
			} else {
				findAll = categoryRepo.Search(keyword, isDeleted, "0", pagingSort);

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
				throw new CustomException("Category Not Found!");
			} else {
				return response;
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public Optional<CategoryEntity> viewCategoryDetails(Integer catId) {
		try {
			Optional<CategoryEntity> findById = this.categoryRepo.findById(catId);
			if (!(findById.isPresent())) {
				throw new CustomException("Category not found!");
			} else {
				return findById;
			}

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponse putCategoryDetailsService(CategoryEntity categoryEntity, Integer catId) {

		try {
			Optional<CategoryEntity> findByCategoryRow = categoryRepo.findById(catId);

			if (!findByCategoryRow.isPresent()) {
				throw new CustomException("Category not found!");
			} else {
				CategoryEntity filterCatDetails = findByCategoryRow.get();

				filterCatDetails.setCategoryName(categoryEntity.getCategoryName());
				filterCatDetails.setCategoryDescription(categoryEntity.getCategoryDescription());
				filterCatDetails.setCategoryImage(categoryEntity.getCategoryImage());
				filterCatDetails.setCreatedBy(categoryEntity.getCreatedBy());
				filterCatDetails.setCreatedOn(new Date());
				filterCatDetails.setLevel(categoryEntity.getLevel());
				filterCatDetails.setParentId("0");
				filterCatDetails.setIsActive(true);
				filterCatDetails.setIsDeleted(false);
				categoryRepo.save(filterCatDetails);

				return new GlobalResponse("SUCCESS", "Category updated successfully", 200);
			}

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	public GlobalResponse putCategoryDeleteService(Integer CatId) {
		try {
			Optional<CategoryEntity> findById = categoryRepo.findById(CatId);
			CategoryEntity filterCatDetails = findById.get();
			if (!findById.isPresent()) {
				throw new CustomException("Category not found!");
			} else {
				filterCatDetails.setIsDeleted(true);
				filterCatDetails.setCreatedOn(new Date());
				categoryRepo.save(filterCatDetails);

				return new GlobalResponse("SUCCESS", "Category deleted successfully", 200);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponse putCategoryStatusService(Integer CatId) {
		try {

			Optional<CategoryEntity> findById = categoryRepo.findById(CatId);
			CategoryEntity filterCatDetails = findById.get();

			if (filterCatDetails.getId() == null) {
				throw new CustomException("Category not found!");

			} else {
				Boolean isStatus = null;
				String message=null;
				if (filterCatDetails.getIsActive() == false) {
					isStatus = true;
					message= "actived";
				} else {
					isStatus = false;
					message= "inactive";
				}

				filterCatDetails.setIsActive(isStatus);
				filterCatDetails.setCreatedOn(new Date());
				categoryRepo.save(filterCatDetails);

				return new GlobalResponse("SUCCESS", "Status "+message+" successfully", 200);
			}
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public GlobalResponse putCategoryMulDeleteService(List<Integer> CateID) {
		try {
			for (Integer CateIdRowId : CateID) {

				Optional<CategoryEntity> findById = categoryRepo.findById(CateIdRowId);
				CategoryEntity filterCatDetails = findById.get();

				if (filterCatDetails.getId() != null) {
					filterCatDetails.setIsDeleted(true);
					filterCatDetails.setCreatedOn(new Date());
					categoryRepo.save(filterCatDetails);
				}
			}
			return new GlobalResponse("SUCCESS", "Category deleted successfully", 200);

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

}