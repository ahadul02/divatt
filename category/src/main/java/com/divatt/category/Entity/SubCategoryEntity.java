package com.divatt.category.Entity;

import java.util.Date;

import javax.validation.constraints.NotEmpty;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

@Document(collection = "tbl_categories")
public class SubCategoryEntity {

		@Id
//		@Field("_id")
//		  @JsonIgnore
		  
		private String id;

//		@Field("my_object_id")
//		private Long myObjectId;
		@NotEmpty(message = "Category Name is Required")
		private String categoryName;
		@NotEmpty(message = "Category Descrition is Required")
		private String categoryDescrition;
		@NotEmpty(message = "Category Image is Required")
		private String categoryImage;
		private Integer level;
		private Boolean isActive;
		private Boolean isDeleted;
		@NotEmpty(message = "Parent Category Name is Required")
		private String parentId;
		@JsonFormat(shape = Shape.STRING,pattern = "yyyy/MM/dd")
		private Date createdOn;
		private String createdBy;
		
		
		public SubCategoryEntity() {
			super();
			// TODO Auto-generated constructor stub
		}


		@Override
		public String toString() {
			return "SubCategoryEntity [id=" + id + ", categoryName=" + categoryName + ", categoryDescrition="
					+ categoryDescrition + ", categoryImage=" + categoryImage + ", level=" + level + ", isActive="
					+ isActive + ", isDeleted=" + isDeleted + ", parentId=" + parentId + ", createdOn=" + createdOn
					+ ", createdBy=" + createdBy + "]";
		}


		public String getId() {
			return id;
		}


		public void setId(String id) {
			this.id = id;
		}


		public String getCategoryName() {
			return categoryName;
		}


		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}


		public String getCategoryDescrition() {
			return categoryDescrition;
		}


		public void setCategoryDescrition(String categoryDescrition) {
			this.categoryDescrition = categoryDescrition;
		}


		public String getCategoryImage() {
			return categoryImage;
		}


		public void setCategoryImage(String categoryImage) {
			this.categoryImage = categoryImage;
		}


		public Integer getLevel() {
			return level;
		}


		public void setLevel(Integer level) {
			this.level = level;
		}


		public Boolean getIsActive() {
			return isActive;
		}


		public void setIsActive(Boolean isActive) {
			this.isActive = isActive;
		}


		public Boolean getIsDeleted() {
			return isDeleted;
		}


		public void setIsDeleted(Boolean isDeleted) {
			this.isDeleted = isDeleted;
		}


		public String getParentId() {
			return parentId;
		}


		public void setParentId(String parentId) {
			this.parentId = parentId;
		}


		public Date getCreatedOn() {
			return createdOn;
		}


		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}


		public String getCreatedBy() {
			return createdBy;
		}


		public void setCreatedBy(String createdBy) {
			this.createdBy = createdBy;
		}
		
		
		
}