package io.sjitech.demo.service;

import io.sjitech.demo.model.TransactionResult;
import io.sjitech.demo.util.MijinUtil;
import org.nem.core.connect.HttpJsonPostRequest;
import org.nem.core.connect.client.NisApiId;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PublicKey;
import org.nem.core.messages.PlainMessage;
import org.nem.core.messages.SecureMessage;
import org.nem.core.model.*;
import org.nem.core.model.ncc.NemAnnounceResult;
import org.nem.core.model.ncc.RequestAnnounce;
import org.nem.core.model.primitive.Amount;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.serialization.Deserializer;
import org.nem.core.time.TimeInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Created by wang on 2016/07/28.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    @Autowired
    private MijinUtil mijinUtil;

    public TransactionResult transferXem(Account sender, String recipientAddress, long amountOfXem) {
        Account recipient = new Account(Address.fromEncoded(recipientAddress));

        return transferWithoutMosaic(sender, recipient, amountOfXem, null);
    }

    public TransactionResult transferSimple(Account sender, String recipientAddress, long amountOfXem,
                                            String messageContent, boolean secureMessage, String recipientPublicKey) {

        Account recipient = new Account(Address.fromEncoded(recipientAddress));

        Supplier<TransferTransaction> transactionSupplier = () -> {
            // message attachment
            TransferTransactionAttachment attachment = null;

            if (messageContent != null && messageContent.trim().length() > 0) {
                // set message
                Message message = null;

                if (secureMessage) {
                    // need public key to decode secure message
                    final Account recipientAccount = new Account(new KeyPair(PublicKey.fromHexString(recipientPublicKey)));

                    message = SecureMessage.fromDecodedPayload(sender, recipientAccount, messageContent.getBytes());
                } else {
                    // plain message (max length 160 bytes)
                    message = new PlainMessage(messageContent.getBytes());
                }

                attachment = new TransferTransactionAttachment();
                attachment.setMessage(message);
            }

            // prepare transaction data and sign it
            final int version = 1;
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final TransferTransaction transaction = new TransferTransaction(
                    version,                          // version
                    timeInstant,                      // time instant
                    sender,                           // sender
                    recipient,                        // recipient
                    Amount.fromNem(amountOfXem),      // amount in xem
                    attachment);                      // attachment (message, mosaics)

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
                log.info(String.format("successfully send %d xem from %s to %s",
                        amountOfXem,
                        sender.getAddress(),
                        recipient.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not send xem from %s to %s, reason: %s",
                        sender.getAddress(),
                        recipient.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));

    }

    private TransactionResult transferWithoutMosaic(Account sender, Account recipient,
                                                    long amountOfXem, Message message) {
        // message attachment
        TransferTransactionAttachment attachment = null;

        if (message != null) {
            attachment = new TransferTransactionAttachment();
            attachment.setMessage(message);
        }

        // prepare transaction data and sign it
        int version = 1;
        final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

        final TransferTransaction transaction = new TransferTransaction(
                version,                          // version
                timeInstant,                      // time instant
                sender,                           // sender
                recipient,                        // recipient
                Amount.fromNem(amountOfXem),      // amount in micro xem
                attachment);                      // attachment (message, mosaics)

        // transfer fee will be calculated automatically
//        TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator();
//        transaction.setFee(calculator.calculateMinimumFee(transaction));

        transaction.setDeadline(timeInstant.addHours(23));
        transaction.sign();

        // send transaction and get result

        final byte[] data = BinarySerializer.serializeToBytes(transaction.asNonVerifiable());
        final RequestAnnounce request = new RequestAnnounce(data, transaction.getSignature().getBytes());

        final CompletableFuture<Deserializer> future = mijinUtil.getConnector().postAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisApiId.NIS_REST_TRANSACTION_ANNOUNCE,
                new HttpJsonPostRequest(request));

        TransactionResult transactionResult = new TransactionResult();

        future.thenAccept(d -> {
            final NemAnnounceResult result = new NemAnnounceResult(d);

            transactionResult.setCode(result.getCode());
            transactionResult.setType(result.getType());
            transactionResult.setMessage(result.getMessage());

            if (result.getCode() == 1) {
                log.info(String.format("successfully send %d xem from %s to %s",
                        amountOfXem,
                        sender.getAddress(),
                        recipient.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not send xem from %s to %s, reason: %s",
                    sender.getAddress(),
                    recipient.getAddress(),
                    result.getMessage()));
            }

        }).exceptionally(e -> {
            log.warn(String.format("could not send xem from %s to %s, reason: %s",
                sender.getAddress(),
                recipient.getAddress(),
                e.getMessage()), e);

            return null;
        }).join();

        return transactionResult;
    }

}
