package com.divatt.auth.controller;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.divatt.auth.entity.GlobalEntity;
import com.divatt.auth.entity.GlobalResponse;
import com.divatt.auth.entity.AdminLoginEntity;
import com.divatt.auth.entity.DesignerLoginEntity;
import com.divatt.auth.entity.LoginAdminData;
import com.divatt.auth.entity.LoginDesignerData;
import com.divatt.auth.entity.LoginUserData;
import com.divatt.auth.entity.PasswordResetEntity;
import com.divatt.auth.entity.SendMail;
import com.divatt.auth.entity.UserLoginEntity;
import com.divatt.auth.exception.CustomException;
import com.divatt.auth.helper.JwtUtil;
import com.divatt.auth.repo.AdminLoginRepository;
import com.divatt.auth.repo.DesignerLoginRepo;
import com.divatt.auth.repo.PasswordResetRepo;
import com.divatt.auth.repo.UserLoginRepo;
import com.divatt.auth.services.LoginUserDetails;
import com.divatt.auth.services.MailService;
import com.divatt.auth.services.SequenceGenerator;

import springfox.documentation.spring.web.json.Json;


@RestController
@SuppressWarnings("All")
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class EcomAuthController implements EcomAuthContollerMethod{
	
	@Autowired
	private MailService mailService;
	
	@Autowired
	private AdminLoginRepository loginRepository;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	private LoginUserDetails loginUserDetails;
	
	@Autowired
	private DesignerLoginRepo designerLoginRepo;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private PasswordResetRepo loginResetRepo;
	
	@Autowired
	private UserLoginRepo userLoginRepo;
	
	@Autowired
	private SequenceGenerator sequenceGenerator;

	Logger LOGGER = LoggerFactory.getLogger(EcomAuthController.class);
	
	@PostMapping("/login")
	public ResponseEntity<?> superAdminLogin(@RequestBody AdminLoginEntity loginEntity) {
		
	System.out.println(passwordEncoder.encode(loginEntity.getPassword()));
		
		LOGGER.info("Inside - EcomAuthController.superAdminLogin()");
		
		try {
			try {
				this.authenticationManager.authenticate(
						new UsernamePasswordAuthenticationToken(loginEntity.getEmail(), loginEntity.getPassword()));
			} catch (Exception e) {
				throw new CustomException(e.getMessage());
			}

			UserDetails vendor = this.loginUserDetails.loadUserByUsername(loginEntity.getEmail());

			String token = jwtUtil.generateToken(vendor);

			Optional<AdminLoginEntity> findByUserName = loginRepository.findByEmail(vendor.getUsername());
			if(findByUserName.isPresent()) {
				AdminLoginEntity loginEntityAfterCheck = findByUserName.get();
				loginEntityAfterCheck.setAuth_token(token);
				AdminLoginEntity save = loginRepository.save(loginEntityAfterCheck);
				
				if(save.equals(null)) {
					throw new CustomException("Data Not Save Try Again");
				}
				
				return ResponseEntity.ok(new LoginAdminData(token,findByUserName.get().getId(),findByUserName.get().getEmail() , findByUserName.get().getPassword(), "Login successful", 200,findByUserName.get().getRole()));
			}else {
				Optional<DesignerLoginEntity> findByUserNameDesigner = designerLoginRepo.findByEmail(vendor.getUsername());
				if(findByUserNameDesigner.isPresent()) {
					DesignerLoginEntity designerLoginEntity = findByUserNameDesigner.get();
					designerLoginEntity.setAuthToken(token);
					designerLoginRepo.save(designerLoginEntity);
					LoginDesignerData loginDesignerData = new LoginDesignerData(findByUserNameDesigner.get().getUid(),findByUserNameDesigner.get().getEmail() , findByUserNameDesigner.get().getPassword(), "Login successful",Stream.of("DESIGNER").map(SimpleGrantedAuthority::new).collect(Collectors.toList()) , 200,findByUserNameDesigner.get().getIsApproved(),findByUserNameDesigner.get().getIsProfileCompleated(),findByUserNameDesigner.get().getIsProfileSubmitted(),token);
					
					System.out.println("loginDesignerData   "+loginDesignerData.toString());
//					Json js = new Json(loginDesignerData.toString());
					return new ResponseEntity<>(loginDesignerData ,HttpStatus.OK);
				}else {
					
					Optional<UserLoginEntity> findByEmail = userLoginRepo.findByEmail(vendor.getUsername());
					if(findByEmail.isPresent()) {
						return ResponseEntity.ok(new LoginUserData(token,findByEmail.get().getuId(),findByEmail.get().getEmail(),findByEmail.get().getPassword(),"Login Successfully",Stream.of("USER").map(SimpleGrantedAuthority::new).collect(Collectors.toList()) , 200));
					}else {
						throw new CustomException("Internal Server Error");
					}
//											.ifPresentOrElse((value)->{}, ()->{throw new CustomException("Internal Server Error");});
					
				}
				
			}
			
			

			
		
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}	
	
	
	@PostMapping("/sendMail")
	public ResponseEntity<String> sendMail(@RequestBody() SendMail senderMailId) {
		LOGGER.info("Inside - EcomAuthController.sendMail()");
		
		try {
			mailService.sendEmail(senderMailId.getSenderMailId(), senderMailId.getSubject(),senderMailId.getBody(),senderMailId.isEnableHtml());
//			return new GlobalResponse("SUCCESS","Mail Send Successfully", 200);
			return new ResponseEntity<>("Mail Send Successfully" ,HttpStatus.OK);
			
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
	}


	
	@GetMapping("/mailForgotPasswordLink/{email}")
	@Description("Using This API You Can Send The Recovery Link to Email, and Using That Link User Can Recover The Password")
	public GlobalResponse mailForgotPasswordLink(@PathVariable("email") String email) {

		LOGGER.info("Inside - EcomAuthController.mailForgotPasswordLink()");

		try {

			UUID uuid = UUID.randomUUID();
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
			Date date = new Date();
			String format = formatter.format(date);
			Calendar calObj = Calendar.getInstance();
			calObj.setTime(date);
//			String forgotPasswordLinkCreateTime = calObj.get(Calendar.YEAR) + "-" + (calObj.get(Calendar.MONTH) + 1)+ "-" + calObj.get(Calendar.DATE) + "-" + (calObj.get(Calendar.HOUR) + "-"+ (calObj.get(Calendar.MINUTE) + "-" + (calObj.get(Calendar.SECOND))));
			String encodedString = Base64.getEncoder().encodeToString(format.getBytes());
			byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
			String decodedString = new String(decodedBytes);
			String forgotPasswordLink = "https://dapp.nichetechnosolution.com/resetpassword/" + uuid.toString() + "/"+ format;
			
			//** CHECKING THE EMAIL IS PREASENT IN DATABASE **//
			Optional<AdminLoginEntity> findByUserName = loginRepository.findByEmail(email);
			Optional<DesignerLoginEntity> findByUserNameDesigner = designerLoginRepo.findByEmail(email);
			Optional<UserLoginEntity> findByUserNameUser = userLoginRepo.findByEmail(email);
			if(!findByUserNameDesigner.isPresent() && !findByUserNameUser.isPresent() && !findByUserName.isPresent())
				throw new CustomException("UserName Not Present");
//			if (findByUserName.isPresent()) {
				PasswordResetEntity loginResetEntity = new PasswordResetEntity();
				Object id = null;
				try {
					 id = findByUserName.get().getId();	
					 loginResetEntity.setUser_type(findByUserName.get().getRole());
					 
				}catch(Exception e) {
					try {
						id = findByUserNameDesigner.get().getUid();
						loginResetEntity.setUser_type("DESIGNER");
					}catch(Exception j) {
						id = findByUserNameUser.get().getuId();
						loginResetEntity.setUser_type("USER");
					}
					 
				}
				 
				loginResetEntity.setUser_id(id);
				loginResetEntity.setPrtoken(uuid.toString() + "/" + format);
				loginResetEntity.setStatus("ACTIVE");
				loginResetEntity.setId(sequenceGenerator.getNextSequence(PasswordResetEntity.SEQUENCE_NAME));
				
				loginResetEntity.setEmail(email);
				Date dateObjForLinkCreateTime = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss")
						.parse(format);
				
				loginResetEntity.setCreated_on(dateObjForLinkCreateTime);
				//** SAVE THE DETAILS IN DATABASE **//
				PasswordResetEntity save = loginResetRepo.save(loginResetEntity);
				
				if (save.equals(null)) {
					throw new CustomException("Data Not Save Try Again");
				}else {
					//** SEND MAIL IF DETAILS SAVE IN DATABASE **//
					try {
						mailService.sendEmail( findByUserName.get().getEmail(),"Forgot Password Link","Hi " + findByUserName.get().getFirst_name() + " "+ findByUserName.get().getLast_name() +"<br>       This is Your Link For Reset Password " + forgotPasswordLink,false);
						
					}catch(Exception e) {
						try {
							mailService.sendEmail( findByUserNameDesigner.get().getEmail(),"Forgot Password Link","Hi " +findByUserNameDesigner.get().getEmail() +"<br>       This is Your Link For Reset Password " + forgotPasswordLink,false);
						}catch(Exception Z) {
							mailService.sendEmail( findByUserNameUser.get().getEmail(),"Forgot Password Link","Hi " +findByUserNameUser.get().getEmail() +"<br>       This is Your Link For Reset Password " + forgotPasswordLink,false);
						}
						
					}
					
				}
				
				return new GlobalResponse("SUCCESS","Mail Send Successfully", 200);
			

		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}

	}
	
	
	@PostMapping("/resetPassword/{link}/{linkTime}")
	@Description("After Got The Link in Mail, Using That link User Can Create New Password")
	public GlobalResponse resetPassword(@PathVariable("link") String link, @PathVariable("linkTime") String linkTime, @RequestBody GlobalEntity globalEntity) {
		
		LOGGER.info("Inside - EcomAuthController.resetPassword()");
		try {
			
			//** CHECK THE LINK IS PRESENT IN DB **//
			Optional<PasswordResetEntity> findByPrToken = loginResetRepo.findByPrtoken(link + "/" + linkTime);

			if (findByPrToken.isPresent()) {
				if(findByPrToken.get().getStatus().equals("ACTIVE")) {
					
					//** Create Current Time **//
					SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
					Date date = new Date();
					formatter.format(date);
					Calendar calObjForCurrentTime = Calendar.getInstance();
					calObjForCurrentTime.setTime(date);
					
//					byte[] forgotPasswordLinkCreateTimeByte = Base64.getDecoder().decode(linkTime);
//					String forgotPasswordLinkCreateTimeString = new String(forgotPasswordLinkCreateTimeByte);
					Calendar calObjForLinkCreateTime = Calendar.getInstance();
					Date dateObjForLinkCreateTime = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss")
	                        .parse(linkTime);
					calObjForLinkCreateTime.setTime(dateObjForLinkCreateTime);
					
					if(calObjForCurrentTime.get(Calendar.YEAR)==calObjForLinkCreateTime.get(Calendar.YEAR) && calObjForCurrentTime.get(Calendar.MONTH)==calObjForLinkCreateTime.get(Calendar.MONTH) && calObjForCurrentTime.get(Calendar.DATE)==calObjForLinkCreateTime.get(Calendar.DATE) && calObjForCurrentTime.get(Calendar.HOUR)==calObjForLinkCreateTime.get(Calendar.HOUR)  && calObjForLinkCreateTime.get(Calendar.MINUTE)<=calObjForCurrentTime.get(Calendar.MINUTE)  &&  calObjForCurrentTime.get(Calendar.MINUTE)<=calObjForLinkCreateTime.get(Calendar.MINUTE)+5) {
						
					}else {
						throw new CustomException("This Link is Expier");
					}
				//** FIND THE USER CORRESPONDING THE LINK IN LOGIN TABLE **//
					PasswordResetEntity loginResetEntity = findByPrToken.get();
					System.out.println("loginResetEntity.getUser_id() "+loginResetEntity.getUser_id());
					Optional<AdminLoginEntity> findById = loginRepository.findById((loginResetEntity.getUser_id()));
					Optional<AdminLoginEntity> findByEmail = loginRepository.findByEmail((loginResetEntity.getEmail()));
					if (findByEmail.isPresent() && findByEmail.get().getId().equals(findById.get().getId())) {
				//** CREATE NEW PASSWORD AND SAVE **//
						AdminLoginEntity loginEntity = findById.get();
						loginEntity.setPassword(passwordEncoder.encode(globalEntity.getNewPass()));
						AdminLoginEntity save = loginRepository.save(loginEntity);
						if(save.equals(null)) {
							throw new CustomException("Data Not Save Try Again");
						}else {
							loginResetEntity.setStatus("DEACTIVE");
							loginResetRepo.save(loginResetEntity);
							return new GlobalResponse("SUCCESS", "Password Generate Successfully", 200);
						}
					}else {
						
						Optional<DesignerLoginEntity> findByIdDesigner = designerLoginRepo.findById((loginResetEntity.getUser_id()));
						Optional<DesignerLoginEntity> findByEmailDesigner = designerLoginRepo.findByEmail((loginResetEntity.getEmail()));
						if(findByEmailDesigner.isPresent() && findByEmailDesigner.get().getUid().equals(findByIdDesigner.get().getUid())) {
							DesignerLoginEntity loginEntity = findByIdDesigner.get();
							loginEntity.setPassword(passwordEncoder.encode(globalEntity.getNewPass()));
							DesignerLoginEntity save = designerLoginRepo.save(loginEntity);
							if(save.equals(null)) {
								throw new CustomException("Data Not Save Try Again");
							}else {
								loginResetEntity.setStatus("DEACTIVE");
								loginResetRepo.save(loginResetEntity);
								return new GlobalResponse("SUCCESS", "Password Generate Successfully", 200);
							}
						}else {
							Optional<UserLoginEntity> findByUserId = userLoginRepo.findById((long)loginResetEntity.getUser_id());
							Optional<UserLoginEntity> findByUserEmail = userLoginRepo.findByEmail(loginResetEntity.getEmail());
							if(!findByUserEmail.isPresent()) 
								throw new CustomException("UserName is Not Present");
							UserLoginEntity loginEntity = findByUserId.get();
							loginEntity.setPassword(passwordEncoder.encode(globalEntity.getNewPass()));
							UserLoginEntity save = userLoginRepo.save(loginEntity);
							if(save.equals(null)) {
								throw new CustomException("Data Not Save Try Again");
							}else {
								loginResetEntity.setStatus("DEACTIVE");
								loginResetRepo.save(loginResetEntity);
								return new GlobalResponse("SUCCESS", "Password Generate Successfully", 200);
							}
						}
							
						
						
					}
					
				}else {
					throw new CustomException("Link is Already Used");
				}
			} else {
				throw new CustomException("This URL is Not Valid");
			}
			
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
		
	}

	@PostMapping("/changePassword")
	@Description("Using This Link user Can Change The Password After Login.")
	public GlobalResponse changePassword(@RequestBody GlobalEntity globalEntity,
			@RequestHeader(name = "Authorization") String token) {
		
		LOGGER.info("Inside - EcomAuthController.changePassword()");
		try {
			if (!token.equals(null)) {
				Optional<AdminLoginEntity> findByUserName = loginRepository.findByEmail(globalEntity.getUserName());
				Optional<DesignerLoginEntity> findByUserNameDesigner = designerLoginRepo.findByEmail(globalEntity.getUserName());
				Optional<UserLoginEntity> findByUserEmail = userLoginRepo.findByEmail(globalEntity.getUserName());
				if (!findByUserName.isPresent() && !findByUserNameDesigner.isPresent() && !findByUserEmail.isPresent()) 
					throw new CustomException("UserName Not Found");
				if (globalEntity.getUserName().equals(jwtUtil.extractUsername(token.substring(7)))) {
					try {
						if(passwordEncoder.matches(globalEntity.getOldPass(), findByUserName.get().getPassword()) ) {
							AdminLoginEntity loginEntity = findByUserName.get();
							loginEntity.setPassword(passwordEncoder.encode(globalEntity.getNewPass()));
							AdminLoginEntity save = loginRepository.save(loginEntity);
						}
					}catch(Exception e) {
						try {
							if(passwordEncoder.matches(globalEntity.getOldPass(), findByUserNameDesigner.get().getPassword()) ) {
								DesignerLoginEntity loginEntity = findByUserNameDesigner.get();
								loginEntity.setPassword(passwordEncoder.encode(globalEntity.getNewPass()));
								DesignerLoginEntity save = designerLoginRepo.save(loginEntity);
							}
							
						}catch(Exception Z) {
								if(!passwordEncoder.matches(globalEntity.getOldPass(), findByUserEmail.get().getPassword()) ) 
									throw new CustomException("Password not match");
								UserLoginEntity loginEntity = findByUserEmail.get();
								loginEntity.setPassword(passwordEncoder.encode(globalEntity.getNewPass()));
								UserLoginEntity save = userLoginRepo.save(loginEntity);
								
							
						}
						
					}							
							return new GlobalResponse("SUCCESS", "Password changed", 200);								
						
						
					} else {
						throw new CustomException("UserName Not Matched");
					}
			} else {
				throw new CustomException("Token not valid");
			}
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}

	}
	 
	
	@PostMapping("/user/add")
	public ResponseEntity<?> addUser(@Valid @RequestBody UserLoginEntity userLoginEntity,Errors error){
		LOGGER.info("Inside - EcomAuthController.addUser()");
		try {		
			if (error.hasErrors()) {
				throw new CustomException("Check The Fields");
			}
			userLoginRepo.findByEmail(userLoginEntity.getEmail()).ifPresentOrElse((data)->new CustomException("Email id is already Present"), null);
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
			Date date = new Date();
			formatter.format(date);
			userLoginEntity.setUsername(userLoginEntity.getEmail());
			userLoginEntity.setIsActive(true);
			userLoginEntity.setIsDeleted(false);
			userLoginEntity.setCreatedOn(date.toString());
			userLoginEntity.setProfilePic("Pic.jpg");
			userLoginEntity.setRegisterType("Self");
			userLoginRepo.save(userLoginEntity);
			return ResponseEntity.ok(new GlobalResponse("SUCCESS","Added Successfully",200));
		}catch(Exception e) {
			throw new CustomException(e.getMessage());
		}
		
		
	}
	
	
	@GetMapping("/admin/testapi")
	public String test() throws ClassNotFoundException {
		Class<?> forName = Class.forName("com.divatt.auth.controller.EcomAuthController");
		int MCount = forName.getDeclaredMethods().length;
		Class<?>[] interfaces = forName.getInterfaces();
		  Class<?> theFirstAndOnlyInterface = interfaces[0]; 
		
//		return theFirstAndOnlyInterface.getMethods().length;
		  return theFirstAndOnlyInterface.getMethods().length + " " + MCount;
	}
	
	
}
