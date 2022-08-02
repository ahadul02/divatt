package com.divatt.user.controller;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.io.Resource;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.divatt.user.entity.OrderAndPaymentGlobalEntity;
import com.divatt.user.entity.UserAddressEntity;
import com.divatt.user.entity.UserLoginEntity;
import com.divatt.user.entity.order.OrderDetailsEntity;
import com.divatt.user.entity.order.OrderSKUDetailsEntity;
import com.divatt.user.entity.orderPayment.OrderPaymentEntity;
import com.divatt.user.exception.CustomException;
import com.divatt.user.helper.JwtUtil;
import com.divatt.user.repo.OrderDetailsRepo;
import com.divatt.user.repo.UserAddressRepo;
import com.divatt.user.repo.UserLoginRepo;
import com.divatt.user.response.GlobalResponse;
import com.divatt.user.services.OrderAndPaymentService;
import com.divatt.user.services.SequenceGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.Base64.InputStream;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import springfox.documentation.spring.web.json.Json;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import org.apache.commons.codec.binary.Base64;


@RestController
@RequestMapping("/userOrder")
public class OrderAndPaymentContoller {

	@Autowired
	JavaMailSender mailSender;

	@Autowired
	private JwtUtil JwtUtil;

	@Autowired
	private OrderDetailsRepo orderDetailsRepo;

	@Autowired
	private OrderAndPaymentService orderAndPaymentService;

	@Autowired
	private SequenceGenerator sequenceGenerator;

	@Autowired
	private UserLoginRepo userLoginRepo;

