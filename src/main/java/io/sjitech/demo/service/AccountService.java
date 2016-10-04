package io.sjitech.demo.service;

import io.sjitech.demo.model.*;
import io.sjitech.demo.util.MijinUtil;
import io.sjitech.demo.util.NisExtendApiId;
import org.nem.core.connect.client.NisApiId;
import org.nem.core.model.MessageTypes;
import org.nem.core.model.TransactionTypes;
import org.nem.core.model.TransferTransaction;
import org.nem.core.model.mosaic.Mosaic;
import org.nem.core.model.ncc.*;
import org.nem.core.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by wang on 2016/07/16.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private MijinUtil mijinUtil;

    public MijinAccount createUser(User user) {
        log.info("create mijin account");

        return generateMijinAccount();
    }

    public MijinAccount getUser(String username) {
        log.info("get user {} mijin data", username);

        return getAccountData(username);
    }

    private MijinAccount getAccountData(String address) {
        final CompletableFuture<Deserializer > future =  mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisApiId.NIS_REST_ACCOUNT_LOOK_UP,
                "address=" + address
        );

        MijinAccount mijinAccount = new MijinAccount();

        future.thenAccept(d -> {
            final AccountMetaDataPair result = new AccountMetaDataPair(d);

            mijinAccount.setAddress(address);
            mijinAccount.setAccountMetaDataPair(result);

            log.info("--------------------------------------------------------------------------------");

            if (result.getEntity() != null) {
                AccountInfo accountInfo = result.getEntity();

                log.info("account address : " + accountInfo.getAddress());
                log.info("account balance : " + accountInfo.getBalance());
                log.info("account vested balance : " + accountInfo.getVestedBalance());
                log.info("account importance : " + accountInfo.getImportance());
                log.info("account harvestedBlocks : " + accountInfo.getNumHarvestedBlocks());
            }

            if (result.getMetaData() != null) {
                AccountMetaData accountMetaData = result.getMetaData();

                log.info("account status " + accountMetaData.getStatus());
                log.info("account remote status " + accountMetaData.getRemoteStatus());

                if (accountMetaData.getCosignatoryOf() != null) {
                    for(AccountInfo cosignatoryOfAccount : accountMetaData.getCosignatoryOf()) {
                        log.info("account cosignatoryOf " + cosignatoryOfAccount.getAddress());
                    }
                }

                if (accountMetaData.getCosignatories() != null) {
                    for(AccountInfo cosignatory : accountMetaData.getCosignatories()) {
                        log.info("account cosignatory " + cosignatory.getAddress());
                    }
                }
            }

        }).exceptionally(e -> {
            log.warn(String.format("could not get account %s data, reason: %s",
                    address,
                    e.getMessage()), e);

            return null;
        }).join();

        return mijinAccount;
    }

    private MijinAccount generateMijinAccount() {
        final CompletableFuture<Deserializer > future =  mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_ACCOUNT_GENERATE,
                null
        );


        MijinAccount mijinAccount = new MijinAccount();

        future.thenAccept(d -> {
            log.info("--------------------------------------------------------------------------------");

            mijinAccount.setAddress(d.readString("address"));
            mijinAccount.setPrivateKey(d.readString("privateKey"));
            mijinAccount.setPublicKey(d.readString("publicKey"));

            log.info(String.format("\n{\n\t\"privateKey\":\"%s\",\n\t\"address\":\"%s\",\n\t\"publicKey\":\"%s\"\n}\n",
                    mijinAccount.getPrivateKey(),
                    mijinAccount.getAddress(),
                    mijinAccount.getPublicKey()
                ));

        }).exceptionally(e -> {
            log.warn(String.format("could not generate new account from %s , reason: %s",
                    NisExtendApiId.NIS_REST_ACCOUNT_GENERATE,
                    e.getMessage()), e);

            return null;
        }).join();

        return mijinAccount;
    }

    public List<TransactionData> getAccountUnconfirmedTransactions(String address) {
        List<TransactionData> transactions = mijinUtil.sendGetRequest(
                NisApiId.NIS_REST_ACCOUNT_UNCONFIRMED,
                "address=" + address,
                (deserializer -> {
                    final List<UnconfirmedTransactionMetaDataPair> resultList =
                            deserializer.readOptionalObjectArray("data", UnconfirmedTransactionMetaDataPair::new);

                    List<TransactionData> transactionList = new ArrayList<TransactionData>();

                    if (resultList != null && resultList.size() > 0) {
                        resultList.stream()
                                .forEach(pair -> {
                                    TransactionData data = new TransactionData();

                                    data.setHash(pair.getMetaData().getInnerTransactionHash().toString());

                                    data.setFee(pair.getEntity().getFee().getNumNem());
                                    data.setSigner(pair.getEntity().getSigner().getAddress().toString());
                                    data.setTimestamp(pair.getEntity().getTimeStamp().getRawTime());
                                    data.setDeadline(pair.getEntity().getDeadline().getRawTime());
                                    data.setType(pair.getEntity().getType());
                                    data.setVersion(pair.getEntity().getVersion());

                                    parseTransactionDataByType(pair, data);

                                    transactionList.add(data);
                                });
                    }

                    return transactionList;
                }));

        if (transactions == null || transactions.isEmpty()) {
            log.warn("No account {} unconfirmed transaction data", address);
        }

        return transactions;
    }

    protected void parseTransactionDataByType(UnconfirmedTransactionMetaDataPair pair, TransactionData data) {
        if (pair.getEntity().getType() == TransactionTypes.TRANSFER) {
            TransferTransaction transferTransaction = (TransferTransaction) pair.getEntity();

            data.setAmount(transferTransaction.getAmount().getNumNem());
            data.setRecipient(transferTransaction.getRecipient().getAddress().toString());
            if (transferTransaction.getMessage() != null) {
                if (transferTransaction.getMessage().getType() == MessageTypes.PLAIN) {
                    data.setMessage(new String(transferTransaction.getMessage().getDecodedPayload()));
                }
            }

            if (transferTransaction.getMosaics() != null && transferTransaction.getMosaics().size() > 0) {
                List<MosaicData> mosaicDataList = new ArrayList<>();

                for (Mosaic mosaic : transferTransaction.getMosaics()) {
                    MosaicData mosaicData = new MosaicData();

                    mosaicData.setNamespace(mosaic.getMosaicId().getNamespaceId().toString());
                    mosaicData.setMosaic(mosaic.getMosaicId().getName());
                    mosaicData.setQuantity(mosaic.getQuantity().getRaw());

                    mosaicDataList.add(mosaicData);
                }
            }
        } else if (pair.getEntity().getType() == TransactionTypes.PROVISION_NAMESPACE) {

        } else if (pair.getEntity().getType() == TransactionTypes.MOSAIC_DEFINITION_CREATION) {

        } else if (pair.getEntity().getType() == TransactionTypes.MULTISIG_AGGREGATE_MODIFICATION) {

        } else if (pair.getEntity().getType() == TransactionTypes.MULTISIG_SIGNATURE) {

        } else if (pair.getEntity().getType() == TransactionTypes.MULTISIG) {

        } // TODO other transaction type
    }

    private List<TransactionData> parseTransactionSearchResult(Deserializer deserializer) {
        final List<TransactionMetaDataPair> resultList =
                deserializer.readOptionalObjectArray("data", TransactionMetaDataPair::new);

        List<TransactionData> transactionList = new ArrayList<TransactionData>();

        if (resultList != null && resultList.size() > 0) {
            resultList.stream()
                    .forEach(pair -> {
                        TransactionData data = new TransactionData();

                        data.setId(pair.getMetaData().getId());
                        data.setHash(pair.getMetaData().getHash().toString());
                        data.setHeight(pair.getMetaData().getHeight().getRaw());

                        data.setFee(pair.getEntity().getFee().getNumNem());
                        data.setSigner(pair.getEntity().getSigner().getAddress().toString());
                        data.setTimestamp(pair.getEntity().getTimeStamp().getRawTime());
                        data.setDeadline(pair.getEntity().getDeadline().getRawTime());
                        data.setType(pair.getEntity().getType());
                        data.setVersion(pair.getEntity().getVersion());

                        if (pair.getEntity().getType() == TransactionTypes.TRANSFER) {
                            TransferTransaction transferTransaction = (TransferTransaction) pair.getEntity();

                            data.setAmount(transferTransaction.getAmount().getNumNem());
                            data.setRecipient(transferTransaction.getRecipient().getAddress().toString());
                            if (transferTransaction.getMessage() != null) {
                                if (transferTransaction.getMessage().getType() == MessageTypes.PLAIN) {
                                    data.setMessage(new String(transferTransaction.getMessage().getDecodedPayload()));
                                } else if (transferTransaction.getMessage().getType() == MessageTypes.SECURE) {
                                    // TODO decrypt message need account private key and sender public key
                                    data.setMessage(new String(transferTransaction.getMessage().getEncodedPayload()));
                                }
                            }

                            if (transferTransaction.getMosaics() != null && transferTransaction.getMosaics().size() > 0) {
                                List<MosaicData> mosaicDataList = new ArrayList<>();

                                for (Mosaic mosaic : transferTransaction.getMosaics()) {
                                    MosaicData mosaicData = new MosaicData();

                                    mosaicData.setNamespace(mosaic.getMosaicId().getNamespaceId().toString());
                                    mosaicData.setMosaic(mosaic.getMosaicId().getName());
                                    mosaicData.setQuantity(mosaic.getQuantity().getRaw());

                                    mosaicDataList.add(mosaicData);
                                }
                            }
                        } // TODO other transaction type

                        transactionList.add(data);
                    });
        }

        return transactionList;
    }

    public List<TransactionData> getAccountAllTransactions(String address) {
        // TODO omit pagination process

        List<TransactionData> transactions = mijinUtil.sendGetRequest(
                NisApiId.NIS_REST_ACCOUNT_TRANSFERS_ALL,
                "address=" + address,
                (deserializer -> {
                    return parseTransactionSearchResult(deserializer);
                }));

        if (transactions == null || transactions.isEmpty()) {
            log.warn("No account {} transaction data", address);
        }

        return transactions;
    }

    public List<UserMosaic> getAccountOwnedMosaic(String namespaceName, String mosaicName, String address) {

        log.info("get user {} owned mosaic {}*{} data", address, namespaceName, mosaicName);

        List<UserMosaic> userMosaics = mijinUtil.sendGetRequest(
                NisExtendApiId.NIS_REST_ACCOUNT_MOSAIC_OWNED,
                "address=" + address,
                (deserializer -> {
                    final List<Mosaic> mosaicList = deserializer.readOptionalObjectArray("data", Mosaic::new);

                    List<UserMosaic> result = new ArrayList<UserMosaic>();

                    if (mosaicList != null && mosaicList.size() > 0) {
                        for (Mosaic mosaic : mosaicList) {
                            UserMosaic userMosaic = new UserMosaic();

                            String nsName = mosaic.getMosaicId().getNamespaceId().toString();
                            String name = mosaic.getMosaicId().getName();

                            userMosaic.setMosaic(name);
                            userMosaic.setNamespace(nsName);
                            userMosaic.setQuantity(mosaic.getQuantity().getRaw());

                            if (mosaicName == null && namespaceName == null) {
                                result.add(userMosaic);
                            } else if (mosaicName == null) {
                                if (nsName.equals(namespaceName)) {
                                    result.add(userMosaic);
                                }
                            } else {
                                if (nsName.equals(namespaceName) && name.equals(mosaicName)) {
                                    result.add(userMosaic);
                                }
                            }
                        }
                    }

                    return result;
                }));

        if (userMosaics == null || userMosaics.isEmpty()) {
            log.warn("account {} mosaic {}*{} data was not found.",
                    address, namespaceName, mosaicName);
        }

        return userMosaics;


        /*
        final CompletableFuture<Deserializer > future =  mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_ACCOUNT_MOSAIC_OWNED,
                "address=" + address
        );

        UserMosaic userMosaic = new UserMosaic();

        future.thenAccept(d -> {
            final List<Mosaic> mosaicList = d.readOptionalObjectArray("data", Mosaic::new);

            if (mosaicList != null && mosaicList.size() > 0) {
                mosaicList.stream()
                        .filter(mosaic -> namespaceName.equals(mosaic.getMosaicId().getNamespaceId().toString()) &&
                                mosaicName.equals(mosaic.getMosaicId().getName()))
                        .forEach(mosaic -> {
                            userMosaic.setNamespace(mosaic.getMosaicId().getNamespaceId().toString());
                            userMosaic.setMosaic(mosaic.getMosaicId().getName());
                            userMosaic.setQuantity(mosaic.getQuantity().getRaw());
                        });

                if (userMosaic.getMosaic() != null) {
                    log.info("--------------------------------------------------------------------------------");

                    log.info("mosaic namespace : " + userMosaic.getNamespace());
                    log.info("mosaic name : " + userMosaic.getMosaic());
                    log.info("mosaic quantity : " + userMosaic.getQuantity());
                }
            }

            if (userMosaic.getMosaic() == null) {
                log.warn("account {} has not owned mosaic {}*{}", address, namespaceName, mosaicName);
//                throw new AppException(String.format("account %s has not owned mosaic %s*%s", address, namespaceName, mosaicName));
            }

        }).exceptionally(e -> {
            log.warn(String.format("could not get account %s owned %s*%s data, reason: %s",
                    address,
                    namespaceName,
                    mosaicName,
                    e.getMessage()), e);

            return null;
        }).join();

        return userMosaic;
        */
    }
}
