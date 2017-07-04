package numenalibs.co.numenalib.models;


import java.util.ArrayList;
import java.util.List;

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
