package org.mousejava.ipviewer.commands;

public enum CommandName {
    HELP("help"),
    RELOAD("reload"),
    LIST("list");

    private final String name;

    CommandName(String name) {
        this.name = name;
    }

    public String get() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
