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
#### Register a user

```java
byte[] publickey = ....
byte[] secretkey = ....
String TESTNAME = "test";
String TESTORG = "testorg";
String TESTAPPDATA = "testappdata";
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

Gradle
Include dependency using Gradle

```java
compile 'com.github.numenalibs:numena-androidlib:0.2.1'
```



