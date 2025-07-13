package com.delight.account.controller;

import com.delight.account.dto.ActivationRequest;
import com.delight.account.service.ActivationService;
import jakarta.validation.Valid;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@RequestMapping("/api")
public class ActivationController {

    private static final Logger logger = LoggerFactory.getLogger(ActivationController.class);

    private final ActivationService activationService;
    private final MessageSource messageSource;

    public ActivationController(ActivationService activationService, MessageSource messageSource) {
        this.activationService = activationService;
        this.messageSource = messageSource;
    }

    @PostMapping("/activate")
    public ResponseEntity<String> activate(@Valid @RequestBody ActivationRequest request,
                                           @RequestHeader(name = "Accept-Language", required = false) Locale locale) {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        Long accountId = (Long) attr.getRequest().getAttribute("accountId");
        String country = attr.getRequest().getLocale().getCountry();
        logger.info("Activation request accountId={} emailAddress={} firstName={} lastName={} phoneNumber={} country={}",
            accountId, request.getEmailAddress(), request.getFirstName(), request.getLastName(), request.getPhoneNumber(), country);
        activationService.activateUser(accountId, request, country);
        String message = messageSource.getMessage("activation.success", null,
            locale == null ? Locale.ENGLISH : locale);
        return ResponseEntity.ok(message);
    }
}
