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
    SettingsElement(@NonNull Type type, @NonNull String name, @NonNull String description, @NonNull T... vars) {
        if (vars.length == 0 && type == Type.Directory || vars.length == 2 && type == Type.ToggleButton || vars.length > 2 && type == Type.Spinner)
            if (vars.length == 0 || (!vars.getClass().equals(Serializable[].class) &&
                    (vars[0].getClass().equals(Integer.class) || vars[0].getClass().equals(Float.class) ||
                            vars[0].getClass().equals(Double.class) || vars[0].getClass().equals(String.class) ||
                            vars[0].getClass().equals(Boolean.class) || vars[0].getClass().equals(Character.class) ||
                            vars[0].getClass().equals(Byte.class) || vars[0].getClass().equals(Short.class) ||
                            vars[0].getClass().equals(Long.class) || vars[0].getClass().isPrimitive()))) {
                ID = "ru.demetrious.bluetoothdrop.setting" + index++;
                this.name = name;
                this.description = description;
                this.type = type;
                this.vars = vars;
            } else throw new ClassFormatException(vars.getClass());
        else throw new TypeSettingsException(type, vars.length);
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

class ClassFormatException extends ExceptionInInitializerError {
    ClassFormatException(Class oClass) {
        super("Need primitive massive or their analogs massive classes but found: " + oClass.getSimpleName());
    }
}

class TypeSettingsException extends ExceptionInInitializerError {
    TypeSettingsException(SettingsElement.Type type, int length) {
        super("Type " + type + " can't have amount " + length);
    }
}