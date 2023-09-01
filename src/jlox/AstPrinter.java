package jlox;

import jlox.Expr.Assign;
import jlox.Expr.Binary;
import jlox.Expr.Call;
import jlox.Expr.Grouping;
import jlox.Expr.Literal;
import jlox.Expr.Logical;
import jlox.Expr.Unary;
import jlox.Expr.Variable;

public class AstPrinter implements Expr.Visitor<String> {
	String print(Expr expr) {
		return expr.accept(this);
	}

	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();
		builder.append('(');
		builder.append(name);
		for (Expr expr : exprs) {
			builder.append(' ');
			builder.append(expr.accept(this));
		}
		builder.append(')');
		return builder.toString();
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		if (expr.value == null)
			return "nil";
		else
			return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesize(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		return String.format("(var %s %s)", expr.name.lexeme, print(expr));
	}

	@Override
	public String visitAssignExpr(Assign expr) {
		return String.format("(= %s %s)", expr.name.lexeme, print(expr.value));
	}

	@Override
	public String visitLogicalExpr(Logical expr) {
		return parenthesize(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitCallExpr(Call expr) {
		// TODO Auto-generated method stub
		return null;
	}
}
