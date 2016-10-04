package io.sjitech.demo.controller;

import io.sjitech.demo.model.User;
import io.sjitech.demo.model.MijinAccount;
import io.sjitech.demo.model.TransactionData;
import io.sjitech.demo.model.UserMosaic;
import io.sjitech.demo.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by wang on 2016/07/16.
 */
@RestController
@RequestMapping("/account")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    protected AccountService accountService;

    @RequestMapping(value = "/new")
    public MijinAccount create() {

        MijinAccount account = accountService.createUser(null);

        log.info("mijin account with address {} created!", account.getAddress());

        return account;
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public MijinAccount create(@RequestBody User user) {

        log.info("create account " + user.getUsername());

        MijinAccount account = accountService.createUser(user);

        log.info("mijin account with address {} created!", account.getAddress());

        return account;
    }

    @RequestMapping("/get/{username}")
    public MijinAccount getAccountData(@PathVariable String username) {

        log.info("get account " + username + " data");

        MijinAccount account = accountService.getUser(username);

        return account;
    }

    @RequestMapping(value = {"/mosaic/get/{username}/{namespace}/{mosaic}", "/mosaic/get/{username}/{namespace}", "/mosaic/get/{username}"})
    public List<UserMosaic> getAccountOwnedMosaic(@PathVariable("username") String username,
                                                  @PathVariable("namespace") Optional<String> namespaceName,
                                                  @PathVariable("mosaic") Optional<String> mosaicName) {

        String nsName = null;

        if (namespaceName.isPresent()) {
            nsName = namespaceName.get();
        }

        String mosaic = null;

        if (mosaicName.isPresent()) {
            mosaic = mosaicName.get();
        }

        log.info("get account " + username + " owned mosaic " + nsName + "*" + mosaic + " data");

        return accountService.getAccountOwnedMosaic(nsName, mosaic, username);
    }

    @RequestMapping("/transaction/unconfirmed/{username}")
    public List<TransactionData> getUnconfirmedTransactions(@PathVariable String username) {

        log.info("get account " + username + " unconfirmed transaction data");

        return accountService.getAccountUnconfirmedTransactions(username);
    }

    @RequestMapping("/transaction/all/{username}")
    public List<TransactionData> getHistoryTransactions(@PathVariable String username) {
        log.info("get account " + username + " history transaction data");

        List<TransactionData> historyData = accountService.getAccountAllTransactions(username);

        return historyData;
    }
}
