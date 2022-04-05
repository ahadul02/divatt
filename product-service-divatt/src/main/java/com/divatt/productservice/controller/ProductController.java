package com.divatt.productservice.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.validation.Valid;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.divatt.productservice.entity.ProductMasterEntity;
import com.divatt.productservice.exception.CustomException;
import com.divatt.productservice.response.GlobalResponce;
import com.divatt.productservice.service.ProductService;
import com.divatt.productservice.service.ProductServiceImp;

@RestController
@RequestMapping("/product")
public class ProductController implements ProductServiceImp{

	@Autowired
	private ProductService productService;
	private static final Logger LOGGER=LoggerFactory.getLogger(ProductController.class);
	
	@GetMapping("/allList")
	public List<ProductMasterEntity> allList()
	{
		LOGGER.info("Inside- ProductController.allList()");
		try
		{
			return this.productService.allList();
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
	@PostMapping("/add")
	public GlobalResponce add(@Valid @RequestBody ProductMasterEntity productEntity)
	{
		try
		{
			LOGGER.info("Inside- ProductController.add()");
			return productService.addData(productEntity);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
	@GetMapping("/view/{productId}")
	public Optional<?> viewProductDetails(@PathVariable Integer productId)
	{
		try
		{
			LOGGER.info("Inside- ProductController.viewProductDetails()");
			return productService.productDetails(productId);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
	@PutMapping("/status/{productId}")
	public GlobalResponce changeStatus(@PathVariable Integer productId)
	{
		try
		{
			LOGGER.info("Inside- ProductController.changeStatus()");
			return this.productService.changeStatus(productId);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
	@PutMapping("/update/{productId}")
	public GlobalResponce updateProductData(@RequestBody ProductMasterEntity productMasterEntity,
											@PathVariable Integer productId)
	{
		try
		{
			LOGGER.info("Inside- ProductController.updateProductData()");
			return this.productService.updateProduct(productId,productMasterEntity);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
	@PutMapping("/delete/{productId}")
	public GlobalResponce productDelete(@PathVariable Integer productId)
	{
		try
		{
			LOGGER.info("Inside- ProductController.productDelete()");
			return this.productService.deleteProduct(productId);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
	@GetMapping("/list")
	public Map<String, Object> getCategoryDetails(			
			@RequestParam(defaultValue = "0") int page, 
			@RequestParam(defaultValue = "10") int limit,
			@RequestParam(defaultValue = "DESC") String sort, 
			@RequestParam(defaultValue = "createdOn") String sortName,
			@RequestParam(defaultValue = "false") Boolean isDeleted, 			
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam Optional<String> sortBy)
	{
		LOGGER.info("Inside - CategoryController.getListCategoryDetails()");
		try
		{
			return this.productService.getProductDetails(page, limit, sort, sortName, isDeleted, keyword,
					sortBy);
		}
		catch(Exception e)
		{
			throw new CustomException(e.getMessage());
		}
	}
//	@GetMapping("/main")
//	public String main()
//	{
//		try
//		{
//			return "hi";
//		}
//		catch(Exception e)
//		{
//			return e.getMessage();
//		}
//	}
}
