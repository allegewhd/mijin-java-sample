package io.sjitech.demo.service;

import io.sjitech.demo.model.MosaicParameter;
import io.sjitech.demo.model.MultisigParameter;
import io.sjitech.demo.model.TransactionResult;
import io.sjitech.demo.model.TransferParameter;
import io.sjitech.demo.util.MijinUtil;
import org.nem.core.crypto.Hash;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.crypto.PublicKey;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.mosaic.MosaicId;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.ncc.NemAnnounceResult;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.time.TimeInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by wang on 2016/08/10.
 */
@Service
public class MultisigService {
    private static final Logger log = LoggerFactory.getLogger(MultisigService.class);

    @Autowired
    private MijinUtil mijinUtil;

    public TransactionResult createMultisigAccount(MultisigParameter parameter) {
        // convert signer to multisig account, use signer private key to start convert transaction
        final Account sender = new Account(new KeyPair(PrivateKey.fromHexString(parameter.getSignerPrivateKey())));

        List<Account> cosigners = new ArrayList<>();

        parameter.getAddCosignerPublicKeys().forEach(cosigner -> {
            cosigners.add(new Account(new KeyPair(PublicKey.fromHexString(cosigner))));
        });

        return convertAccountToMultisigAccount(sender, cosigners, parameter.getRelativeMinCosignerNum());
    }

