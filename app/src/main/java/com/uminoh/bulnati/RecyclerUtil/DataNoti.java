package com.uminoh.bulnati.RecyclerUtil;

public class DataNoti {

    public String title;
    public String broad;
    public String week;
    public boolean check;

    public DataNoti(String title, String broad, String week, boolean check) {
        this.title = title;
        this.broad = broad;
        this.week = week;
        this.check = check;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBroad() {
        return broad;
    }

    public void setBroad(String broad) { this.broad = broad; }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public boolean getCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }
}
