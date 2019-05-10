package ru.demetrious.bluetoothdrop;

import java.util.Date;

class ExplorerElement {
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

    float getSize() {
        return size;
    }

    Date getDate() {
        return date;
    }

    boolean isSelected() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    boolean isFolder() {
        return isFolder;
    }
}
