# Mijin demo application

This may be the first [mijin](http://mijin.io/ja/) demo application written in Java 8.

Mijin is next generation private blockchain platform inspired by [NEM](http://nem.io/) platform.

I studied [NEM sample](https://github.com/BloodyRookie/nem-samples) project.(Mijin use same [API](http://bob.nem.ninja/docs/) with nem) This project has become obsolete and is not well compatible with mijin.

So I write this project to demo how to develop a REST mijin application with [Spring Boot](http://projects.spring.io/spring-boot/).

Until now(2016/08) mijin is not released yet, I use the [public beta test environment supplied by sakura internet](http://mijin.io/ja/mijin-cloud-chain-beta-test).

There's a Japanese slide for introducing Mijin development, check it [here](https://github.com/sjitech/mijin-dev-slide).

### Status ###

* 2016-11-15
    + upgrade spring boot to 1.4.2.RELEASE
    + update for nem.core 0.6.82-beta, [https://blog.nem.io/nem-updated-0-6-82/](https://blog.nem.io/nem-updated-0-6-82/)
    + update mosaic fee calculation for new fee caculator API. On private environment, assume new fee will be applied from block start
    + add new transaction get API endpoint in API enum
    + refined build.gradle

* Under development
    + No UI, use `curl` to send http request
    + RDB access function is omitted
    + Error is not well handled
    + A lot of duplicated code exists
    + Not consider how to protect user's private key

### Development Requirements ###

* Java 8
* Spring boot 1.4.1
* Gradle 3.1
* NEM core 0.6.77-beta(used as mijin client library)

### Install and Running ###

Just clone this project and launch with the following command:

```bash
$ cd [project dir]
$ mv src/main/resources/config/application.yml.sample src/main/resources/config/application.yml
$ # edit application.yml to match your environment, read the following configuration section
$ ./gradlew bootRun
```

Send some REST request with `curl` command:

```bash
$ curl -sS http://localhost:8080/account/new
```

### Configuration ###

* Log(use log4j2)
    + src/main/resources/log4j2.yml
    + output log to `./logs` directory
* Application configuration
    + src/main/resources/config/application.yml

```yml
  mijin:
    serverNodeIp: 59.106.209.120
    serverNodePort: 7895
    serverNodeProtocol: http
    existAccountPrivateKeys:
      - [root account private key. should have some XEM balance.(above 10000XEM is preferred)]
      - 00ba193de39df1e0f583670dda5a351c2a369a21db731821bd49bda95e69c944b3
      - [some test account private key]
      - ...
```


### Function ###

* System(blockchain and node) related
    + simple get request, not implement yet
* Account related
    + create new account

    ```
    $ curl -sS http://localhost:8080/account/new
    ```

    + get account data

    ```
    $ curl -sS http://localhost:8080/account/get/{address}
    ```

    + get account owned mosaic data

    ```
    $ curl -sS http://localhost:8080/account/mosaic/get/{address}
    ```

    + get account unconfirmed transactions

    ```
    $ curl -sS http://localhost:8080/account/transaction/unconfirmed/{address}
    ```

    + get account transactions(history)

    ```
    $ curl -sS http://localhost:8080/account/transaction/all/{address}
    ```

* Mosaic related
    + create namespace(root namespace example)

    ```
    $ curl -sS http://localhost:8080/mosaic/namespace/create/{namespace name}
    ```

    + create mosaic

    ```
    $ curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "namespace" : "{namespace name}", "mosaic": "{mosaic name}", "description" : "{mosaic description}", "initialSupply" : 899999999, "divisibility" : 3, "supplyMutable" : true, "transferable": true}' http://localhost:8080/mosaic/new
    ```

    + get namespace definition(only creator can access this data)

    ```
    $ curl -sS http://localhost:8080/mosaic/namespace/get/{namespace name}
    ```

    + get mosaic definition(only creator can access this data)

    ```
    $ curl -sS http://localhost:8080/mosaic/get/{namespace name}/{mosaic name}
    ```

    + mosaic supply change
        - Not implemented yet
        - just post MosaicSupplyChangeTransaction to NIS Node

* Transfer transaction related
    + transfer XEM (from root account)

    ```
    $ curl -sS http://localhost:8080/transfer/simple/{address}/{amount}
    ```

    + transfer XEM with message (from root account)

    ```
    $  curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "address" : "{recipient address}", "message": "{message text}", "amount" : {amount of XEM},  "secureMessage": {true|false}, "publicKeyHexString": "{recipient public key(used for encode secure message)}" }' http://localhost:8080/transfer/message
    ```

    + transfer mosaics (from root account, amount is fixed, [read more](http://bob.nem.ninja/docs/#version-2-transfer-transactions))

    ```
    $  curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "address" : "{recipient address}", "mosaics": [{ "namespace" : "{namespace name}", "mosaic" : "{mosaic name}", "quantity" : {send quantity} }], "amount" : 1}' http://localhost:8080/mosaic/send
    ```

* Multisig transaction related
    + create multisig account(convert a normal account to 2 of 3 multisig account)

    ```
    $  curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "signerPrivateKey" : "{to be converted account private key}", "addCosignerPublicKeys": ["{cosigner 1 public key}", "{cosigner 2 public key}", "..."], "relativeMinCosignerNum" : 2}' http://localhost:8080/multisig/create
    ```

    + modify multisig account(add one cosigner, change account to 3 of 4 multisig account)

    ```
    $  curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "signerPrivateKey" : "{cosigner private key}", "multisigPublicKey" : "multisig account public key", "addCosignerPublicKeys" : ["to be added account public key"], "relativeMinCosignerNum" : 1 }' http://localhost:8080/multisig/modify
    ```


    + transfer XEM or mosaics from multisig account

    ```
    $  curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "signerPrivateKey" : "{cosigner private key}", "multisigPublicKey" : "multisig account public key", "transferParameter" : {"address" : "{recipient address}", "amount" : {transfer amount of XEM} } }' http://localhost:8080/multisig/transfer
    ```

    + sign multisig transaction (by cosigner)

    ```
    $ curl -sS -H "Accept: application/json" -H "Content-type: application/json" -X POST -d '{ "signerPrivateKey" : "{cosigner private key}", "multisigPublicKey" : "{multisig account public key}", "innerHashString" : "{inner transaction hash value}" }' http://localhost:8080/multisig/sign
    ```

### Copyright ###

* You can download and modify the code without any restriction and limitation.
* I do not guarantee the code will work on the future version of mijin.
* Feedback is welcome, but I am not sure response it quickly.
* Use [issues](https://bitbucket.org/samfisher/mijin-demo-backend/issues?status=new&status=open) to report bug.

**Happy Hacking**
