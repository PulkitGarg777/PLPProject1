package delphi;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.antlr.v4.runtime.tree.ParseTree;

public final class Interpreter extends DelphiBaseVisitor<Object> {
    private final RuntimeEnvironment env = new RuntimeEnvironment();
    private final Map<String, TypeDef> types = new HashMap<>();
    private final Deque<ClassDef> classContext = new ArrayDeque<>();
    private Scanner input;
    private PrintStream output;

    public void execute(ParseTree tree, Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
        RuntimeEnvironment.clearClassRegistry();
        registerBuiltins();
        collectTypes(tree);
        validateInterfaces();
        visit(tree);
    }

    private void registerBuiltins() {
        types.put(normalize("integer"), new IntegerTypeDef());
    }

    private void collectTypes(ParseTree tree) {
        if (tree instanceof DelphiParser.ProgramContext programContext) {
            DelphiParser.TypeSectionContext typeSection = programContext.block().typeSection();
            if (typeSection == null) {
                return;
            }
            for (DelphiParser.TypeDeclContext decl : typeSection.typeDecl()) {
                String name = normalize(decl.IDENT().getText());
                DelphiParser.TypeSpecContext spec = decl.typeSpec();
                if (spec.classType() != null) {
                    ClassDef classDef = buildClassDef(name, spec.classType());
                    types.put(name, classDef);
                    RuntimeEnvironment.registerClass(classDef);
                } else if (spec.interfaceType() != null) {
                    InterfaceDef interfaceDef = buildInterfaceDef(name, spec.interfaceType());
                    types.put(name, interfaceDef);
                } else {
                    types.put(name, types.get(normalize(spec.typeId().getText())));
                }
            }
        }
    }

    private ClassDef buildClassDef(String name, DelphiParser.ClassTypeContext ctx) {
        ClassDef classDef = new ClassDef(name);
        if (ctx.classInheritance() != null) {
            List<DelphiParser.TypeIdContext> ids = ctx.classInheritance().typeIdList().typeId();
            if (!ids.isEmpty()) {
                classDef.setBaseClassName(normalize(ids.get(0).getText()));
                for (int i = 1; i < ids.size(); i++) {
                    classDef.addInterfaceName(normalize(ids.get(i).getText()));
                }
            }
        }

        for (DelphiParser.VisibilitySectionContext section : ctx.classBody().visibilitySection()) {
            Visibility visibility = Visibility.PUBLIC;
            if (section.visibilitySpec() != null) {
                visibility = toVisibility(section.visibilitySpec());
            }
            for (DelphiParser.ClassMemberDeclContext member : section.classMemberDecl()) {
                if (member.fieldDecl() != null) {
                    addFields(classDef, member.fieldDecl(), visibility);
                } else if (member.constructorDecl() != null) {
                    MethodDef methodDef = buildMethod(classDef, member.constructorDecl().IDENT().getText(),
                            member.constructorDecl().formalParams(), member.constructorDecl().block(), visibility, true, false);
                    classDef.addConstructor(methodDef);
                } else if (member.destructorDecl() != null) {
                    MethodDef methodDef = buildMethod(classDef, member.destructorDecl().IDENT().getText(),
                            member.destructorDecl().formalParams(), member.destructorDecl().block(), visibility, false, true);
                    classDef.setDestructor(methodDef);
                } else if (member.methodDecl() != null) {
                    MethodDef methodDef = buildMethod(classDef, member.methodDecl().IDENT().getText(),
                            member.methodDecl().formalParams(), member.methodDecl().block(), visibility, false, false);
                    classDef.addMethod(methodDef);
                }
            }
        }

        return classDef;
    }

    private InterfaceDef buildInterfaceDef(String name, DelphiParser.InterfaceTypeContext ctx) {
        List<String> methods = new ArrayList<>();
        for (DelphiParser.MethodSignatureContext signature : ctx.interfaceBody().methodSignature()) {
            methods.add(normalize(signature.IDENT().getText()));
        }
        return new InterfaceDef(name, methods);
    }

    private void addFields(ClassDef classDef, DelphiParser.FieldDeclContext ctx, Visibility visibility) {
        String typeName = normalize(ctx.typeId().getText());
        for (var ident : ctx.identList().IDENT()) {
            String fieldName = normalize(ident.getText());
            classDef.addField(new FieldDef(fieldName, typeName, visibility, classDef));
        }
    }

    private MethodDef buildMethod(ClassDef classDef, String rawName, DelphiParser.FormalParamsContext params,
            DelphiParser.BlockContext block, Visibility visibility, boolean constructor, boolean destructor) {
        List<String> names = new ArrayList<>();
        List<String> typesList = new ArrayList<>();
        if (params != null) {
            for (DelphiParser.ParamDeclContext param : params.paramDecl()) {
                String typeName = normalize(param.typeId().getText());
                for (var ident : param.identList().IDENT()) {
                    names.add(normalize(ident.getText()));
                    typesList.add(typeName);
                }
            }
        }
        return new MethodDef(normalize(rawName), names, typesList, visibility, classDef, block, constructor, destructor);
    }

