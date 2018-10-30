package com.uminoh.bulnati.CfrUtil;

public class Info {

    Integer faceCount;
    Size size;

    public Integer getFacecount() {
        return faceCount;
    }

    public Size getSize() {
        return size;
    }

    public class Size {
        Integer width;
        Integer height;

        public Integer getHeight() {
            return height;
        }

        public Integer getWidth() {
            return width;
        }
    }

}