	@Autowired
	private MongoOperations mongoOperations;
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderAndPaymentContoller.class);

	@PostMapping("/razorpay/create")
	public ResponseEntity<?> postRazorpayOrderCreate(@RequestHeader("Authorization") String token,
			@Valid @RequestBody OrderDetailsEntity orderDetailsEntity) {
		LOGGER.info("Inside - OrderAndPaymentContoller.postRazorpayOrderCreate()");

		try {
			String extractUsername = null;
			try {
				extractUsername = JwtUtil.extractUsername(token.substring(7));
			} catch (Exception e) {
				throw new CustomException("Unauthorized");
			}

			if (!userLoginRepo.findByEmail(extractUsername).isPresent()) {
				throw new CustomException("Unauthorized");
			}
			return this.orderAndPaymentService.postRazorpayOrderCreateService(orderDetailsEntity);

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@PostMapping("/payment/add")
	public ResponseEntity<?> postOrderPaymentDetails(@RequestHeader("Authorization") String token,
			@Valid @RequestBody OrderPaymentEntity orderPaymentEntity) {
		LOGGER.info("Inside - OrderAndPaymentContoller.postOrderPaymentDetails()");

		try {
			String extractUsername = null;
			try {
				extractUsername = JwtUtil.extractUsername(token.substring(7));
			} catch (Exception e) {
				throw new CustomException("Unauthorized");
			}

//			if (userLoginRepo.findByEmail(extractUsername).isPresent()) {
				return this.orderAndPaymentService.postOrderPaymentService(orderPaymentEntity);
//			}
			
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}
	@PostMapping("/orderSKUDetails/add")
	public ResponseEntity<?> postOrderSKUDetails(@RequestHeader("Authorization") String token, @Valid @RequestBody OrderSKUDetailsEntity orderSKUDetailsEntity) {
		LOGGER.info("Inside - OrderAndPaymentContoller.postOrderSKUDetails()");

		try {
			String extractUsername = null;
			try {
				extractUsername = JwtUtil.extractUsername(token.substring(7));
			} catch (Exception e) {
				throw new CustomException("Unauthorized");
			}
				return this.orderAndPaymentService.postOrderSKUService(orderSKUDetailsEntity);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@RequestMapping(value = { "/payment/list" }, method = RequestMethod.GET)
	public Map<String, Object> getOrderPaymentDetails(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "DESC") String sort,
			@RequestParam(defaultValue = "createdOn") String sortName, @RequestParam(defaultValue = "") String keyword,
			@RequestParam Optional<String> sortBy) {
		LOGGER.info("Inside - OrderAndPaymentContoller.getOrderPaymentDetails()");

		try {
			return this.orderAndPaymentService.getOrderPaymentService(page, limit, sort, sortName, keyword, sortBy);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@PostMapping("/add")
	public ResponseEntity<?> addOrder(@RequestHeader("Authorization") String token,
			@RequestBody OrderAndPaymentGlobalEntity orderAndPaymentGlobalEntity) {
		LOGGER.info("Inside - OrderAndPaymentContoller.addOrder()");

		try {

			Map<String, Object> map = new HashMap<>();
			String extractUsername = null;
			try {
				extractUsername = JwtUtil.extractUsername(token.substring(7));
			} catch (Exception e) {
				throw new CustomException("Unauthorized");
			}

			if (userLoginRepo.findByEmail(extractUsername).isPresent()) {

				OrderDetailsEntity orderDetailsEntity = orderAndPaymentGlobalEntity.getOrderDetailsEntity();
				
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
				SimpleDateFormat formatters = new SimpleDateFormat("dd/MM/yyyy");
				Date date = new Date();
				String format = formatter.format(date);
				String formatDate = formatters.format(date);

				OrderDetailsEntity OrderLastRow=orderDetailsRepo.findTopByOrderByIdDesc();
 
			    String InvNumber = String.format("%014d", OrderLastRow.getId());   

			    
				orderDetailsEntity.setId(sequenceGenerator.getNextSequence(OrderDetailsEntity.SEQUENCE_NAME));
				orderDetailsEntity.setInvoiceId("IV"+InvNumber);				
				orderDetailsEntity.setOrderId("OR" + System.currentTimeMillis());
				orderDetailsEntity.setBillingAddress(orderDetailsEntity.getBillingAddress());
				orderDetailsEntity.setOrderDate(formatDate);
				orderDetailsEntity.setOrderStatus(orderDetailsEntity.getOrderStatus());
				orderDetailsEntity.setDeliveryStatus(orderDetailsEntity.getDeliveryStatus());
				orderDetailsEntity.setUserId(orderDetailsEntity.getUserId());
				orderDetailsEntity.setShippingAddress(orderDetailsEntity.getShippingAddress());
				orderDetailsEntity.setDeliveryMode(orderDetailsEntity.getDeliveryMode());
				orderDetailsEntity.setDeliveryCheckUrl(orderDetailsEntity.getDeliveryCheckUrl());
				orderDetailsEntity.setShippingCharges(orderDetailsEntity.getShippingCharges());	
				orderDetailsEntity.setTaxAmount(orderDetailsEntity.getTaxAmount());	
				orderDetailsEntity.setDiscount(orderDetailsEntity.getDiscount());	
				orderDetailsEntity.setMrp(orderDetailsEntity.getMrp());	
				Query query= new Query();
				query.addCriteria(Criteria.where("id").is(orderDetailsEntity.getUserId()));
				UserLoginEntity userLoginEntity=mongoOperations.findOne(query, UserLoginEntity.class);

				OrderDetailsEntity OrderData = orderDetailsRepo.save(orderDetailsEntity);
				
				OrderPaymentEntity orderPaymentEntity = orderAndPaymentGlobalEntity.getOrderPaymentEntity();
				orderDetailsEntity.setId(sequenceGenerator.getNextSequence(OrderPaymentEntity.SEQUENCE_NAME));
				orderPaymentEntity.setOrderId(OrderData.getOrderId());
				orderPaymentEntity.setCreatedOn(new Date());

				postOrderPaymentDetails(token, orderPaymentEntity);
				
				List<OrderSKUDetailsEntity> orderSKUDetailsEntity = orderAndPaymentGlobalEntity.getOrderSKUDetailsEntity();
				for (OrderSKUDetailsEntity orderSKUDetailsEntityRow : orderSKUDetailsEntity) {
					
					orderSKUDetailsEntityRow.setId(sequenceGenerator.getNextSequence(OrderSKUDetailsEntity.SEQUENCE_NAME));
					orderSKUDetailsEntityRow.setOrderId(OrderData.getOrderId());
					orderSKUDetailsEntityRow.setCreatedOn(format);

				postOrderSKUDetails(token,orderSKUDetailsEntityRow);
				}
				
				map.put("orderId", OrderData.getOrderId());
				map.put("status", 200);
				map.put("message", "Order placed successfully");

				File createPdfSupplier = createPdfSupplier(orderDetailsEntity);
				sendEmailWithAttachment(
						extractUsername, "Order summary", "Hi " +userLoginEntity.getFirstName() + ""
								+ ",\n                           " + " Your order created successfully. ",
						false, createPdfSupplier);

				createPdfSupplier.delete();
			}

			return ResponseEntity.ok(map);

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@RequestMapping(value = { "/list" }, method = RequestMethod.GET)
	public Map<String, Object> getOrderDetails(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int limit, @RequestParam(defaultValue = "DESC") String sort,
			@RequestParam(defaultValue = "createdOn") String sortName, @RequestParam(defaultValue = "") String keyword,
			@RequestParam Optional<String> sortBy) {
		LOGGER.info("Inside - OrderAndPaymentContoller.getOrderDetails()");

		try {
			return this.orderAndPaymentService.getOrders(page, limit, sort, sortName, keyword, sortBy);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@GetMapping("/getOrder/{orderId}")
	public ResponseEntity<?> getOrderDetails(@PathVariable() String orderId) {

		LOGGER.info("Inside - OrderAndPaymentContoller.getOrderDetails()");

		try {
			return this.orderAndPaymentService.getOrderDetailsService(orderId);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@GetMapping("/getUserOrder/{userId}")
	public ResponseEntity<?> getOrderDetailsByuserId(@PathVariable() Integer userId) {

		LOGGER.info("Inside - OrderAndPaymentContoller.getOrderDetailsByuserId()");

		try {
			return this.orderAndPaymentService.getUserOrderDetailsService(userId);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}
	
	@PutMapping("/updateOrder/{orderId}")
	public GlobalResponse updateOrder(@RequestBody OrderDetailsEntity orderDetailsEntity,@PathVariable String orderId)
	{
		try {
			return this.orderAndPaymentService.orderUpdateService(orderDetailsEntity,orderId);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	@RequestMapping(value = { "/list/{designerId}" }, method = RequestMethod.GET)
	public Map<String, Object> getOrderByDesigner(@PathVariable int designerId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int limit,
			@RequestParam(defaultValue = "DESC") String sort, @RequestParam(defaultValue = "createdOn") String sortName,
			@RequestParam(defaultValue = "") String keyword, @RequestParam Optional<String> sortBy) {
		LOGGER.info("Inside - OrderAndPaymentContoller.getOrderByDesigner()");

		try {
			return this.orderAndPaymentService.getDesigerOrders(designerId, page, limit, sort, sortName, keyword,
					sortBy);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

//	@GetMapping("/invoice/{orderId}")
//	public GlobalResponse invoiceGenarater(@PathVariable String orderId) {
//		try {
//			return this.orderAndPaymentService.invoiceGenarator(orderId);
//		} catch (Exception e) {
//			throw new CustomException(e.getMessage());
//		}
//	}

	@PostMapping("/genpdf/order")
	File createPdfSupplier(@RequestBody OrderDetailsEntity orderDetailsEntity) throws IOException {
		System.out.println("ok");

		/* first, get and initialize an engine */
		VelocityEngine ve = new VelocityEngine();

		/* next, get the Template */
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		try {
			ve.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Template t = null;
		try {
			t = ve.getTemplate("templates/orderSummary.vm");
		} catch (Exception e) {
			e.printStackTrace();
		}

		VelocityContext context = new VelocityContext();
		context.put("orderDetailsEntity", orderDetailsEntity);
		StringWriter writer = new StringWriter();
		t.merge(context, writer);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		baos = generatePdf(writer.toString());

		try (OutputStream outputStream = new FileOutputStream("order-summary.pdf")) {
			baos.writeTo(outputStream);

		}

		return new File("order-summary.pdf");
	}

	public ByteArrayOutputStream generatePdf(String html) {

		String pdfFilePath = "";
		PdfWriter pdfWriter = null;

		// create a new document
		Document document = new Document();
		try {

			document = new Document();			
			document.addAuthor("Divatt");
			document.addCreationDate();
			document.addProducer();
			document.addCreator("Divatt");
			document.addTitle("Divatt");
			document.setPageSize(PageSize.LETTER);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfWriter.getInstance(document, baos);

			// open document
			document.open();

			XMLWorkerHelper xmlWorkerHelper = XMLWorkerHelper.getInstance();
			xmlWorkerHelper.getDefaultCssResolver(true);
			StringReader stringReader = new StringReader(html);
			xmlWorkerHelper.parseXHtml(pdfWriter, document, stringReader);
			// close the document
			document.close();
			System.out.println("PDF generated successfully");

			return baos;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public void sendEmailWithAttachment(String to, String subject, String body, Boolean enableHtml, File file) {

		try {

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setSubject(subject);
			helper.setFrom("no-reply@nitsolution.in");
			helper.setTo(to);
			helper.setText(body, enableHtml);
			helper.addAttachment("order-summary", file);
			mailSender.send(message);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}

	@GetMapping("/orderProductDetails/{orderId}")
	public Map<String, Object> getOrderproductDetails(@PathVariable String orderId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int limit,
			@RequestParam(defaultValue = "DESC") String sort, @RequestParam(defaultValue = "createdOn") String sortName,
			@RequestParam(defaultValue = "") String keyword, @RequestParam Optional<String> sortBy) {
		try {
			return this.orderAndPaymentService.getProductDetails(orderId, page, limit, sort, sortName, keyword, sortBy);
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	@GetMapping("/invoice/{orderId1}")
	File invGenarator(@PathVariable String orderId1) throws IOException {
//		System.out.println("ok");
		Query query= new Query();
		query.addCriteria(Criteria.where("orderId").is(orderId1));
		OrderDetailsEntity orderDetailsEntity=mongoOperations.findOne(query, OrderDetailsEntity.class);
		/* first, get and initialize an engine */
		VelocityEngine ve = new VelocityEngine();
 
		/* next, get the Template */
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		try {
			ve.init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Template t = null;
		try {
			t = ve.getTemplate("templates/orderSummary.vm");
		} catch (Exception e) {
			e.printStackTrace();
		}

		VelocityContext context = new VelocityContext();
		context.put("orderDetailsEntity", orderDetailsEntity);
		StringWriter writer = new StringWriter();
		t.merge(context, writer);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		baos =generatePdf (writer.toString());

		try (OutputStream outputStream = new FileOutputStream("order-summary.pdf")) {
			baos.writeTo(outputStream);

		}

		return new File("order-summary.pdf");
	}
	
	@GetMapping(value = "/api/files/system/{filename}", produces = "text/csv; charset=utf-8")
	@ResponseStatus(HttpStatus.OK)
	public Resource getFileFromFileSystem(@PathVariable String filename, HttpServletResponse response) {
		return orderAndPaymentService.getFileSystem(filename, response);
	}
 
	@GetMapping(value = "/api/files/classpath/{filename}", produces = "text/csv; charset=utf-8")
	@ResponseStatus(HttpStatus.OK)
	public Resource getFileFromClasspath(@PathVariable String filename, HttpServletResponse response) {
		return orderAndPaymentService.getClassPathFile(filename, response);
	}
	
	@PostMapping("/imageEncode")
	public ResponseEntity<?> ImageEncide(@RequestBody String ImageReq) throws Exception{
		//encode image to Base64 String
		String file = ImageReq.toString();
		System.out.println(ImageReq.toString());

//		JsonNode jn1 = new JsonNode(ImageReq.toString());
//		JSONObject object = jn1.getObject();
//		String file= object.get("image").toString();
		
		File f = new File("https://divatt-uat.s3.amazonaws.com/f1.avif"); 
		FileInputStream fis = new FileInputStream(f);
		byte byteArray[] = new byte[(int)f.length()];
		fis.read(byteArray);
		String imageString = Base64.encodeBase64String(byteArray);
		//decode Base64 String to image
//		FileOutputStream fos = new FileOutputStream("https://divatt-uat.s3.amazonaws.com/f1.avif"); //change path of image according to you
//		byteArray = Base64.decodeBase64(imageString);
//		fos.write(byteArray);
		fis.close();
//		fos.close();
		return ResponseEntity.ok(imageString);
		}

	@PutMapping("/cancelOrder/{orderId}/{productId}")
	public GlobalResponse cancelOrder(@PathVariable String orderId, @PathVariable Integer productId) {
		try {
			return this.orderAndPaymentService.cancelOrderService(orderId,productId);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	@GetMapping("/getOrderByInvoiceId/{invoiceId}")
	public Object getOrderByInvoiceId(@PathVariable String invoiceId) {
		try {
			return this.orderAndPaymentService.getOrderServiceByInvoiceId(invoiceId);
		}
		catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}
	
	
	@GetMapping("/getPdfByOrderId/{id}")
	public ResponseEntity<byte[]> getPdfByOrderId(@PathVariable String id) throws IOException {
		System.err.println(id);
		List<OrderDetailsEntity> findByOrderId = orderDetailsRepo.findByOrderId(id);
		System.out.println(findByOrderId.toString());
		OrderDetailsEntity orderDetailsEntity = findByOrderId.get(0);
		File createPdfSupplier = createPdfSupplier(orderDetailsEntity);
		
		FileInputStream fl = new FileInputStream(createPdfSupplier);
        byte[] arr = new byte[(int)createPdfSupplier.length()];
        fl.read(arr);
        fl.close();
		
		 HttpHeaders headers = new HttpHeaders();

		    headers.setContentType(MediaType.parseMediaType("application/pdf"));
		    String filename = "Order.pdf";

		    headers.add("content-disposition", "inline;filename=" + filename);
//		    headers.add("content-disposition", "attachment;filename=" + filename);

		    //headers.setContentDispositionFormData(filename, filename);
		    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
		    ResponseEntity<byte[]> response = new ResponseEntity<byte[]>(arr, headers, HttpStatus.OK);
		    return response;
	}
}
