package io.sjitech.demo.controller;

import io.sjitech.demo.model.TransactionResult;
import io.sjitech.demo.model.TransferParameter;
import io.sjitech.demo.service.TransferService;
import io.sjitech.demo.util.MijinUtil;
import org.nem.core.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by wang on 2016/07/16.
 */
@RestController
@RequestMapping("/transfer")
public class TransferController {

    private static final Logger log = LoggerFactory.getLogger(TransferController.class);

    @Autowired
    private MijinUtil mijinUtil;

    @Autowired
    private TransferService transferService;

    @RequestMapping("/simple/{username}/{amount}")
    public TransactionResult transferXem(@PathVariable("username") String username,
                                      @PathVariable("amount") long amount) {

        log.info("transfer " + amount + " xem to  account " + username + " ");

        // TODO get sender from login session
        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        return transferService.transferXem(sender, username, amount);
    }

    @RequestMapping(value = "/message", method = RequestMethod.POST)
    public TransactionResult transferXemWithMessage(@RequestBody TransferParameter parameter) {

        log.info(String.format("transfer %d xem to account %s with message %s",
                    parameter.getAmount(),
                    parameter.getAddress(),
                    parameter.getMessage()
                ));

        // TODO get sender from login session
        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        return transferService.transferSimple(sender, parameter.getAddress(), parameter.getAmount(),
                parameter.getMessage(), parameter.isSecureMessage(), parameter.getPublicKeyHexString());
    }


}
