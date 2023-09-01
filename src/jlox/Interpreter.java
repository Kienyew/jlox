package jlox;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jlox.Expr.Assign;
import jlox.Expr.Binary;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Logical;
import jlox.Expr.Unary;
import jlox.Expr.Variable;
import jlox.Stmt.Block;
import jlox.Stmt.Expression;
import jlox.Stmt.Print;
import jlox.Stmt.Var;
import jlox.Stmt.While;
import jlox.RuntimeError;
import jlox.Environment;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    public Environment globals = new Environment(null);
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    void interpret(List<Stmt> statements) {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<clock: native function>";
            }
        });

        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private String stringify(Object value) {
        if (value == null)
            return "nil";
        if (value instanceof Double) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length() - 2);
            }
            return text;
        }
        return value.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
        case MINUS:
            checkNumberOperands(expr.operator, left, right);
            return (double) left - (double) right;
        case SLASH:
            checkNumberOperands(expr.operator, left, right);
            return (double) left / (double) right;
        case STAR:
            checkNumberOperands(expr.operator, left, right);
            return (double) left * (double) right;
        case PLUS:
            if (left instanceof Double && right instanceof Double)
                return (double) left + (double) right;

            if (left instanceof String && right instanceof String)
                return (String) left + (String) right;

            if (left instanceof String) {
                return (String) left + stringify(right);
            }

            throw new RuntimeError(expr.operator, "Operands must be two numbers or strings");
        case GREATER:
            checkNumberOperands(expr.operator, left, right);
            return (double) left > (double) right;
        case GREATER_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return (double) left >= (double) right;
        case LESS:
            checkNumberOperands(expr.operator, left, right);
            return (double) left < (double) right;
        case LESS_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return (double) left <= (double) right;
        case BANG_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return !isEqual(left, right);
        case EQUAL_EQUAL:
            checkNumberOperands(expr.operator, left, right);
            return isEqual(left, right);
        default:
            return null;
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double)
            return;
        else
            throw new RuntimeError(operator, "Operands must be numbers.");

    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null)
            return true;
        else if (left == null)
            return false;
        else
            return left.equals(right);
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
        case MINUS:
            checkNumberOperand(expr.operator, right);
            return -(double) right;
        case BANG:
            return !isTruthy(right);
        default:
            return null;
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private boolean isTruthy(Object object) {
        if (object == null)
            return false;
        else if (object instanceof Boolean)
            return (boolean) object;
        else
            return true;
    }

    @Override
    public Void visitExpressionStmt(Expression stmt) {
        evaluate(stmt.expr);
        return null;
    }

    @Override
    public Void visitPrintStmt(Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Void visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment old_environment = this.environment;
        this.environment = environment;
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = old_environment;
        }

    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else {
            if (stmt.elseBranch != null) execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {
        Object left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else if (expr.operator.type == TokenType.AND) {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);

    }

    @Override
    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null)
            value = evaluate(stmt.value);

        throw new Return(value);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            boolean isInitializer = method.name.lexeme.equals("init");
            LoxFunction function = new LoxFunction(method, environment, isInitializer);
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance)object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have property");
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance)object).set(expr.name, value);
        return null;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr.keyword, expr);
    }

}
