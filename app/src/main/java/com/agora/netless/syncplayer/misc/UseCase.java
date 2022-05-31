package com.agora.netless.syncplayer.misc;

import android.app.Activity;

public class UseCase {
    public String title;
    public String describe;

    public Class<? extends Activity> displayClass;

    public UseCase(String title, String describe, Class<? extends Activity> displayClass) {
        this.title = title;
        this.describe = describe;
        this.displayClass = displayClass;
    }
}
