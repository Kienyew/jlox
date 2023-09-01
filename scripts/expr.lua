local function trim(s)
	return s:match("^%s(.-)%s*$")
end

local function define_type(file, base_name, class_name, fields)
	local code
	code = string.format(
		[[
        static class %s extends %s {  
    ]],
		class_name,
		base_name
	)

	file:write(code)

	-- constructor
	code = string.format(
		[[
        %s(%s) {
    ]],
		class_name,
		fields
	)

	file:write(code)

	for field in string.gmatch(fields, "[^,]+") do
		field = trim(field)
		local name = string.match(field, "%g+ (%a+)")
		file:write(string.format("this.%s = %s;\n", name, name))
	end
	file:write("}\n")

	file:write("\n")
	file:write(string.format(
		[[
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visit%s%s(this);
        }
    ]],
		class_name,
		base_name
	))

	for field in string.gmatch(fields, "[^,]+") do
		file:write(string.format("final %s;\n", field))
	end

	file:write("}\n")
end

local function define_visitor(file, base_name, types)
	file:write("interface Visitor<R> {\n")
	for _, type in pairs(types) do
		local class_name = string.match(type, "(%a+):.*")
		file:write(string.format("R visit%s%s(%s %s);", class_name, base_name, class_name, string.lower(base_name)))
	end
	file:write("}")
end

local function define_ast(output_dir, base_name, types)
	local path = string.format("%s/%s.java", output_dir, base_name)
	local file = assert(io.open(path, "w"))
	local code = string.format(
		[[ 
        package jlox;

        import java.util.List;

        abstract class %s {
    ]],
		base_name
	)

	file:write(code)

	define_visitor(file, base_name, types)

	for _, type in pairs(types) do
		local class_name, fields = string.match(type, "(%a+):(.*)")
		define_type(file, base_name, class_name, fields)
	end

	file:write("\n")
	file:write("abstract <R> R accept(Visitor<R> visitor);")

	file:write("}")
	file:close()
end

local function main()
	if #arg ~= 1 then
		local message = string.format("Usage: %s <output directory>", arg[0])
		io.stderr:write(message)
		os.exit(65)
	end

	local output_dir = arg[1]
	define_ast(output_dir, "Expr", {
		"Binary: Expr left, Token operator, Expr right",
		"Grouping: Expr expression",
		"Literal: Object value",
		"Unary: Token operator, Expr right",
		"Variable: Token name",
		"Assign: Token name, Expr value",
		"Logical: Expr left, Token operator, Expr right",
		"Call: Expr callee, Token paren, List <Expr> arguments",
		"Get: Expr object, Token name",
		"Set: Expr object, Token name, Expr value",
		"This: Token keyword",
	})

	define_ast(output_dir, "Stmt", {
		"Expression: Expr expr",
		"Print: Expr expression",
		"Var: Token name, Expr initializer",
		"Block: List<Stmt> statements",
		"If: Expr condition, Stmt thenBranch, Stmt elseBranch",
		"While: Expr condition, Stmt body",
		"Function: Token name, List<Token> params, List<Stmt> body",
		"Return: Token keyword, Expr value",
		"Class: Token name, List<Stmt.Function> methods",
	})
end

main()
