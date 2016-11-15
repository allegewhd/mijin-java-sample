package io.sjitech.demo.service;

import io.sjitech.demo.exception.AppException;
import io.sjitech.demo.exception.MijinException;
import io.sjitech.demo.model.*;
import io.sjitech.demo.util.CommonUtil;
import io.sjitech.demo.util.MijinUtil;
import io.sjitech.demo.util.NisExtendApiId;
import org.nem.core.connect.HttpJsonPostRequest;
import org.nem.core.connect.client.NisApiId;
import org.nem.core.messages.PlainMessage;
import org.nem.core.model.*;
import org.nem.core.model.mosaic.*;
import org.nem.core.model.namespace.Namespace;
import org.nem.core.model.namespace.NamespaceId;
import org.nem.core.model.namespace.NamespaceIdPart;
import org.nem.core.model.ncc.MosaicDefinitionMetaDataPair;
import org.nem.core.model.ncc.MosaicIdSupplyPair;
import org.nem.core.model.ncc.NemAnnounceResult;
import org.nem.core.model.ncc.RequestAnnounce;
import org.nem.core.model.primitive.Amount;
import org.nem.core.model.primitive.Quantity;
import org.nem.core.model.primitive.Supply;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.serialization.Deserializer;
import org.nem.core.time.TimeInstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Created by wang on 2016/07/27.
 */
@Service
public class MosaicService {

    private static final Logger log = LoggerFactory.getLogger(MosaicService.class);

    @Autowired
    private MijinUtil mijinUtil;

    public MijinNamespace getNamespace(String name) {
        log.info("get namespace {} data from mijin", name);

        return getNamespaceData(name);
    }


    private MijinNamespace getNamespaceData(String namespaceFullName) {
        final CompletableFuture<Deserializer> future = mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_NAMESPACE,
                "namespace=" + namespaceFullName
        );

        MijinNamespace namespace = new MijinNamespace();

        future.thenAccept(d -> {
            log.info("--------------------------------------------------------------------------------");

            namespace.setFqn(d.readString("fqn"));
            namespace.setOwner(d.readString("owner"));
            namespace.setHeight(d.readInt("height"));

            log.info(String.format("\n{\n\t\"fqn\":\"%s\",\n\t\"owner\":\"%s\",\n\t\"height\":\"%s\"\n}\n",
                    namespace.getFqn(),
                    namespace.getOwner(),
                    namespace.getHeight()
            ));

        }).exceptionally(e -> {
            log.warn(String.format("could not get %s data by %s , reason: %s",
                    namespaceFullName,
                    NisExtendApiId.NIS_REST_NAMESPACE,
                    e.getMessage()), e);

            return null;
        }).join();

