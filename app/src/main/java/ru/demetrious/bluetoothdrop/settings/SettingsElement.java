package ru.demetrious.bluetoothdrop.settings;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class SettingsElement<T> {
    private static int index = 0;
    private final String ID;
    private String name, description;
    private T[] vars;
    private Type type;

    @SafeVarargs
    SettingsElement(Type type, String name, String description, @NonNull T... vars) {
        ID = "ru.demetrious.bluetoothdrop.setting" + index++;
        this.name = name;
        this.description = description;
        this.type = type;
        if ((type == Type.Directory && vars.length == 0 || type == Type.ToggleButton && vars.length == 2 || type == Type.Spinner && vars.length > 2) && !vars.getClass().equals(Serializable[].class))
            this.vars = vars;
        else if (type == Type.Directory)
            throw new NumberFormatException("Vars is must be empty");
        else if (type == Type.ToggleButton)
            throw new NumberFormatException("Vars is must have two arguments");
        else if (type == Type.Spinner)
            throw new NumberFormatException("Error in spinner");
        else
            throw new NullPointerException("Setting include different types");
    }

    Type getType() {
        return type;
    }

    String getID() {
        return ID;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    T[] getVars() {
        return vars;
    }

    enum Type {
        ToggleButton, Spinner, Directory
    }
}
