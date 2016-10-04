package io.sjitech.demo.util;

import io.sjitech.demo.model.MijinResult;
import io.sjitech.demo.model.TransactionResult;
import org.nem.core.connect.ErrorResponseDeserializerUnion;
import org.nem.core.connect.HttpJsonPostRequest;
import org.nem.core.connect.HttpMethodClient;
import org.nem.core.connect.client.DefaultAsyncNemConnector;
import org.nem.core.connect.client.NisApiId;
import org.nem.core.crypto.KeyPair;
import org.nem.core.crypto.PrivateKey;
import org.nem.core.model.Account;
import org.nem.core.model.NetworkInfos;
import org.nem.core.model.Transaction;
import org.nem.core.model.ncc.RequestAnnounce;
import org.nem.core.node.ApiId;
import org.nem.core.node.NodeEndpoint;
import org.nem.core.serialization.BinarySerializer;
import org.nem.core.serialization.Deserializer;
import org.nem.core.time.SystemTimeProvider;
import org.nem.core.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by wang on 2016/07/21.
 */
@Component
public class MijinUtil implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(MijinUtil.class);

    protected TimeProvider timeProvider = new SystemTimeProvider();

    protected NodeEndpoint mijinNodeEndpoint = null;

    protected List<Account> existMijinAccounts = null;

    protected HttpMethodClient<ErrorResponseDeserializerUnion> client;

    protected DefaultAsyncNemConnector<ApiId> connector;

    @Autowired
    private MijinConfig config;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        log.info("initialize mijin environment on ApplicationReadyEvent");

        // choose mijin network
        NetworkInfos.setDefault(NetworkInfos.fromFriendlyName("mijinnet"));

        mijinNodeEndpoint = new NodeEndpoint(config.getServerNodeProtocol(),
               config.getServerNodeIp(), config.getServerNodePort());

        existMijinAccounts = config.getExistAccountPrivateKeys().stream()
                .map(PrivateKey::fromHexString)
                .map(pk -> new Account(new KeyPair(pk)))
                .collect(Collectors.toList());

        client = new HttpMethodClient<>(config.getConnectionTimeout(),
               config.getSocketTimeout(), config.getRequestTimeout());


        connector = new DefaultAsyncNemConnector<>(client, r -> { throw new RuntimeException(); });
        connector.setAccountLookup(Account::new);
    }

    public TimeProvider getTimeProvider() {
        return timeProvider;
    }

    public NodeEndpoint getMijinNodeEndpoint() {
        return mijinNodeEndpoint;
    }

    public List<Account> getExistMijinAccounts() {
        return existMijinAccounts;
    }

    public HttpMethodClient<ErrorResponseDeserializerUnion> getClient() {
        return client;
    }

    public DefaultAsyncNemConnector<ApiId> getConnector() {
        return connector;
    }

    public <R> R sendGetRequest(ApiId apiId, String query,
                                Function<Deserializer, R> resultHandler) {
        final CompletableFuture<Deserializer > future =  getConnector().getAsync(
                getMijinNodeEndpoint(), apiId, query );

        MijinResult<R> result = new MijinResult<R>();

        future.thenAccept(d -> {
            result.setRaw(resultHandler.apply(d));
        }).exceptionally(e -> {
            log.warn(String.format("error on GET %s with query %s, reason: %s",
                    apiId.toString(),
                    query,
                    e.getMessage()), e);

            return null;
        }).join();

        return result.getRaw();
    }

    public TransactionResult postTransactionAnnounce(Account sender,
                                                    Supplier<? extends Transaction> transactionFactory,
                                                    Function<Deserializer, TransactionResult> resultHandler) {
        final Transaction transaction = transactionFactory.get();

        final byte[] data = BinarySerializer.serializeToBytes(transaction.asNonVerifiable());
        final RequestAnnounce request = new RequestAnnounce(data, transaction.getSignature().getBytes());

        final CompletableFuture<Deserializer> future = getConnector().postAsync(
                getMijinNodeEndpoint(),
                NisApiId.NIS_REST_TRANSACTION_ANNOUNCE,
                new HttpJsonPostRequest(request));

        MijinResult<TransactionResult> result = new MijinResult<>();

        future.thenAccept(d -> {
            result.setRaw(resultHandler.apply(d));
        }).exceptionally(e -> {
            log.warn(String.format("account %s transaction(type: %d) failed, reason: %s",
                    sender.getAddress(),
                    transaction.getType(),
                    e.getMessage()), e);

            return null;
        }).join();

        return result.getRaw();
    }
}
