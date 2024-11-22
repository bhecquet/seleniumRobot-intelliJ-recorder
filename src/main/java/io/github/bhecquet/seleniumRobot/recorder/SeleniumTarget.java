package io.github.bhecquet.seleniumRobot.recorder;

public class SeleniumTarget {

    private String targetType;
    private String targetSelector;

    public SeleniumTarget(String targetSelector, String targetType) {
        this.targetType = targetType;
        this.targetSelector = targetSelector;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetSelector() {
        return targetSelector;
    }
}
