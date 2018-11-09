package com.uminoh.bulnati.RecyclerUtil;

public class DataProgram {

    private String imgUrl;
    private String broadcastStation;
    private String programTitle;
    private String programTime;
    private int total;
    private String programIntro;
    private String programRating;
    private int msgNew;

    public DataProgram(String imgUrl, String broadcastStation, String programTitle, String programTime, int total, String programIntro, String programRating, int msgNew) {
        this.imgUrl = imgUrl;
        this.broadcastStation = broadcastStation;
        this.programTitle = programTitle;
        this.programTime = programTime;
        this.total = total;
        this.programIntro = programIntro;
        this.programRating = programRating;
        this.msgNew = msgNew;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }

    public String getBroadcastStation() { return broadcastStation; }

    public void setBroadcastStation(String broadcastStation) { this.broadcastStation = broadcastStation; }

    public String getProgramTitle() {
        return programTitle;
    }

    public void setProgramTitle(String programTitle) {
        this.programTitle = programTitle;
    }

    public String getProgramTime() {
        return programTime;
    }

    public void setProgramTime(String programTime) {
        this.programTime = programTime;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getProgramIntro() {
        return programIntro;
    }

    public void setProgramIntro(String programIntro) {
        this.programIntro = programIntro;
    }

    public String getProgramRating() {
        return programRating;
    }

    public void setProgramRating(String programRating) {
        this.programRating = programRating;
    }

    public int getMsgNew() {
        return msgNew;
    }

    public void setMsgNew(int msgNew) {
        this.msgNew = msgNew;
    }



}
