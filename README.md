# Numena Android SDK

Android SDK for Numena Communication

### Prerequisites

It is required to have this line removed from the Manifest of your application to make libsodium work
```
android:allowBackup="true"
```

Also the minimumSdkVersion can be no lower than 18
```
minSdkVersion 18
```

## Usage

#### Get the Numena object

```java
Numena numena = Numena.getInstance();
```

It is recommended to close the numena connection when the activity stops
```java
 @Override
    protected void onStop() {
        super.onStop();
        numena.getMessageHandler().closeSocket();
    }
```

#### Register a user

```java
byte[] publickey = ....
byte[] secretkey = ....
String TESTNAME = "test";
byte[] TESTORG = "testorg".getBytes();
byte[] TESTAPPDATA = "testappdata".getBytes();
numena.getMessageHandler().unregister(publickey, secretkey,TESTNAME , TESTORG, TESTAPPDATA, new ResultsListener<NumenaResponse>() {
                        @Override
                        public void onCompletion(NumenaResponse result) {
                          
                        }
                    });
```


#### Query users

```java
String query = "numenausers";
numena.getMessageHandler().getUsers(query, new ResultsListener<NumenaResponse>() {
                        @Override
                        public void onCompletion(NumenaResponse result) {
                        }
                    });
```
#### StoreObject for a list of NumenaUsers
```java
List<NumenaUser> numenaUsers = new ArrayList<>();
byte[] content = ....
byte[] organisationId = ....
byte[] appId = ....
boolean writePermission = true;
boolean readPermission = true;
numena.getMessageHandler().storeObject(numenaUsers, content, organisationId, appId, writePermission, readPermission, new ResultsListener<NumenaResponse>() {
            @Override
            public void onCompletion(NumenaResponse result) {

            }
        });

```

### Instant Messaging
In order to enable instant messaging, it is required by the user to subscribe to his own publickey.
To handle the results from every message received, a NumenaChatHandler is required.
This handler is used as an argument when subcribing
```java
NumenaChatHandler handler = new NumenaChatHandler() {
            @Override
            public void onMessage(byte[] bytes) {

            }
        };

```
The two null arguments are the public- and secretkey which is not required. If not provided the first forged keys will be used.
```java
numena.getMessageHandler().subscribe(null, null, TESTORGANISATION, TESTAPPID, handler, newResultsListener<NumenaResponse>() {
                    @Override
                    public void onCompletion(NumenaResponse numenaResponse) {

                    }
                });

```
#### NumenaCryptoBox
You can use the NumenaCryptobox to enable another layer of encryption.
A keypair of encryptionkeys is required in order to make this work.
This is a keypair which you should register yourself. Put this these in appdata, formattet in e.g. JSON
Also put these newly forged keys inside the cryptobox. This way it will try to decrypt the incoming message automatically.
```java
NumenaCryptoBox cryptoBox = numena.getMessageHandler().getNumenaCryptoBox();
NumenaKeyPair keyPair = cryptoBox.generateAppKey("PKEYNAME","SKEYNAME");
List<NumenaKey> keys = new ArrayList<>();
keys.add(keyPair.getSecretKey());
cryptoBox.refreshSecretKeyList(keys);
```
This is how you send a message encrypted with a selected users publickey
```java
//The keypair from before
byte[] myAppPublicKey = ....
byte[] myAppSecretKey = ....
//The publickey belonging to the user
byte[] selectedPublickey = ....
//Here the appMessage is created and encrypted.
byte[] appMessage = cryptoBox.createEncryptedAppMessage(myAppPublicKey,selectedPublicKey,myAppSecretKey, messageText.getBytes());
```
This is how you can use the cryptobox to get the content when an appmessage is received.
It is recommended to put these lines of code inside the "onMessage" method in the NumenaChatHandler. This way you can instantly decrypt the users message.
```java
try {
    decrypted = cryptoBox.decryptAppMessage(bytes,null);
} catch (NumenaLibraryException e) {
    e.printStackTrace();
}
```


#### NumenaResponse 
```java
public class NumenaResponse {

    private String status;
    private List<NumenaObject> numenaObjects = new ArrayList<>();

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public List<NumenaObject> getNumenaObjects() {
        return numenaObjects;
    }

    public void setNumenaObjects(List<NumenaObject> numenaObjects) {
        this.numenaObjects.clear();
        this.numenaObjects.addAll(numenaObjects);
    }
}
```


### Installing

Include dependency using Gradle

```java
compile 'com.github.numenalibs:numena-androidlib:0.2.1'
```



