package net.skywall.fragment.view;

import android.graphics.drawable.Drawable;

import java.util.Date;

public class DisplayApp {

    private String name;
    private String packageName;
    private Drawable icon;
    private Date whitelistTime;
    private boolean isSelected;

    public DisplayApp(String name, String packageName, Drawable icon, Date whitelistTime) {
        this.name = name;
        this.packageName = packageName;
        this.icon = icon;
        this.whitelistTime = whitelistTime;
        isSelected = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public Date getWhitelistTime() {
        return whitelistTime;
    }

    public void setWhitelistTime(Date whitelistTime) {
        this.whitelistTime = whitelistTime;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
