# java-sbidinapp
### Functional examples of inapp authentication

---

This sample illustrates the API calls required to make the flow work as expected. It can be viewed as a jumping-off point for your app implementation.

**Dependencies**:

* JDK 8
* [Org.JSON](https://mvnrepository.com/artifact/org.json/json): version 20180813 is used
* [commons-codec](https://mvnrepository.com/artifact/commons-codec/commons-codec): Common codec library.
* [Apache&trade; httpclient](https://mvnrepository.com/artifact/org.apache.httpcomponents/httpclient): HTTP client library.

### Supported eIDs.

* [SbidInapp.java](./src/main/java/SbidInapp.java): Swedish BankID.

### Flow

1. Call /authorize.
2. Poll collect method until success.
3. Call complete method - the last redirect will contain CODE and STATE.
4. Call /token end-point as normal (using CODE we got in STEP 3).
5. Call /userinfo with access token. (optional)

### Application Usage

**Swedish BankID** ([SbidInapp.java](./src/main/java/SbidInapp.java))

You need to change the variable ```final String nid``` to a valid Swedish BankID test-user.