    private Visibility toVisibility(DelphiParser.VisibilitySpecContext ctx) {
        if (ctx.PUBLIC() != null) {
            return Visibility.PUBLIC;
        }
        if (ctx.PRIVATE() != null) {
            return Visibility.PRIVATE;
        }
        return Visibility.PROTECTED;
    }

    private void validateInterfaces() {
        for (TypeDef typeDef : types.values()) {
            if (typeDef instanceof ClassDef classDef) {
                for (String ifaceName : classDef.getInterfaceNames()) {
                    TypeDef ifaceDef = types.get(ifaceName);
                    if (!(ifaceDef instanceof InterfaceDef interfaceDef)) {
                        throw new RuntimeException("Unknown interface: " + ifaceName);
                    }
                    for (String methodName : interfaceDef.getMethodNames()) {
                        MethodDef methodDef = classDef.resolveMethod(methodName);
                        if (methodDef == null) {
                            throw new RuntimeException("Class " + classDef.getName() + " missing interface method: " + methodName);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Object visitProgram(DelphiParser.ProgramContext ctx) {
        return visit(ctx.block());
    }

    @Override
    public Object visitBlock(DelphiParser.BlockContext ctx) {
        return executeBlock(ctx, true);
    }

    private Object executeBlock(DelphiParser.BlockContext ctx, boolean createScope) {
        if (createScope) {
            env.pushScope();
        }
        if (ctx.varSection() != null) {
            declareVars(ctx.varSection());
        }
        Object result = visit(ctx.compoundStatement());
        if (createScope) {
            env.popScope();
        }
        return result;
    }

    private void declareVars(DelphiParser.VarSectionContext ctx) {
        for (DelphiParser.VarDeclContext decl : ctx.varDecl()) {
            String typeName = normalize(decl.typeId().getText());
            for (var ident : decl.identList().IDENT()) {
                env.define(normalize(ident.getText()), defaultValueFor(typeName));
            }
        }
    }

    private Object defaultValueFor(String typeName) {
        if ("integer".equals(typeName)) {
            return 0;
        }
        return null;
    }

    @Override
    public Object visitCompoundStatement(DelphiParser.CompoundStatementContext ctx) {
        if (ctx.statementList() == null) {
            return null;
        }
        return visit(ctx.statementList());
    }

    @Override
    public Object visitStatementList(DelphiParser.StatementListContext ctx) {
        Object result = null;
        for (DelphiParser.StatementContext stmt : ctx.statement()) {
            result = visit(stmt);
        }
        return result;
    }

    @Override
    public Object visitAssignment(DelphiParser.AssignmentContext ctx) {
        Object value = visit(ctx.expr());
        assignVariable(ctx.variable(), value);
        return value;
    }

    @Override
    public Object visitProcedureCall(DelphiParser.ProcedureCallContext ctx) {
        return visit(ctx.callExpr());
    }

    @Override
    public Object visitIfStatement(DelphiParser.IfStatementContext ctx) {
        boolean cond = asBoolean(visit(ctx.expr()));
        if (cond) {
            return visit(ctx.statement(0));
        }
        if (ctx.statement().size() > 1) {
            return visit(ctx.statement(1));
        }
        return null;
    }

    @Override
    public Object visitWhileStatement(DelphiParser.WhileStatementContext ctx) {
        while (asBoolean(visit(ctx.expr()))) {
            visit(ctx.statement());
        }
        return null;
    }

    @Override
    public Object visitCallExpr(DelphiParser.CallExprContext ctx) {
        List<String> parts = extractParts(ctx.variable());
        if (parts.size() == 1) {
            String name = parts.get(0);
            if ("readln".equals(name)) {
                handleRead(ctx.argList());
                return null;
            }
        }

        List<Object> args = new ArrayList<>();
        if (ctx.argList() != null) {
            for (DelphiParser.ExprContext expr : ctx.argList().expr()) {
                args.add(visit(expr));
            }
        }

        if (parts.size() == 1 && "writeln".equals(parts.get(0))) {
            handleWrite(args);
            return null;
        }

        if (isTypeName(parts.get(0))) {
            if (parts.size() != 2) {
                throw new RuntimeException("Constructor call must be ClassName.MethodName");
            }
            return construct(parts.get(0), parts.get(1), args);
        }

        Object target = resolveTarget(parts, parts.size() - 1);
        if (!(target instanceof ObjectInstance objectInstance)) {
            throw new RuntimeException("Cannot call method on non-object");
        }
        return invokeMethod(objectInstance, parts.get(parts.size() - 1), args);
    }

    @Override
    public Object visitPrimary(DelphiParser.PrimaryContext ctx) {
        if (ctx.INT() != null) {
            return Integer.parseInt(ctx.INT().getText());
        }
        if (ctx.variable() != null) {
            return resolveVariable(ctx.variable());
        }
        if (ctx.callExpr() != null) {
            return visit(ctx.callExpr());
        }
        return visit(ctx.expr());
    }

    private Object resolveVariable(DelphiParser.VariableContext ctx) {
        List<String> parts = extractParts(ctx);
        Object current = resolveBaseReference(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            if (!(current instanceof ObjectInstance objectInstance)) {
                throw new RuntimeException("Cannot access field on non-object");
            }
            current = objectInstance.getField(parts.get(i), currentClass());
        }
        return current;
    }

    private void assignVariable(DelphiParser.VariableContext ctx, Object value) {
        List<String> parts = extractParts(ctx);
        if (parts.size() == 1) {
            if (assignToSelfField(parts.get(0), value)) {
                return;
            }
            env.assign(parts.get(0), value);
            return;
        }
        Object target = resolveTarget(parts, parts.size() - 1);
        if (!(target instanceof ObjectInstance objectInstance)) {
            throw new RuntimeException("Cannot assign field on non-object");
        }
        objectInstance.setField(parts.get(parts.size() - 1), value, currentClass());
    }

    private Object resolveTarget(List<String> parts, int lastIndex) {
        Object current = resolveBaseReference(parts.get(0));
        for (int i = 1; i < lastIndex; i++) {
            if (!(current instanceof ObjectInstance objectInstance)) {
                throw new RuntimeException("Cannot access field on non-object");
            }
            current = objectInstance.getField(parts.get(i), currentClass());
        }
        return current;
    }

    private Object resolveBaseReference(String name) {
        Object current = env.resolve(name);
        if (current != null) {
            return current;
        }
        Object selfObj = env.resolve("self");
        if (selfObj instanceof ObjectInstance instance) {
            FieldDef field = instance.getClassDef().resolveFieldDef(name);
            if (field != null) {
                return instance.getField(name, currentClass());
            }
        }
        throw new RuntimeException("Unknown identifier: " + name);
    }

    private boolean assignToSelfField(String name, Object value) {
        Object selfObj = env.resolve("self");
        if (!(selfObj instanceof ObjectInstance instance)) {
            return false;
        }
        FieldDef field = instance.getClassDef().resolveFieldDef(name);
        if (field == null) {
            return false;
        }
        instance.setField(name, value, currentClass());
        return true;
    }

    private Object construct(String className, String ctorName, List<Object> args) {
        TypeDef typeDef = types.get(className);
        if (!(typeDef instanceof ClassDef classDef)) {
            throw new RuntimeException("Unknown class: " + className);
        }
        ObjectInstance instance = new ObjectInstance(classDef);
        MethodDef constructor = classDef.resolveConstructor(ctorName);
        if (constructor == null) {
            if (!args.isEmpty()) {
                throw new RuntimeException("No matching constructor for " + className + "." + ctorName);
            }
            return instance;
        }
        invokeMethod(instance, constructor, args);
        return instance;
    }

    private Object invokeMethod(ObjectInstance instance, String methodName, List<Object> args) {
        MethodDef methodDef = instance.getClassDef().resolveMethod(methodName);
        if (methodDef == null) {
            throw new RuntimeException("Unknown method: " + methodName);
        }
        return invokeMethod(instance, methodDef, args);
    }

    private Object invokeMethod(ObjectInstance instance, MethodDef methodDef, List<Object> args) {
        if (!methodDef.getVisibility().canAccess(currentClass(), methodDef.getDeclaringClass())) {
            throw new RuntimeException("Method not accessible: " + methodDef.getName());
        }
        if (methodDef.getParamNames().size() != args.size()) {
            throw new RuntimeException("Argument count mismatch for method " + methodDef.getName());
        }

        env.pushScope();
        env.define("self", instance);
        for (int i = 0; i < args.size(); i++) {
            env.define(methodDef.getParamNames().get(i), args.get(i));
        }
        classContext.push(instance.getClassDef());
        Object result = executeBlock(methodDef.getBlock(), false);
        classContext.pop();
        env.popScope();
        return result;
    }

    private void handleWrite(List<Object> args) {
        if (args.isEmpty()) {
            output.println();
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(stringify(args.get(i)));
        }
        output.println(builder.toString());
    }

    private void handleRead(DelphiParser.ArgListContext argsCtx) {
        if (argsCtx == null || argsCtx.expr().size() != 1) {
            throw new RuntimeException("readln expects one argument");
        }
        DelphiParser.VariableContext variable = extractVariable(argsCtx.expr(0));
        if (variable == null) {
            throw new RuntimeException("readln argument must be a variable");
        }
        if (!input.hasNextInt()) {
            throw new RuntimeException("Expected integer input");
        }
        assignVariable(variable, input.nextInt());
    }

    private DelphiParser.VariableContext extractVariable(DelphiParser.ExprContext expr) {
        DelphiParser.EqualityExprContext equality = expr.equalityExpr();
        if (equality.relationalExpr().size() != 1 || equality.getChildCount() != 1) {
            return null;
        }
        DelphiParser.RelationalExprContext relational = equality.relationalExpr(0);
        if (relational.addExpr().size() != 1 || relational.getChildCount() != 1) {
            return null;
        }
        DelphiParser.AddExprContext add = relational.addExpr(0);
        if (add.mulExpr().size() != 1 || add.getChildCount() != 1) {
            return null;
        }
        DelphiParser.MulExprContext mul = add.mulExpr(0);
        if (mul.unaryExpr().size() != 1 || mul.getChildCount() != 1) {
            return null;
        }
        DelphiParser.UnaryExprContext unary = mul.unaryExpr(0);
        if (unary.getChildCount() != 1) {
            return null;
        }
        DelphiParser.PrimaryContext primary = unary.primary();
        return primary.variable();
    }

    @Override
    public Object visitExpr(DelphiParser.ExprContext ctx) {
        return visit(ctx.equalityExpr());
    }

    @Override
    public Object visitEqualityExpr(DelphiParser.EqualityExprContext ctx) {
        Object left = visit(ctx.relationalExpr(0));
        for (int i = 1; i < ctx.relationalExpr().size(); i++) {
            Object right = visit(ctx.relationalExpr(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            left = compare(left, right, op);
        }
        return left;
    }

    @Override
    public Object visitRelationalExpr(DelphiParser.RelationalExprContext ctx) {
        Object left = visit(ctx.addExpr(0));
        for (int i = 1; i < ctx.addExpr().size(); i++) {
            Object right = visit(ctx.addExpr(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            left = compare(left, right, op);
        }
        return left;
    }

    @Override
    public Object visitAddExpr(DelphiParser.AddExprContext ctx) {
        Object result = visit(ctx.mulExpr(0));
        for (int i = 1; i < ctx.mulExpr().size(); i++) {
            Object right = visit(ctx.mulExpr(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            int leftVal = asInt(result);
            int rightVal = asInt(right);
            if ("+".equals(op)) {
                result = leftVal + rightVal;
            } else {
                result = leftVal - rightVal;
            }
        }
        return result;
    }

    @Override
    public Object visitMulExpr(DelphiParser.MulExprContext ctx) {
        Object result = visit(ctx.unaryExpr(0));
        for (int i = 1; i < ctx.unaryExpr().size(); i++) {
            Object right = visit(ctx.unaryExpr(i));
            String op = ctx.getChild((i * 2) - 1).getText();
            int leftVal = asInt(result);
            int rightVal = asInt(right);
            if ("*".equals(op)) {
                result = leftVal * rightVal;
            } else {
                result = leftVal / rightVal;
            }
        }
        return result;
    }

    @Override
    public Object visitUnaryExpr(DelphiParser.UnaryExprContext ctx) {
        Object value = visit(ctx.primary());
        if (ctx.MINUS() != null) {
            return -asInt(value);
        }
        return value;
    }

    private Object compare(Object left, Object right, String op) {
        int leftVal = asInt(left);
        int rightVal = asInt(right);
        boolean result;
        switch (op) {
            case "=":
                result = leftVal == rightVal;
                break;
            case "<>":
                result = leftVal != rightVal;
                break;
            case "<":
                result = leftVal < rightVal;
                break;
            case "<=":
                result = leftVal <= rightVal;
                break;
            case ">":
                result = leftVal > rightVal;
                break;
            case ">=":
                result = leftVal >= rightVal;
                break;
            default:
                throw new RuntimeException("Unknown operator: " + op);
        }
        return result ? 1 : 0;
    }

    private boolean asBoolean(Object value) {
        return asInt(value) != 0;
    }

    private int asInt(Object value) {
        if (value instanceof Integer intVal) {
            return intVal;
        }
        throw new RuntimeException("Expected integer value");
    }

    private String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof ObjectInstance instance) {
            return instance.getClassDef().getName();
        }
        return value.toString();
    }

    private List<String> extractParts(DelphiParser.VariableContext ctx) {
        List<String> parts = new ArrayList<>();
        for (var ident : ctx.IDENT()) {
            parts.add(normalize(ident.getText()));
        }
        return parts;
    }

    private boolean isTypeName(String name) {
        return types.get(name) instanceof ClassDef;
    }

    private ClassDef currentClass() {
        return classContext.peek();
    }

    private String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
