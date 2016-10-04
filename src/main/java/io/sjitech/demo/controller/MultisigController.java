package io.sjitech.demo.controller;

import io.sjitech.demo.model.MultisigParameter;
import io.sjitech.demo.model.TransactionResult;
import io.sjitech.demo.service.MultisigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by wang on 2016/07/28.
 */
@RestController
@RequestMapping("/multisig")
public class MultisigController {

    private static final Logger log = LoggerFactory.getLogger(MultisigController.class);

    @Autowired
    protected MultisigService multisigService;

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public TransactionResult create(@RequestBody MultisigParameter parameter) {

        log.info("convert account {} to multisig account", parameter.getSignerPrivateKey());

        return multisigService.createMultisigAccount(parameter);
    }

    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    public TransactionResult modify(@RequestBody MultisigParameter parameter) {

        log.info("modify multisig account {} by {}", parameter.getMultisigPublicKey(), parameter.getSignerPrivateKey());

        return multisigService.modifyMultisigAccount(parameter);
    }

    @RequestMapping(value = "/transfer", method = RequestMethod.POST)
    public TransactionResult transfer(@RequestBody MultisigParameter parameter) {

        log.info("transfer some XEM or mosaic from multisig account {} by {} to {}",
                parameter.getMultisigPublicKey(), parameter.getSignerPrivateKey(), parameter.getTransferParameter().getAddress());

        return multisigService.transferFromMultisig(parameter);
    }

    @RequestMapping(value = "/sign", method = RequestMethod.POST)
    public TransactionResult sign(@RequestBody MultisigParameter parameter) {

        log.info("sign multisig transaction for account {} by {}", parameter.getMultisigPublicKey(), parameter.getSignerPrivateKey());

        return multisigService.signMultisigTransaction(parameter);
    }
}
