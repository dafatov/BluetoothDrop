package ru.demetrious.bluetoothdrop;

import java.util.Date;

public class ExplorerElement {
    private String name;
    private float size;
    private Date date;
    private boolean selected;
    private boolean isFolder;

    ExplorerElement(String name, Date date, float size, boolean isFolder, boolean selected) {
        this.name = name;
        this.date = date;
        this.size = size;
        this.isFolder = isFolder;
        this.selected = selected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }
}
