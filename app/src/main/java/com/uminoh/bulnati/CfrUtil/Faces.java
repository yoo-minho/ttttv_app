package com.uminoh.bulnati.CfrUtil;

public class Faces {

    Celebrity celebrity;
    Emotion emotion; //0:angry, 1:disgust, 2:fear, 3:laugh, 4:neutral, 5:sad, 6:suprise, 7:smile, 8:talking
    Age age;
    Gender gender;
    Pose pose;

    public Pose getPose() {
        return pose;
    }

    public Gender getGender() {
        return gender;
    }

    public Age getAge() {
        return age;
    }

    public Emotion getEmotion() {
        return emotion;
    }

    public Celebrity getCelebrity() {
        return celebrity;
    }

    public class Celebrity {
        private String value;
        private double confidence;

        public String getValue() {
            return value;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public class Emotion {
        public String value;
        public double confidence;

        public String getValue() {
            return value;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public class Age {
        public String value;
        public double confidence;

        public String getValue() {
            return value;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public class Gender {
        public String value;
        public double confidence;

        public String getValue() {
            return value;
        }

        public double getConfidence() {
            return confidence;
        }
    }

    public class Pose {
        public String value;
        public double confidence;

        public String getValue() {
            return value;
        }

        public double getConfidence() {
            return confidence;
        }
    }

}
