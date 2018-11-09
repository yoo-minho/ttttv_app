package com.uminoh.bulnati.RecyclerUtil;

public class DataChat {

    public static final int ME_TYPE=0;
    public static final int YOU_TYPE=1;
    public static final int ENTRY_TYPE=2;

    public int type;
    public String message;
    public String nickname;
    public String date;

    public DataChat(int type, String message, String nickname, String date) {
        this.message = message;
        this.nickname = nickname;
        this.date = date;
        this.type = type;
    }

    public int getType(){return type; }

    public void setType(int type){this.type = type; }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

}
