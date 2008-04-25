package de.tum.in.jmoped.translator;

import org.gjt.jclasslib.structures.AccessFlags;
import org.gjt.jclasslib.structures.CPInfo;
import org.gjt.jclasslib.structures.ClassFile;
import org.gjt.jclasslib.structures.FieldInfo;
import org.gjt.jclasslib.structures.InvalidByteCodeException;
import org.gjt.jclasslib.structures.attributes.ConstantValueAttribute;
import org.gjt.jclasslib.structures.constants.ConstantIntegerInfo;

import de.tum.in.jmoped.translator.stub.StubManager;

/**
 * A class field translator.
 * 
 * @author suwimont
 *
 */
public class FieldTranslator {

	/**
	 * Each instace field has a unique id starting from 1.
	 */
	private int id;
	
	/**
	 * The jclasslib's filed info.
	 */
	private FieldInfo field;
	
	/**
	 * The field's name.
	 */
	private String name;
	
	/**
	 * This field has value if it is final.
	 */
	private Object value;
	
	/**
	 * Constructs a field translator.
	 * 
	 * @param field the field info.
	 */
	public FieldTranslator(FieldInfo field) {
		
		this.field = field;
		
		// Sets value
		if (isFinal()) {
			ConstantValueAttribute attr = (ConstantValueAttribute) 
					field.findAttribute(ConstantValueAttribute.class);
			if (attr != null) {
				CPInfo cp = field.getClassFile().getConstantPool()[attr.getConstantvalueIndex()];
				if (cp instanceof ConstantIntegerInfo) {
					value = ((ConstantIntegerInfo) cp).getInt();
				}
			}
		}
	}
	
	/**
	 * Sets the id of this field. 
	 * Each instace field has a unique id starting from 1.
	 * 
	 * @param id the id.
	 */
	void setId(int id) {
		
		this.id = id;
	}
	
	/**
	 * Gets the id of this field. Each instance field in a module collection 
	 * has a unique id. Ids are meaningless for static fields and always
	 * have value zeros.
	 * 
	 * @return the id of this field.
	 */
	public int getId() {
		
		return id;
	}
	
	public String getName() {
		
		if (name != null) return name;
		name = formatName(field.getClassFile(), field);
		return name;
	}
	
	/**
	 * Returns <code>true</code> if this field has a constant value defined;
	 * or <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> iff this field has a constant value defined.
	 */
	public boolean hasValue() {
		return value != null;
	}
	
	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	public static boolean isFinal(int flag) {
		return (flag & AccessFlags.ACC_FINAL) != 0;
	}
	
	/**
	 * Return <code>true</code> if this field is final; or <code>false</code>
	 * otherwise.
	 * 
	 * @return <code>true</code> iff this field is final.
	 */
	public boolean isFinal() {
		return isFinal(field.getAccessFlags());
	}
	
	public boolean isStatic() {
		
		return TranslatorUtils.isStatic(field.getAccessFlags());
	}
	
	public boolean isAssertionsDisabledField() {
		
		return getName().endsWith("$assertionsDisabled");
	}
	
	public static String formatName(String className, String fieldName) {
		
		return className + "." + fieldName;
	}
	
	public static String formatName(String[] name) {
		
		return formatName(name[0], name[1]);
	}
	
	public static String formatName(ClassFile cf, FieldInfo fi) {
		
		try {
			return formatName(
					StubManager.removeStub(cf.getThisClassName()), 
					fi.getName());
		} catch (InvalidByteCodeException e) {
			System.err.println("Unexpected error in class: " + cf + ", field:" + fi);
			e.printStackTrace();
			return null;
		}
	}
	
	public String toString() {
		StringBuilder out = new StringBuilder();
		out.append("name: ").append(getName());
		if (id > 0)
			out.append(", id: ").append(id);
		if (value != null)
			out.append(", value: ").append(value);
		return out.toString();
	}
	
	public Object clone() {
		
		FieldTranslator clone = new FieldTranslator(field);
		clone.id = id;
		clone.name = name;
		
		return clone;
	}
}
