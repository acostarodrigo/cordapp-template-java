package org.shield.webserver.membership;

import com.google.gson.JsonObject;
import org.shield.membership.ShieldMetadata;

public class ResponseWrapper {
    private int index;
    private String status;
    private ShieldMetadata metadata;
    private String party;
    private String bno;
    private String issued;

    public ResponseWrapper(int index, String status, ShieldMetadata metadata,String party, String bno, String issued) {
        this.index = index;
        this.status = status;
        this.metadata = metadata;
        this.party = party;
        this.bno = bno;
        this.issued = issued;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ShieldMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ShieldMetadata metadata) {
        this.metadata = metadata;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getBno() {
        return bno;
    }

    public void setBno(String bno) {
        this.bno = bno;
    }

    public String getIssued() {
        return issued;
    }

    public void setIssued(String issued) {
        this.issued = issued;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public JsonObject toJson(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("index", index);
        jsonObject.addProperty("status", status);
        jsonObject.add("metadata", metadata.toJson());
        jsonObject.addProperty("party", party);
        jsonObject.addProperty("bno", bno);
        jsonObject.addProperty("issued", issued);

        return jsonObject;
    }
}
