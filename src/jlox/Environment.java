package jlox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment parent;
    private final Map<String, Object> values = new HashMap<String, Object>();

    Environment(Environment parent) {
        this.parent = parent;
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {

        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (parent != null) {
            return parent.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; ++i) {
            environment = environment.parent;
        }
        return environment;
    }

    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (parent != null) {
            parent.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }
}
