/*
 * @(#) net/sf/jadretro/FieldMethodEntry.java --
 * a part of JadRetro source.
 **
 * Copyright (C) 2007-2008 Ivan Maidanski <ivmai@mail.ru>
 * All rights reserved.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package net.sf.jadretro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

final class FieldMethodEntry extends ClassEntity {

	private/* final */AccessFlags accessFlags;

	private/* final */ConstantRef name;

	private ConstantRef descriptor;

	private/* final */Vector attributes;

	FieldMethodEntry(AccessFlags accessFlags, ConstantRef name,
			ConstantRef descriptor) {
		this.accessFlags = accessFlags;
		this.name = name;
		this.descriptor = descriptor;
		attributes = new Vector(2);
	}

	FieldMethodEntry(InputStream in, ClassFile classFile) throws IOException {
		accessFlags = new AccessFlags(in);
		name = new ConstantRef(in, classFile, false);
		descriptor = new ConstantRef(in, classFile, false);
		attributes = readAttributes(in, classFile);
	}

	void writeTo(OutputStream out) throws IOException {
		accessFlags.writeTo(out);
		name.writeTo(out);
		descriptor.writeTo(out);
		writeToForArray(attributes, out);
	}

	AccessFlags accessFlags() {
		return accessFlags;
	}

	ConstantRef name() {
		return name;
	}

	ConstantRef descriptor() {
		return descriptor;
	}

	void changeDescriptor(ConstantRef descriptor) {
		this.descriptor = descriptor;
	}

	void addAttribute(AttributeEntry attrEntry) {
		attributes.addElement(attrEntry);
	}

	boolean hasExceptionsSynthetic(boolean isExceptions)
			throws BadClassFileException {
		String attrName = isExceptions ? AttrRawContent.exceptionsName()
				: AttrRawContent.syntheticName();
		int count = attributes.size();
		for (int i = 0; i < count; i++) {
			if (((AttributeEntry) attributes.elementAt(i)).getNameValue()
					.equals(attrName))
				return true;
		}
		return false;
	}

	AttrCodeContent findCode() {
		int count = attributes.size();
		for (int i = 0; i < count; i++) {
			AttrContent content = ((AttributeEntry) attributes.elementAt(i))
					.content();
			if (content instanceof AttrCodeContent)
				return (AttrCodeContent) content;
		}
		return null;
	}

	int getArgSlotsCount() throws BadClassFileException {
		int argSlots = 0;
		if (!accessFlags.isStatic()) {
			argSlots++;
		}
		String descrValue = descriptor.utfValue();
		int endPos = descrValue.lastIndexOf(')');
		if (endPos <= 0 || descrValue.charAt(0) != '(')
			throw new BadClassFileException();
		for (int pos = 1; pos < endPos; pos++) {
			char ch = descrValue.charAt(pos);
			argSlots++;
			if (ch == 'D' || ch == 'J') {
				argSlots++;
			} else {
				while (ch == '[') {
					ch = descrValue.charAt(++pos);
				}
				if (ch == 'L') {
					pos = descrValue.indexOf(';', pos + 1);
					if (pos < 0 || pos >= endPos)
						throw new BadClassFileException();
				}
			}
		}
		return argSlots;
	}

	boolean isVoidRetType() throws BadClassFileException {
		String descrValue = descriptor.utfValue();
		int len = descrValue.length();
		if (len <= 2)
			throw new BadClassFileException();
		return descrValue.charAt(len - 1) == 'V'
				&& descrValue.charAt(len - 2) == ')';
	}
}
