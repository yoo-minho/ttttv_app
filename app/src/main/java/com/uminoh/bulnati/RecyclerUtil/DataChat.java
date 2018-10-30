package com.uminoh.bulnati.RecyclerUtil;

public class DataChat {

    public String message;
    public String nickname;
    public boolean isMe;
    public String date;

    public DataChat(String message, String nickname, boolean isMe, String date) {
        this.message = message;
        this.nickname = nickname;
        this.isMe = isMe;
        this.date = date;
    }

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

    public boolean getIsMe() {
        return isMe;
    }

    public void setIsMe(boolean isMe) {
        this.isMe = isMe;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

}
