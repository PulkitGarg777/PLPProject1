package delphi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface TypeDef {
    String getName();
}

final class IntegerTypeDef implements TypeDef {
    @Override
    public String getName() {
        return "integer";
    }
}

final class InterfaceDef implements TypeDef {
    private final String name;
    private final List<String> methodNames;

    InterfaceDef(String name, List<String> methodNames) {
        this.name = name;
        this.methodNames = methodNames;
    }

    @Override
    public String getName() {
        return name;
    }

    List<String> getMethodNames() {
        return methodNames;
    }
}

enum Visibility {
    PUBLIC,
    PRIVATE,
    PROTECTED;

    boolean canAccess(ClassDef accessor, ClassDef declaring) {
        if (this == PUBLIC) {
            return true;
        }
        if (accessor == null) {
            return false;
        }
        if (this == PRIVATE) {
            return accessor == declaring;
        }
        return accessor == declaring || accessor.isSubclassOf(declaring);
    }
}

final class FieldDef {
    private final String name;
    private final String typeName;
    private final Visibility visibility;
    private final ClassDef declaringClass;

    FieldDef(String name, String typeName, Visibility visibility, ClassDef declaringClass) {
        this.name = name;
        this.typeName = typeName;
        this.visibility = visibility;
        this.declaringClass = declaringClass;
    }

    String getName() {
        return name;
    }

    String getTypeName() {
        return typeName;
    }

    Visibility getVisibility() {
        return visibility;
    }

    ClassDef getDeclaringClass() {
        return declaringClass;
    }
}

final class MethodDef {
    private final String name;
    private final List<String> paramNames;
    private final List<String> paramTypes;
    private final Visibility visibility;
    private final ClassDef declaringClass;
    private final DelphiParser.BlockContext block;
    private final boolean constructor;
    private final boolean destructor;

    MethodDef(String name, List<String> paramNames, List<String> paramTypes, Visibility visibility,
            ClassDef declaringClass, DelphiParser.BlockContext block, boolean constructor, boolean destructor) {
        this.name = name;
        this.paramNames = paramNames;
        this.paramTypes = paramTypes;
        this.visibility = visibility;
        this.declaringClass = declaringClass;
        this.block = block;
        this.constructor = constructor;
        this.destructor = destructor;
    }

    String getName() {
        return name;
    }

    List<String> getParamNames() {
        return paramNames;
    }

    List<String> getParamTypes() {
        return paramTypes;
    }

    Visibility getVisibility() {
        return visibility;
    }

    ClassDef getDeclaringClass() {
        return declaringClass;
    }

    DelphiParser.BlockContext getBlock() {
        return block;
    }

    boolean isConstructor() {
        return constructor;
    }

    boolean isDestructor() {
        return destructor;
    }
}

final class ClassDef implements TypeDef {
    private final String name;
    private String baseClassName;
    private final List<String> interfaceNames;
    private final Map<String, FieldDef> fields;
    private final Map<String, MethodDef> methods;
    private final Map<String, MethodDef> constructors;
    private MethodDef destructor;

    ClassDef(String name) {
        this.name = name;
        this.interfaceNames = new ArrayList<>();
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
        this.constructors = new HashMap<>();
    }

    @Override
    public String getName() {
        return name;
    }

    void setBaseClassName(String baseClassName) {
        this.baseClassName = baseClassName;
    }

    String getBaseClassName() {
        return baseClassName;
    }

    void addInterfaceName(String name) {
        interfaceNames.add(name);
    }

    List<String> getInterfaceNames() {
        return interfaceNames;
    }

    void addField(FieldDef fieldDef) {
        fields.put(fieldDef.getName(), fieldDef);
    }

    void addMethod(MethodDef methodDef) {
        methods.put(methodDef.getName(), methodDef);
    }

    void addConstructor(MethodDef methodDef) {
        constructors.put(methodDef.getName(), methodDef);
    }

    void setDestructor(MethodDef methodDef) {
        destructor = methodDef;
    }

