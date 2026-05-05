package com.sneakershop.backend.service.order;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Getter
public class SepayService {

    @Value("${sepay.enabled:false}")
    private boolean enabled;

    @Value("${sepay.account-name:}")
    private String accountName;

    @Value("${sepay.bank-account-no:}")
    private String bankAccountNo;

    @Value("${sepay.bank-code:}")
    private String bankCode;

    @Value("${sepay.bank-name:}")
    private String bankName;

    @Value("${sepay.expected-sub-account:}")
    private String expectedSubAccount;

    @Value("${sepay.webhook-api-key:}")
    private String webhookApiKey;

    @Value("${sepay.qr-template:compact2}")
    private String qrTemplate;

    @Value("${sepay.payment-prefix:SS}")
    private String paymentPrefix;

    public String buildPaymentCode(Long orderId) {
        return paymentPrefix + orderId;
    }

    public String buildTransferContent(Long orderId) {
        return buildPaymentCode(orderId);
    }

    public String buildQrImageUrl(BigDecimal amount, String transferContent) {
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        return "https://img.vietqr.io/image/"
                + bankCode + "-" + bankAccountNo + "-" + qrTemplate
                + ".png?amount=" + amount.toBigInteger().toString()
                + "&addInfo=" + encodedContent
                + "&accountName=" + encodedName;
    }

    public boolean isValidWebhookAuth(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }
        String expected = "Apikey " + webhookApiKey;
        return expected.equals(authorizationHeader.trim());
    }

    public boolean matchesReceivingAccount(String accountNumber, String subAccount) {
        if (subAccount != null && !subAccount.isBlank()) {
            return expectedSubAccount != null && expectedSubAccount.equalsIgnoreCase(subAccount.trim());
        }
        return bankAccountNo != null && bankAccountNo.equalsIgnoreCase(accountNumber);
    }
}