    public TransactionResult convertAccountToMultisigAccount(Account sender, List<Account> cosigners,
                                                             int minCosignerNum) {

        Supplier<MultisigAggregateModificationTransaction> transactionSupplier = () -> {
            final List<MultisigCosignatoryModification> cosignerList =
                    cosigners.stream()
                            .map(cosigner -> new MultisigCosignatoryModification(MultisigModificationType.AddCosignatory, cosigner))
                            .collect(Collectors.toList());

            final MultisigMinCosignatoriesModification requiredMinCosignators =
                    new MultisigMinCosignatoriesModification(minCosignerNum);

            // prepare transaction data and sign it
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final MultisigAggregateModificationTransaction transaction = new MultisigAggregateModificationTransaction(
                    timeInstant,
                    sender,
                    cosignerList,
                    requiredMinCosignators);

            transaction.setDeadline(timeInstant.addHours(23));
            transaction.sign();

            return transaction;
        };

        return mijinUtil.postTransactionAnnounce(sender, transactionSupplier, (deserializer -> {
            TransactionResult transactionResult = new TransactionResult();

            final NemAnnounceResult result = new NemAnnounceResult(deserializer);

            transactionResult.setCode(result.getCode());
            transactionResult.setType(result.getType());
            transactionResult.setMessage(result.getMessage());

            if (result.getCode() == 1) {
                log.info(String.format("successfully converted %s into a multisig account",
                        sender.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("can not convert account %s to multisig account, reason: %s",
                        sender.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    public TransactionResult transferFromMultisig(MultisigParameter parameter) {
        // only need multisig account public key
        final Account sender = new Account(new KeyPair(PublicKey.fromHexString(parameter.getMultisigPublicKey())));

        // need cosigner private key to start multisig transfer transaction
        final Account signer = new Account(new KeyPair(PrivateKey.fromHexString(parameter.getSignerPrivateKey())));

        Supplier<MultisigTransaction> transactionSupplier = () -> {
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final Transaction innerTransaction = createTransferTransaction(sender, parameter.getTransferParameter());

            MultisigTransaction transaction = new MultisigTransaction(timeInstant, signer, innerTransaction);

            transaction.setDeadline(timeInstant.addHours(23));
            transaction.sign();

            return transaction;
        };

        return mijinUtil.postTransactionAnnounce(sender, transactionSupplier, (deserializer -> {
            TransactionResult transactionResult = new TransactionResult();

            final NemAnnounceResult result = new NemAnnounceResult(deserializer);

            transactionResult.setCode(result.getCode());
            transactionResult.setType(result.getType());
            transactionResult.setMessage(result.getMessage());

            if (result.getCode() == 1) {
                log.info(String.format("transfer some XEM or mosaic from multisig account %s to %s signed by %s successfully",
                        sender.getAddress(),
                        parameter.getTransferParameter().getAddress(),
                        signer.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("%s create multisig transaction for %s transfer XEM or mosaic to %s failed! reason: %s",
                        signer.getAddress(),
                        sender.getAddress(),
                        parameter.getTransferParameter().getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    private TransferTransaction createTransferTransaction(Account sender, TransferParameter parameter) {
        Account recipient = new Account(Address.fromEncoded(parameter.getAddress()));

        TransferTransactionAttachment attachment = null;

        if (parameter.getMessage() != null && parameter.getMessage().length() > 0) {
            attachment = new TransferTransactionAttachment();
            attachment.setMessage(new PlainMessage(parameter.getMessage().getBytes()));
        }

        int version = 1;

        if (parameter.getMosaics() != null && parameter.getMosaics().size() > 0) {
            version = 2;

            if (attachment == null) {
                attachment = new TransferTransactionAttachment();
            }

            for (MosaicParameter mosaicData : parameter.getMosaics()) {
                MosaicId mosaicId = new MosaicId(new NamespaceId(mosaicData.getNamespace()), mosaicData.getMosaic());
                Mosaic mosaic = new Mosaic(mosaicId, Quantity.fromValue(mosaicData.getQuantity()));

                attachment.addMosaic(mosaic);
            };
        }

        final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

        final TransferTransaction transferTransaction = new TransferTransaction(
                version,
                timeInstant,
                sender,
                recipient,
                Amount.fromNem(parameter.getAmount()),
                attachment);

        transferTransaction.setDeadline(timeInstant.addHours(23));
//        transferTransaction.sign();

        return transferTransaction;
    }

    public TransactionResult signMultisigTransaction(MultisigParameter parameter) {
        // only need multisig account public key
        final Account sender = new Account(new KeyPair(PublicKey.fromHexString(parameter.getMultisigPublicKey())));
        // need cosigner private key
        final Account signer = new Account(new KeyPair(PrivateKey.fromHexString(parameter.getSignerPrivateKey())));

        return signMultisigTransaction(signer, sender, parameter.getInnerHashString());
    }

    public TransactionResult signMultisigTransaction(Account signer, Account sender, String innerHashString) {
        Supplier<MultisigSignatureTransaction> transactionSupplier = () -> {
            final Hash innerHash = Hash.fromHexString(innerHashString);

            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final MultisigSignatureTransaction transaction = new MultisigSignatureTransaction(
                    timeInstant,
                    signer,
                    sender,
                    innerHash);

            transaction.setDeadline(timeInstant.addHours(23));
            transaction.sign();

            return transaction;
        };

        return mijinUtil.postTransactionAnnounce(sender, transactionSupplier, (deserializer -> {
            // TODO can be common function for transaction post result processing
            TransactionResult transactionResult = new TransactionResult();

            final NemAnnounceResult result = new NemAnnounceResult(deserializer);

            transactionResult.setCode(result.getCode());
            transactionResult.setType(result.getType());
            transactionResult.setMessage(result.getMessage());

            if (result.getCode() == 1) {
                log.info(String.format("sign multisig transaction %s for %s by %s successfully",
                        innerHashString,
                        sender.getAddress(),
                        signer.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("sign multisig transaction %s for %s by %s failed! reason: %s",
                        innerHashString,
                        signer.getAddress(),
                        sender.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    public TransactionResult modifyMultisigAccount(MultisigParameter parameter) {
        // use cosigner private key
        final Account signer = new Account(new KeyPair(PrivateKey.fromHexString(parameter.getSignerPrivateKey())));
        // need multisig account public key
        final Account multisig = new Account(new KeyPair(PublicKey.fromHexString(parameter.getMultisigPublicKey())));

        Map<MultisigModificationType, Account> modifyCosigners = new HashMap<>();

        if (parameter.getAddCosignerPublicKeys() != null && parameter.getAddCosignerPublicKeys().size() > 0) {
            parameter.getAddCosignerPublicKeys().forEach(addCosignerPublicKey -> {
                modifyCosigners.put(MultisigModificationType.AddCosignatory,
                        new Account(new KeyPair(PublicKey.fromHexString(addCosignerPublicKey))));
            });
        }

        if (parameter.getDelCosignerPublicKeys() != null && parameter.getDelCosignerPublicKeys().size() > 0) {
            parameter.getDelCosignerPublicKeys().forEach(delCosignerPublicKey -> {
                modifyCosigners.put(MultisigModificationType.DelCosignatory,
                        new Account(new KeyPair(PublicKey.fromHexString(delCosignerPublicKey))));
            });
        }

        return modifyCosignerOfMultisigAccount(signer, multisig,
                modifyCosigners, parameter.getRelativeMinCosignerNum());
    }

    public TransactionResult modifyCosignerOfMultisigAccount(Account signer, Account multisig,
                                                             Map<MultisigModificationType, Account> modifyCosigners,
                                                             int minSingerNumDelta) {
        Supplier<MultisigTransaction> transactionSupplier = () -> {
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final List<MultisigCosignatoryModification> modifications = new ArrayList<>();

            modifyCosigners.forEach((type, cosigner) -> {
                modifications.add(new MultisigCosignatoryModification(type, cosigner));
            });

            final MultisigMinCosignatoriesModification minCosignatoriesModification =
                    new MultisigMinCosignatoriesModification(minSingerNumDelta);

            final MultisigAggregateModificationTransaction modificationTransaction =
                    new MultisigAggregateModificationTransaction(
                            timeInstant, multisig, modifications, minCosignatoriesModification);

            modificationTransaction.setDeadline(timeInstant.addHours(23));
//            modificationTransaction.sign();

            final MultisigTransaction transaction = new MultisigTransaction(
                    timeInstant,
                    signer,
                    modificationTransaction);

            transaction.setDeadline(timeInstant.addHours(23));
            transaction.sign();

            return transaction;
        };

        return mijinUtil.postTransactionAnnounce(signer, transactionSupplier, (deserializer -> {
            TransactionResult transactionResult = new TransactionResult();

            final NemAnnounceResult result = new NemAnnounceResult(deserializer);

            transactionResult.setCode(result.getCode());
            transactionResult.setType(result.getType());
            transactionResult.setMessage(result.getMessage());

            if (result.getCode() == 1) {
                log.info(String.format("change multisig %s cosigners by %s successfully",
                        multisig.getAddress(),
                        signer.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("change multisig %s cosigners by %s failed! reason: %s",
                        multisig.getAddress(),
                        signer.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }
}
