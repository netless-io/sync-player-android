package com.agora.netless.syncplayer.misc;

public class UseCase {
    public String title;
    public String describe;

    public Class displayClass;

    public UseCase(String title, String describe, Class displayClass) {
        this.title = title;
        this.describe = describe;
        this.displayClass = displayClass;
    }
}