    MethodDef getDestructor() {
        return destructor;
    }

    FieldDef resolveFieldDef(String name) {
        FieldDef local = fields.get(name);
        if (local != null) {
            return local;
        }
        ClassDef base = getBaseClass();
        if (base == null) {
            return null;
        }
        return base.resolveFieldDef(name);
    }

    MethodDef resolveMethod(String name) {
        MethodDef local = methods.get(name);
        if (local != null) {
            return local;
        }
        ClassDef base = getBaseClass();
        if (base == null) {
            return null;
        }
        return base.resolveMethod(name);
    }

    MethodDef resolveConstructor(String name) {
        MethodDef local = constructors.get(name);
        if (local != null) {
            return local;
        }
        ClassDef base = getBaseClass();
        if (base == null) {
            return null;
        }
        return base.resolveConstructor(name);
    }

    Map<String, FieldDef> getDeclaredFields() {
        return fields;
    }

    Map<String, MethodDef> getDeclaredMethods() {
        return methods;
    }

    ClassDef getBaseClass() {
        return RuntimeEnvironment.resolveClass(baseClassName);
    }

    boolean isSubclassOf(ClassDef other) {
        ClassDef current = getBaseClass();
        while (current != null) {
            if (current == other) {
                return true;
            }
            current = current.getBaseClass();
        }
        return false;
    }
}

final class ObjectInstance {
    private final ClassDef classDef;
    private final Map<String, Object> fields;

    ObjectInstance(ClassDef classDef) {
        this.classDef = classDef;
        this.fields = new HashMap<>();
        initializeFields(classDef);
    }

    ClassDef getClassDef() {
        return classDef;
    }

    Object getField(String name, ClassDef accessor) {
        FieldDef def = classDef.resolveFieldDef(name);
        if (def == null) {
            throw new RuntimeException("Unknown field: " + name);
        }
        if (!def.getVisibility().canAccess(accessor, def.getDeclaringClass())) {
            throw new RuntimeException("Field not accessible: " + name);
        }
        return fields.get(name);
    }

    void setField(String name, Object value, ClassDef accessor) {
        FieldDef def = classDef.resolveFieldDef(name);
        if (def == null) {
            throw new RuntimeException("Unknown field: " + name);
        }
        if (!def.getVisibility().canAccess(accessor, def.getDeclaringClass())) {
            throw new RuntimeException("Field not accessible: " + name);
        }
        fields.put(name, value);
    }

    private void initializeFields(ClassDef def) {
        ClassDef base = def.getBaseClass();
        if (base != null) {
            initializeFields(base);
        }
        for (FieldDef field : def.getDeclaredFields().values()) {
            fields.put(field.getName(), defaultValueFor(field.getTypeName()));
        }
    }

    private Object defaultValueFor(String typeName) {
        if ("integer".equals(typeName)) {
            return 0;
        }
        return null;
    }
}

final class RuntimeEnvironment {
    private static final Map<String, ClassDef> CLASS_REGISTRY = new HashMap<>();
    private final List<Map<String, Object>> scopes = new ArrayList<>();

    void pushScope() {
        scopes.add(new HashMap<>());
    }

    void popScope() {
        if (scopes.isEmpty()) {
            throw new RuntimeException("Scope stack underflow");
        }
        scopes.remove(scopes.size() - 1);
    }

    void define(String name, Object value) {
        if (scopes.isEmpty()) {
            pushScope();
        }
        scopes.get(scopes.size() - 1).put(name, value);
    }

    void assign(String name, Object value) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name)) {
                scopes.get(i).put(name, value);
                return;
            }
        }
        define(name, value);
    }

    Object resolve(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Object> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null;
    }

    static void registerClass(ClassDef classDef) {
        CLASS_REGISTRY.put(classDef.getName(), classDef);
    }

    static ClassDef resolveClass(String name) {
        if (name == null) {
            return null;
        }
        return CLASS_REGISTRY.get(name);
    }

    static void clearClassRegistry() {
        CLASS_REGISTRY.clear();
    }
}
