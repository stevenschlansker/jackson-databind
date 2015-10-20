package com.fasterxml.jackson.databind.type;

import java.util.*;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Simple types are defined as anything other than one of recognized
 * container types (arrays, Collections, Maps). For our needs we
 * need not know anything further, since we have no way of dealing
 * with generic types other than Collections and Maps.
 */
public class SimpleType // note: until 2.6 was final
    extends TypeBase
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected SimpleType(Class<?> cls) {
        this(cls, TypeBindings.emptyBindings(), null, null);
    }

    protected SimpleType(Class<?> cls, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts) {
        this(cls, bindings, superClass, superInts, null, null, false);
    }

    /**
     * Simple copy-constructor, usually used when upgrading/refining a simple type
     * into more specialized type.
     *
     * @since 2.7
     */
    protected SimpleType(TypeBase base) {
        super(base);
    }

    /**
     * 
     * @param parametersFrom Interface or abstract class implemented by this type,
     *   and for which type parameters apply. It may be <code>cls</code> itself,
     *   but more commonly it is one of its supertypes.
     */
    protected SimpleType(Class<?> cls, TypeBindings bindings, // probably wrong
            JavaType superClass, JavaType[] superInts,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(cls, bindings, superClass, superInts,
                0, valueHandler, typeHandler, asStatic);
    }

    /**
     * Pass-through constructor used by {@link ReferenceType}.
     * 
     * @since 2.6
     */
    protected SimpleType(Class<?> cls, TypeBindings bindings,
            JavaType superClass, JavaType[] superInts, int extraHash,
            Object valueHandler, Object typeHandler, boolean asStatic)
    {
        super(cls, bindings, superClass, superInts, 
                extraHash, valueHandler, typeHandler, asStatic);
    }
    
    /**
     * Method used by core Jackson classes: NOT to be used by application code.
     *<p>
     * NOTE: public only because it is called by <code>ObjectMapper</code> which is
     * not in same package
     */
    public static SimpleType constructUnsafe(Class<?> raw) {
        return new SimpleType(raw, null,
                // 18-Oct-2015, tatu: Should be ok to omit possible super-types, right?
                null, null, null, null, false);
    }

    @Override
    protected JavaType _narrow(Class<?> subclass)
    {
        // Should we check that there is a sub-class relationship?
        return new SimpleType(subclass, _bindings, _superClass, _superInterfaces,
                _valueHandler, _typeHandler, _asStatic);
    }

    @Override
    public JavaType narrowContentsBy(Class<?> subclass)
    {
        // should never get called
        throw new IllegalArgumentException("Internal error: SimpleType.narrowContentsBy() should never be called");
    }

    @Override
    public JavaType widenContentsBy(Class<?> subclass)
    {
        // should never get called
        throw new IllegalArgumentException("Internal error: SimpleType.widenContentsBy() should never be called");
    }
    
    public static SimpleType construct(Class<?> cls)
    {
        /* Let's add sanity checks, just to ensure no
         * Map/Collection entries are constructed
         */
        if (Map.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Can not construct SimpleType for a Map (class: "+cls.getName()+")");
        }
        if (Collection.class.isAssignableFrom(cls)) {
            throw new IllegalArgumentException("Can not construct SimpleType for a Collection (class: "+cls.getName()+")");
        }
        // ... and while we are at it, not array types either
        if (cls.isArray()) {
            throw new IllegalArgumentException("Can not construct SimpleType for an array (class: "+cls.getName()+")");
        }
        return new SimpleType(cls);
    }

    @Override
    public SimpleType withTypeHandler(Object h) {
        return new SimpleType(_class, _bindings, _superClass, _superInterfaces, _valueHandler, h, _asStatic);
    }

    @Override
    public JavaType withContentTypeHandler(Object h) {
        // no content type, so:
        throw new IllegalArgumentException("Simple types have no content types; can not call withContenTypeHandler()");
    }

    @Override
    public SimpleType withValueHandler(Object h) {
        if (h == _valueHandler) {
            return this;
        }
        return new SimpleType(_class, _bindings, _superClass, _superInterfaces, h, _typeHandler, _asStatic);
    }
    
    @Override
    public  SimpleType withContentValueHandler(Object h) {
        // no content type, so:
        throw new IllegalArgumentException("Simple types have no content types; can not call withContenValueHandler()");
    }

    @Override
    public SimpleType withStaticTyping() {
        return _asStatic ? this : new SimpleType(_class, _bindings,
                _superClass, _superInterfaces, _valueHandler, _typeHandler, true);
    }

    @Override
    public JavaType refine(Class<?> rawType, TypeBindings bindings,
            JavaType superClass, JavaType[] superInterfaces) {
        // SimpleType means something not-specialized, so:
        return null;
    }
    
    @Override
    protected String buildCanonicalName()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(_class.getName());

        final int count = _bindings.size();
        if (count > 0) {
            sb.append('<');
            for (int i = 0; i < count; ++i) {
                JavaType t = containedType(i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(t.toCanonical());
            }
            sb.append('>');
        }
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    @Override
    public boolean isContainerType() { return false; }

    @Override
    public StringBuilder getErasedSignature(StringBuilder sb) {
        return _classSignature(_class, sb, true);
    }
    
    @Override
    public StringBuilder getGenericSignature(StringBuilder sb)
    {
        _classSignature(_class, sb, false);

        final int count = _bindings.size();
        if (count > 0) {
            sb.append('<');
            for (int i = 0; i < count; ++i) {
                sb = containedType(i).getGenericSignature(sb);
            }
            sb.append('>');
        }
        sb.append(';');
        return sb;
    }

    /*
    /**********************************************************
    /* Standard methods
    /**********************************************************
     */

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(40);
        sb.append("[simple type, class ").append(buildCanonicalName()).append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        SimpleType other = (SimpleType) o;

        // Classes must be identical... 
        if (other._class != this._class) return false;

        // And finally, generic bindings, if any
        TypeBindings b1 = _bindings;
        TypeBindings b2 = other._bindings;
        return b1.equals(b2);
    }
}
