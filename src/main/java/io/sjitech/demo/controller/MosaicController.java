package io.sjitech.demo.controller;

import io.sjitech.demo.model.*;
import io.sjitech.demo.service.MosaicService;
import io.sjitech.demo.util.MijinUtil;
import org.nem.core.model.Account;
import org.nem.core.model.mosaic.MosaicDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by wang on 2016/07/25.
 */
@RestController
@RequestMapping("/mosaic")
public class MosaicController {

    private static final Logger log = LoggerFactory.getLogger(MosaicController.class);

    @Autowired
    private MijinUtil mijinUtil;

    @Autowired
    protected MosaicService mosaicService;

    @RequestMapping("/namespace/get/{name}")
    public MijinNamespace getNamespace(@PathVariable String name) {

        log.info("get namespace " + name + " data");

        return mosaicService.getNamespace(name);
    }

    @RequestMapping("/get/{namespace}/{mosaic}")
    public MijinMosaic getMosaic(@PathVariable("namespace") String namespace,
                                 @PathVariable("mosaic") String mosaic) {

        log.info("get mosaic " + mosaic + " within namespace " + namespace + " data");

        return mosaicService.getMosaic(namespace, mosaic);
    }

    @RequestMapping("/query/{namespace}/{mosaic}")
    public MijinMosaic getMosaicDefinition(@PathVariable("namespace") String namespace,
                                                @PathVariable("mosaic") String mosaic) {

        log.info("get mosaic " + mosaic + " definition within namespace " + namespace);

        return mosaicService.retrieveMosaicDefinition(namespace, mosaic);
    }

    @RequestMapping("/namespace/create/{name}")
    public TransactionResult createNamespace(@PathVariable String name) {

        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        log.info("create namespace " + name + " by " + sender.getAddress());

        return mosaicService.createRootNamespace(sender, name);
    }


    @RequestMapping("/create/{namespace}/{name}")
    public TransactionResult createMosaic(@PathVariable("namespace") String namespace,
                                          @PathVariable("name") String name) {

        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        // TODO get mosaic definition from post request body
        log.info("create mosaic " + name + " in namespace " + namespace + " by " + sender.getAddress());

        return mosaicService.createMosaic(sender, namespace, name);
    }

    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public TransactionResult createMosaic(@RequestBody MosaicParameter parameter) {

        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        log.info(String.format("create mosaic %s in namespace %s by %s",
                parameter.getMosaic(),
                parameter.getNamespace(),
                sender.getAddress()));

        return mosaicService.createMosaic(sender, parameter);
    }

    @RequestMapping("/transfer/{username}/{namespace}/{mosaic}/{amount}")
    public TransactionResult transferMosaic(@PathVariable("namespace") String namespaceName,
                                            @PathVariable("mosaic") String mosaicName,
                                            @PathVariable("username") String recipientAddress,
                                            @PathVariable("amount") long amount) {

        log.info(String.format("transfer %s*%s %d to %s", namespaceName, mosaicName, amount, recipientAddress));

        // TODO get sender from login session
        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        return mosaicService.transferMosaic(sender, recipientAddress, namespaceName, mosaicName, amount);
    }

    @RequestMapping(value = "/send", method = RequestMethod.POST)
    public TransactionResult sendMosaic(@RequestBody TransferParameter parameter) {
        Account sender = mijinUtil.getExistMijinAccounts().get(0); // use root account

        log.info(String.format("transfer mosaics from %s to %s",
                sender.getAddress(),
                parameter.getAddress()));

        return mosaicService.sendMosaics(sender, parameter);
    }

}