        return namespace;
    }

    public MijinMosaic getMosaic(String namespaceId, String mosaicId) {
        log.info("get namespace {} mosaic {} data from mijin", namespaceId, mosaicId);

        return getMosaicDefinition(namespaceId, mosaicId);
    }

    private MijinMosaic getMosaicDefinition(String namespaceId, String mosaicId) {
        final CompletableFuture<Deserializer> future = mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_MOSAIC_DEFINITION_PAGE,
                "namespace=" + namespaceId
        );

        MijinMosaic mosaic = new MijinMosaic();

        future.thenAccept(d -> {
            log.info("get mosaic definitions successfully! ");

            final List<MosaicDefinitionMetaDataPair> metaDataPairList =
                    d.readOptionalObjectArray("data", MosaicDefinitionMetaDataPair::new);

            if (metaDataPairList != null && metaDataPairList.size() > 0) {
                metaDataPairList.stream()
                        .filter(pair -> mosaicId.equals(pair.getEntity().getId().getName()))
                        .forEach(pair -> {

                            log.info("--------------------------------------------------------------------------------");

                            mosaic.setNamespaceId(namespaceId);
                            mosaic.setName(mosaicId);
                            mosaic.setCreator(pair.getEntity().getCreator().getAddress().toString());
                            mosaic.setId(pair.getMetaData().getId());
                            mosaic.setDescription(CommonUtil.urlDecode(pair.getEntity().getDescriptor().toString()));
                            mosaic.setDivisibility(pair.getEntity().getProperties().getDivisibility());
                            mosaic.setInitialSupply(pair.getEntity().getProperties().getInitialSupply());
                            mosaic.setSupplyMutable(pair.getEntity().getProperties().isSupplyMutable());
                            mosaic.setTransferable(pair.getEntity().getProperties().isTransferable());
                            mosaic.setHasLevy(pair.getEntity().isMosaicLevyPresent());
                            if (pair.getEntity().isMosaicLevyPresent()) {
                                mosaic.setLevyType(pair.getEntity().getMosaicLevy().getType());
                                mosaic.setRecipientAddress(pair.getEntity().getMosaicLevy().getRecipient().getAddress().toString());
                                mosaic.setLevyFee(pair.getEntity().getMosaicLevy().getFee().getRaw());
                            }

                            log.info(String.format("\n{\n\t\"namespace\":\"%s\",\n\t\"mosaic\":\"%s\",\n\t\"initial supply\":%d,\n\t\"divisibility\":%d\n}\n",
                                    mosaic.getNamespaceId(),
                                    mosaic.getName(),
                                    mosaic.getInitialSupply(),
                                    mosaic.getDivisibility()
                            ));

                        });
            }

            if (mosaic.getCreator() == null) {
                log.warn("mosaic {} definition can not be found in namespace {}", mosaicId, namespaceId);
            }

        }).exceptionally(e -> {
            log.warn(String.format("could not get %s*%s definition from %s , reason: %s",
                    namespaceId,
                    mosaicId,
                    NisExtendApiId.NIS_REST_MOSAIC_DEFINITION_PAGE,
                    e.getMessage()), e);

            return null;
        }).join();

        return mosaic;
    }

    public TransactionResult sendMosaics(Account sender, TransferParameter parameter) {
        if (parameter == null || parameter.getMosaics() == null || parameter.getMosaics().isEmpty()) {
            throw new AppException(String.format("mosaic data can not be empty"));
        }

        Supplier<TransferTransaction> transactionSupplier = () -> {
            final Account recipient = new Account(Address.fromEncoded(parameter.getAddress()));

            TransferTransactionAttachment attachment = new TransferTransactionAttachment();

            if (parameter.getMessage() != null) {
                PlainMessage plainMessage = new PlainMessage(parameter.getMessage().getBytes());
                attachment.setMessage(plainMessage);
            }

            parameter.getMosaics().forEach(mosaicData -> {
                MosaicId mosaicId = new MosaicId(new NamespaceId(mosaicData.getNamespace()), mosaicData.getMosaic());
                Mosaic mosaic = new Mosaic(mosaicId, Quantity.fromValue(mosaicData.getQuantity()));

                attachment.addMosaic(mosaic);
            });

            // prepare transaction data and sign it
            final int version = 2;
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final TransferTransaction transaction = new TransferTransaction(
                    version,                                // version
                    timeInstant,                            // time instant
                    sender,                                 // sender
                    recipient,                              // recipient
                    Amount.fromNem(parameter.getAmount()),  // amount in xem
                    attachment);                            // attachment (message, mosaics)


            TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator(
                    id -> {
                        for (MosaicParameter mosaicData : parameter.getMosaics()) {
                            MosaicId mosaicId = new MosaicId(new NamespaceId(mosaicData.getNamespace()), mosaicData.getMosaic());

                            if (id.equals(mosaicId)) {
                                MosaicDefinition mosaicDefinition = retrieveMosaicDefinition(mosaicId);

                                return new MosaicFeeInformation(
                                        Supply.fromValue(mosaicDefinition.getProperties().getInitialSupply()),
                                        mosaicDefinition.getProperties().getDivisibility());
                            }
                        }

                        return null;
                    },
                    () -> mijinUtil.getNewFeeApplyForkHeight(),
                    mijinUtil.getNewFeeApplyForkHeight());

            transaction.setFee(calculator.calculateMinimumFee(transaction));

            log.info("mosaic transfer fee is {}", transaction.getFee().getNumNem());

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
                log.info(String.format("successfully send mosaics from %s to %s",
                        sender.getAddress(),
                        parameter.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not send mosaics from %s to %s, reason: %s",
                        sender.getAddress(),
                        parameter.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    public TransactionResult transferMosaic(Account sender, String recipientAddress,
                                            String namespaceName, String mosaicName, long amount) {
        TransactionResult result = null;

        Account recipient = new Account(Address.fromEncoded(recipientAddress));

        MijinMosaic mosaicDef = getMosaic(namespaceName, mosaicName);

        if (mosaicDef == null || mosaicDef.getCreator() == null) {
            throw new MijinException(
                    String.format("can not get mosaic %s definition within namespace %s", mosaicName, namespaceName));
        }

        final MosaicId mosaicId = new MosaicId(new NamespaceId(namespaceName), mosaicName);
        final Mosaic mosaic = new Mosaic(mosaicId, Quantity.fromValue(amount));
        final TransferTransactionAttachment attachment = new TransferTransactionAttachment();
        attachment.addMosaic(mosaic);

        return transferMosaic(sender, recipient, 1, attachment, mosaic, mosaicDef);
    }

    private TransactionResult transferMosaic(Account sender, Account recipient,
                                             long amountOfXem,
                                             TransferTransactionAttachment attachment,
                                             Mosaic mosaic,
                                             MijinMosaic mosaicDef) {
        if (attachment == null || attachment.getMosaics() == null || attachment.getMosaics().isEmpty()) {
            throw new AppException(String.format("mosaic %s*%s attachment can not be empty",
                    mosaicDef.getNamespaceId(), mosaicDef.getName()));
        }

        // prepare transaction data and sign it
        int version = 2;


        final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

        final TransferTransaction transaction = new TransferTransaction(
                version,                          // version
                timeInstant,                      // time instant
                sender,                           // sender
                recipient,                        // recipient
                Amount.fromNem(amountOfXem),      // amount in micro xem
                attachment);                      // attachment (message, mosaics)

        final MosaicFeeInformation feeInfo =
                new MosaicFeeInformation(Supply.fromValue(mosaicDef.getInitialSupply()), mosaicDef.getDivisibility());
        TransactionFeeCalculator calculator = new DefaultTransactionFeeCalculator(
                id -> feeInfo,
                () -> mijinUtil.getNewFeeApplyForkHeight(),
                mijinUtil.getNewFeeApplyForkHeight());
        transaction.setFee(calculator.calculateMinimumFee(transaction));

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
                log.info(String.format("successfully send %d %s*%s from %s to %s",
                        mosaic.getQuantity().getRaw(),
                        mosaicDef.getNamespaceId(),
                        mosaicDef.getName(),
                        sender.getAddress(),
                        recipient.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not send %s*%s from %s to %s, reason: %s",
                        mosaicDef.getNamespaceId(),
                        mosaicDef.getName(),
                        sender.getAddress(),
                        recipient.getAddress(),
                        result.getMessage()));
            }

        }).exceptionally(e -> {
            log.warn(String.format("could not send %s*%s from %s to %s, reason: %s",
                    mosaicDef.getNamespaceId(),
                    mosaicDef.getName(),
                    sender.getAddress(),
                    recipient.getAddress(),
                    e.getMessage()), e);

            return null;
        }).join();

        return transactionResult;
    }

    public Namespace getNamespace(final NamespaceId id) {
        final CompletableFuture<Deserializer> future = mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_NAMESPACE,
                String.format("namespace=%s", id.toString()));
        final Deserializer deserializer = future.join();
        return new Namespace(deserializer);
    }

    public TransactionResult createRootNamespace(final Account sender, final String rootNamespaceName) {
        final NamespaceIdPart namespaceIdPart = new NamespaceIdPart(rootNamespaceName);

        return createNamespace(sender, namespaceIdPart, null);
    }

    public TransactionResult createNamespace(final Account sender,
                                             final NamespaceIdPart newPart,
                                             final NamespaceId parent) {

        Supplier<ProvisionNamespaceTransaction> transactionSupplier = () -> {
            // prepare transaction data and sign it
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final ProvisionNamespaceTransaction transaction = new ProvisionNamespaceTransaction(
                    timeInstant,
                    sender,
                    newPart,
                    parent);

//            transaction.setFee(Amount.fromNem(108));
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
                log.info(String.format("successfully provisioned new namespace %s for owner %s",
                        null == parent ? newPart.toString() : parent.concat(newPart).toString(),
                        sender.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not provisioned new namespace %s for owner %s, reason: %s",
                        null == parent ? newPart.toString() : parent.concat(newPart).toString(),
                        sender.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    public MijinMosaic retrieveMosaicDefinition(String namespaceName, String mosaicName) {
        final NamespaceId namespaceId = new NamespaceId(namespaceName);
        final MosaicId mosaicId = new MosaicId(namespaceId, mosaicName);

        MijinMosaic mosaic = new MijinMosaic();

        MosaicDefinition definition = retrieveMosaicDefinition(mosaicId);

        mosaic.setNamespaceId(definition.getId().getNamespaceId().toString());
        mosaic.setName(definition.getId().getName());
        mosaic.setCreator(definition.getCreator().getAddress().toString());
        mosaic.setDescription(CommonUtil.urlDecode(definition.getDescriptor().toString()));
        mosaic.setDivisibility(definition.getProperties().getDivisibility());
        mosaic.setInitialSupply(definition.getProperties().getInitialSupply());
        mosaic.setSupplyMutable(definition.getProperties().isSupplyMutable());
        mosaic.setTransferable(definition.getProperties().isTransferable());
        mosaic.setHasLevy(definition.isMosaicLevyPresent());

        if (definition.isMosaicLevyPresent()) {
            mosaic.setLevyType(definition.getMosaicLevy().getType());
            mosaic.setRecipientAddress(definition.getMosaicLevy().getRecipient().getAddress().toString());
            mosaic.setLevyFee(definition.getMosaicLevy().getFee().getRaw());
        }

        return mosaic;
    }

    public MosaicDefinition retrieveMosaicDefinition(final MosaicId id) {
        final CompletableFuture<Deserializer> future = mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_MOSAIC_DEFINITION,
                String.format("mosaicId=%s", CommonUtil.urlEncode(id.toString())));
        final Deserializer deserializer = future.join();
        return new MosaicDefinition(deserializer);
    }


    public TransactionResult createMosaic(final Account sender, final String namespaceName, final String mosaicName) {

        final NamespaceId namespaceId = new NamespaceId(namespaceName);

        Supplier<MosaicDefinitionCreationTransaction> transactionSupplier = () -> {
            // prepare transaction data and sign it
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final MosaicDefinitionCreationTransaction transaction = new MosaicDefinitionCreationTransaction(
                    timeInstant,
                    sender,
                    createMosaicDefinition(sender, namespaceId, mosaicName));

//            transaction.setFee(Amount.fromNem(108));
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
                log.info(String.format("successfully created mosaic definition %s*%s for owner %s",
                        namespaceId.toString(),
                        mosaicName,
                        sender.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not create mosaic definition %s*%s for owner %s, reason: %s",
                        namespaceId.toString(),
                        mosaicName,
                        sender.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    // The creator of a mosaic definition must always be the sender of the transaction that publishes the mosaic definition.
    // The namespace in which the mosaic will reside is given by its id.
    // The mosaic name within the namespace must be unique.
    private MosaicDefinition createMosaicDefinition(
            final Account creator,
            final NamespaceId namespaceId,
            final String mosaicName) {
        final MosaicId mosaicId = new MosaicId(namespaceId, mosaicName);
        final MosaicDescriptor descriptor =
                new MosaicDescriptor(CommonUtil.urlEncode("provide a description for the mosaic here"));

        // This shows how properties of the mosaic can be chosen.
        // If no custom properties are supplied default values are taken.
        final Properties properties = new Properties();
        properties.put("initialSupply", Long.toString(1000000000));
        properties.put("divisibility", Long.toString(3));
        properties.put("supplyMutable", Boolean.toString(true));
        properties.put("transferable", Boolean.toString(true));
        final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

        // Optionally a levy can be supplied. It is allowed to be null.
        final MosaicLevy levy = new MosaicLevy(
                MosaicTransferFeeType.Absolute, // levy specifies an absolute value
                creator,                        // levy is send to the creator
                MosaicConstants.MOSAIC_ID_XEM,  // levy is paid in XEM
                Quantity.fromValue(1000)        // Each transfer of the mosaic will transfer 1000 micro XEM
                // from the mosaic sender to the recipient (here the creator)
        );

        return new MosaicDefinition(creator, mosaicId, descriptor, mosaicProperties, null);
    }

    public MosaicIdSupplyPair retrieveMosaicSupply(final MosaicId id) {
        final CompletableFuture<Deserializer> future = mijinUtil.getConnector().getAsync(
                mijinUtil.getMijinNodeEndpoint(),
                NisExtendApiId.NIS_REST_MOSAIC_SUPPLY,
                String.format("mosaicId=%s", CommonUtil.urlEncode(id.toString())));
        final Deserializer deserializer = future.join();
        return new MosaicIdSupplyPair(deserializer);
    }

    public TransactionResult createMosaicSupply(final Account sender, String nsName,
                                                String mosaicName, final long supplyDelta) {
        final NamespaceId namespaceId = new NamespaceId(nsName);
        final MosaicId mosaicId = new MosaicId(namespaceId, mosaicName);

        Supplier<MosaicSupplyChangeTransaction> transactionSupplier = () -> {
            // prepare transaction data and sign it
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final MosaicSupplyChangeTransaction transaction = new MosaicSupplyChangeTransaction(
                    timeInstant,
                    sender,
                    mosaicId,
                    MosaicSupplyType.Create,         // increase supply
                    Supply.fromValue(supplyDelta));  // change in supply (always in whole units, not subunits)

//            transaction.setFee(Amount.fromNem(108));
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
                log.info(String.format("successfully changed supply for mosaic %s, %d units added ",
                        mosaicId,
                        supplyDelta));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not change supply for mosaic %s, reason: %s",
                        mosaicId,
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }

    public TransactionResult createMosaic(Account sender, MosaicParameter parameter) {

        Supplier<MosaicDefinitionCreationTransaction> transactionSupplier = () -> {
            final NamespaceId namespaceId = new NamespaceId(parameter.getNamespace());
            final MosaicId mosaicId = new MosaicId(namespaceId, parameter.getMosaic());
            final MosaicDescriptor descriptor =
                    new MosaicDescriptor(CommonUtil.urlEncode(parameter.getDescription()));

            // This shows how properties of the mosaic can be chosen.
            final Properties properties = new Properties();

            properties.put("initialSupply", Long.toString(parameter.getInitialSupply()));
            properties.put("divisibility", Long.toString(parameter.getDivisibility()));
            properties.put("supplyMutable", Boolean.toString(parameter.isSupplyMutable()));
            properties.put("transferable", Boolean.toString(parameter.isTransferable()));

            final MosaicProperties mosaicProperties = new DefaultMosaicProperties(properties);

            // no levy
            final MosaicDefinition mosaicDefinition =
                    new MosaicDefinition(sender, mosaicId, descriptor, mosaicProperties, null);

            // prepare transaction data and sign it
            final TimeInstant timeInstant = mijinUtil.getTimeProvider().getCurrentTime();

            final MosaicDefinitionCreationTransaction transaction = new MosaicDefinitionCreationTransaction(
                    timeInstant,
                    sender,
                    mosaicDefinition);

//            transaction.setFee(Amount.fromNem(108));
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
                log.info(String.format("successfully created mosaic definition %s*%s for owner %s",
                        parameter.getNamespace(),
                        parameter.getMosaic(),
                        sender.getAddress()));

                transactionResult.setSuccess(true);

                transactionResult.setTransactionHash("" + result.getTransactionHash());
                transactionResult.setInnerTransactionHash("" + result.getInnerTransactionHash());
            } else {
                log.warn(String.format("could not create mosaic definition %s*%s for owner %s, reason: %s",
                        parameter.getNamespace(),
                        parameter.getMosaic(),
                        sender.getAddress(),
                        result.getMessage()));
            }

            return transactionResult;
        }));
    }
}
