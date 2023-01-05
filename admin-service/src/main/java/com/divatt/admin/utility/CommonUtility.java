package com.divatt.admin.utility;

import java.text.DecimalFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.divatt.admin.constant.MessageConstant;
import com.divatt.admin.entity.AccountEntity;
import com.divatt.admin.entity.LoginEntity;
import com.divatt.admin.entity.PaymentCharges;
import com.divatt.admin.exception.CustomException;
import com.divatt.admin.helper.EmailSenderThread;
import com.divatt.admin.repo.LoginRepository;

@Component
public class CommonUtility {

	@Autowired
	private LoginRepository loginRepository;

	@Autowired
	private TemplateEngine templateEngine;

	@Autowired
	private RestTemplate restTemplate;

//	private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtility.class);

	public static double duoble(float f) {

		DecimalFormat df = new DecimalFormat("0.00");
		return Double.valueOf(df.format(f));

	}

	public void mailSendAccount(AccountEntity accountEntity) {
		try {
			LoginEntity findByRoleName = loginRepository.findByRoleName(MessageConstant.ADMIN_ROLES.getMessage());
			String email = findByRoleName.getEmail();
			String firstName = findByRoleName.getFirstName();
			String lastName = findByRoleName.getLastName();
			String name = firstName + " " + lastName;
			String gstIn = findByRoleName.getGstIn();
			String pan = findByRoleName.getPan();
			Context context = new Context();

			context.setVariable("name", name);
			context.setVariable("gstIn", gstIn);
			context.setVariable("pan", pan);
			context.setVariable("email", email);
			String htmlContent = templateEngine.process("adminAccountUpdate.html", context);
			EmailSenderThread emailSenderThread = new EmailSenderThread(email, "Account updated", htmlContent, true, null, restTemplate);
			emailSenderThread.start();
		} catch (Exception e) {
			throw new CustomException(e.getMessage());
		}
	}

	public PaymentCharges invoiceUpdateMapper(List<AccountEntity> accountEntity) {
		PaymentCharges charges = new PaymentCharges();
		charges.setAdminCity(accountEntity.get(0).getAdmin_details().getCity());
		charges.setAdminCountry(accountEntity.get(0).getAdmin_details().getCountry());
		charges.setAdminGst(accountEntity.get(0).getAdmin_details().getGst_in());
		charges.setAdminPan(accountEntity.get(0).getAdmin_details().getPan());
		charges.setAdminPhone(accountEntity.get(0).getAdmin_details().getMobile());
		charges.setAdminPin(accountEntity.get(0).getAdmin_details().getPin());
		charges.setAdminState(accountEntity.get(0).getAdmin_details().getState());

		charges.setDesignerCity(accountEntity.get(0).getDesigner_details().getCity());
		charges.setDesignerCountry(accountEntity.get(0).getDesigner_details().getCountry());
		charges.setDesignerPan(accountEntity.get(0).getDesigner_details().getPan());
		charges.setDesignerState(accountEntity.get(0).getDesigner_details().getState());
		charges.setDesignerPin(accountEntity.get(0).getDesigner_details().getPin());
		charges.setDesignerPhone(accountEntity.get(0).getDesigner_details().getMobile());
		charges.setDesignerGst(accountEntity.get(0).getDesigner_details().getGst_in());
		charges.setDesignerName(accountEntity.get(0).getDesigner_details().getDesigner_name());
		charges.setDisplayName(accountEntity.get(0).getDesigner_details().getDisplay_name());
		return charges;
	}

	public PaymentCharges invoiceUpdateMap(AccountEntity accountEntity) {
		PaymentCharges charges = new PaymentCharges();
		charges.setProductName(MessageConstant.DIVATT_CHARGES.getMessage());
		charges.setFee(accountEntity.getService_charge().getFee() + "");
		charges.setTotal(accountEntity.getService_charge().getTotal_amount() + "");
		charges.setSgst(accountEntity.getService_charge().getSgst() + "");
		charges.setIgst(accountEntity.getService_charge().getIgst() + "");
		charges.setCgst(accountEntity.getService_charge().getCgst() + "");
		charges.setRate(accountEntity.getService_charge().getRate() + "");
		charges.setTcs(accountEntity.getService_charge().getTcs() + "");
		charges.setTcsRate(accountEntity.getService_charge().getTcs_rate() + "");
		charges.setGrandTotal(accountEntity.getService_charge().getTotal_amount() + "");
		charges.setHsnCode(accountEntity.getOrder_details().get(0).getHsn_code());
		return charges;
	}

}